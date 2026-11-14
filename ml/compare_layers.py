#!/usr/bin/env python3
"""UniLink ML - ADIM 5: KURAL vs ML vs HİBRİT kapsama karşılaştırması.

Hocanın istediği "kural tabanlı / ML / hibrit" karşılaştırması. Her (ip, dakika) penceresi için
üç katmanın da o pencereyi işaretleyip işaretlemediğine bakılır:

  * KURAL  : 4 kuraldan en az biri o pencerede tetiklendi mi?
             (ham olay akışı üzerinde, kuralların kendi pencereleriyle yeniden oynatılır)
             FailedLogin 5/600sn, HighRequestRate 100/10sn, ResourceProbing 5/60sn (403/404),
             AdminProbe 3/600sn (yetkisiz /api/admin). Eşik+pencere değerleri agent.md ile birebir.
  * ML     : kNN anomali mesafesi >= eşik (knn_model.joblib; train_model.py ile aynı eşik).
  * HİBRİT : KURAL veya ML (birleşim). Pratikte DecisionEngine'in kuralSkoru + 0.4*mlSkoru
             birleşimiyle aynı "en az biri işaret verdi" davranışı.

Neden skor değil kapsama? Sürekli risk skorunun tam birikme biçimi (her olayda mı, pencere
başına mı) ve DecisionEngine orta-bant alt sınırı implementasyon detayıdır; kapsama ise yalnızca
belgelenmiş eşik+pencere değerlerine dayanır, bu yüzden varsayımsız ve tekrarlanabilir.

Adillik: ML normal yanlış-pozitifi YALNIZCA test IP'lerinde ölçülür (eğitimde görülmeyen,
train_model.py ile aynı %60/20/20 IP bölmesi). Saldırılar zaten hiçbir zaman eğitime girmez.

Gereksinim: pip install scikit-learn numpy joblib
Girdi : data/*_events.csv (ham), data/features_*.csv (build_features çıktısı), models/knn_model.joblib
Çıktı : konsolda karşılaştırma tablosu.
"""
import csv, random
from collections import defaultdict, deque
from datetime import datetime
import numpy as np
import joblib

SEED = 42
RULES = {  # ad: (pencere_sn, eşik, puan)
    "failed_login": (600, 5, 20),
    "rate":         (10, 100, 20),
    "probing":      (60,  5, 30),
    "admin_probe":  (600, 3, 40),
}

def parse_ts(s):
    return datetime.fromisoformat(s)

def minute_key(occurred_at):
    return occurred_at[:16]

# ---- 1) Kuralları ham olay akışında yeniden oynat -> her (ip,dakika) için tetiklenen kurallar ----
def replay_rules(event_files):
    by_ip = defaultdict(list)
    for path in event_files:
        for e in csv.DictReader(open(path, newline="", encoding="utf-8")):
            if int(e["status_code"]) == 429:      # 429 geri beslenmez (observation modu zaten üretmez)
                continue
            by_ip[e["ip_address"]].append(e)
    fired = defaultdict(set)                       # (ip,minute) -> {kural adları}
    for ip, evs in by_ip.items():
        evs.sort(key=lambda e: e["occurred_at"])
        win = {k: deque() for k in RULES}          # her kural için zaman damgası penceresi
        for e in evs:
            t = parse_ts(e["occurred_at"]).timestamp()
            st = int(e["status_code"])
            member = {
                "failed_login": e["event_type"] == "LOGIN_FAILURE",
                "rate": True,                                             # her istek
                "probing": st in (403, 404),
                "admin_probe": e["path_pattern"].startswith("/api/admin") and st in (401, 403),
            }
            bucket = (ip, minute_key(e["occurred_at"]))
            for name, (wsec, thr, _pts) in RULES.items():
                dq = win[name]
                if member[name]:
                    dq.append(t)
                while dq and t - dq[0] > wsec:
                    dq.popleft()
                if member[name] and len(dq) >= thr:
                    fired[bucket].add(name)
    return fired

# ---- 2) kNN ml_score'u (ip,dakika) başına ----
FEATURES = ["req_count", "distinct_paths", "err_4xx_ratio", "c401", "c403", "c404",
            "login_fail_count", "distinct_login_ids", "write_ratio", "admin_path_hits"]

def load_features(path):
    return list(csv.DictReader(open(path, newline="", encoding="utf-8")))

def ml_distance(rows, model):
    X = np.array([[float(r[f]) for f in FEATURES] for r in rows], float)
    X[:, model["count_cols_log1p"]] = np.log1p(X[:, model["count_cols_log1p"]])
    d = model["nn"].kneighbors(model["scaler"].transform(X))[0].mean(axis=1)
    return d

# ---- 3) Aynı IP bölmesini yeniden üret (train_model.split_by_ip ile birebir) ----
def test_ips(normal_rows, seed=SEED):
    ips = sorted({r["ip_address"] for r in normal_rows})
    random.Random(seed).shuffle(ips)
    b = int(len(ips) * 0.8)
    return set(ips[b:])                            # test %20

def main():
    model = joblib.load("models/knn_model.joblib")
    thr = model["threshold"]
    fired = replay_rules(["data/normal_events.csv", "data/attack_events.csv"])

    normal = load_features("data/features_normal.csv")
    attack = load_features("data/features_attack.csv")
    te_ips = test_ips(normal)
    normal_te = [r for r in normal if r["ip_address"] in te_ips]

    dn = ml_distance(normal_te, model)
    da = ml_distance(attack, model)

    def layers(rows, dist):
        out = []
        for r, d in zip(rows, dist):
            rule = bool(fired.get((r["ip_address"], r["minute"])))
            ml = d >= thr
            out.append((r, rule, ml, rule or ml))
        return out

    L_norm = layers(normal_te, dn)
    L_atk = layers(attack, da)

    def rates(sel):
        n = len(sel)
        if n == 0:
            return 0, 0.0, 0.0, 0.0
        k = sum(x[1] for x in sel) / n
        m = sum(x[2] for x in sel) / n
        h = sum(x[3] for x in sel) / n
        return n, k, m, h

    # Başlıca kuralı bul (bir saldırı varyantını en çok hangi kural yakalıyor)
    def top_rule(sel):
        c = defaultdict(int)
        for r, *_ in sel:
            for name in fired.get((r["ip_address"], r["minute"]), ()):
                c[name] += 1
        return max(c, key=c.get) if c else "-"

    print(f"{'saldırı varyantı':24s} {'n':>5s} {'KURAL':>7s} {'ML':>7s} {'HİBRİT':>7s}  başlıca kural")
    print("-" * 72)
    by_var = defaultdict(list)
    for x in L_atk:
        by_var[f"{x[0]['label']}/{x[0]['profile']}"].append(x)
    tot = [0, 0, 0, 0]
    for var in sorted(by_var):
        n, k, m, h = rates(by_var[var])
        tot[0] += n; tot[1] += k * n; tot[2] += m * n; tot[3] += h * n
        print(f"{var:24s} {n:5d} {k:7.3f} {m:7.3f} {h:7.3f}  {top_rule(by_var[var])}")
    N = tot[0]
    print("-" * 72)
    print(f"{'TÜM SALDIRILAR (recall)':24s} {N:5d} {tot[1]/N:7.3f} {tot[2]/N:7.3f} {tot[3]/N:7.3f}")

    n, k, m, h = rates(L_norm)
    print(f"\n{'NORMAL (yanlış pozitif)':24s} {n:5d} {k:7.3f} {m:7.3f} {h:7.3f}   (test IP'leri)")
    print("\nOkuma: KURAL sütunu kuralların tek başına yakaladığı oran, ML kNN'in, HİBRİT ikisinin birleşimi.")
    print("Hibrit recall >= her iki katmandan büyük olanı; bedeli normal yanlış pozitifteki küçük artış.")

if __name__ == "__main__":
    main()