#!/usr/bin/env python3
"""UniLink ML - ADIM 4: anomali modeli eğitimi ve değerlendirme.

Ana model: kNN mesafesi (daha yüksek recall). Isolation Forest raporda karşılaştırma temeli
olarak eğitilir ve kaydedilir. Ham kNN mesafesi, hibrit için doğrulama normalinden kalibre
edilen bir sigmoid ile [0,1] aralığındaki ml_score'a çevrilir (eşikte 0.5).

Gereksinim:  pip install scikit-learn numpy
Girdi : data/features_normal.csv, data/features_attack.csv  (build_features.py çıktısı)
Çıktı : models/knn_model.joblib (ana), models/iforest.joblib (karşılaştırma),
        models/model_meta.json ve konsolda değerlendirme tablosu.

Yöntem (denetimsiz): model YALNIZCA normal istemci-dakikalarıyla eğitilir.
Normal veri istemci (IP) bazında %60 eğitim / %20 doğrulama / %20 test ayrılır (aynı istemcinin
satırları farklı kümelere dağılmaz -> sızıntı yok). Test = %20 normal + tüm saldırı satırları.
Eşik: DOĞRULAMA normallerinin anomali skorunun %99'luk dilimi (hedef yanlış pozitif ~%1);
eşik eğitim verisinden seçilmez (model eğitim verisini fazla iyi bildiği için yanıltır).
Karşılaştırma modeli: k-en yakın komşu mesafesi (kNN). Local Outlier Factor denendi ancak veri
çok sayıda birebir aynı satır içerdiği (14.332 satırda yalnızca ~863 benzersiz) için LOF skorları
bozuldu (doğrulama normallerinde ~1e9); bu bulgu raporda yer alacak.
"""
import csv, json, os, random
from collections import defaultdict
import numpy as np
from sklearn.ensemble import IsolationForest
from sklearn.neighbors import NearestNeighbors
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import roc_auc_score, precision_score, recall_score, f1_score
import joblib

FEATURES = ["req_count", "distinct_paths", "err_4xx_ratio", "c401", "c403", "c404",
            "login_fail_count", "distinct_login_ids", "write_ratio", "admin_path_hits"]
COUNT_COLS = [0, 1, 3, 4, 5, 6, 7, 9]          # log1p uygulanacak sayaç sütunları
SEED = 42

def load(path):
    rows = list(csv.DictReader(open(path, newline="", encoding="utf-8")))
    X = np.array([[float(r[f]) for f in FEATURES] for r in rows])
    return rows, X

def prep(X):
    X = X.copy()
    X[:, COUNT_COLS] = np.log1p(X[:, COUNT_COLS])
    return X

def split_by_ip(rows, seed=SEED):
    ips = sorted({r["ip_address"] for r in rows})
    random.Random(seed).shuffle(ips)
    a, b = int(len(ips) * 0.6), int(len(ips) * 0.8)
    groups = {"tr": set(ips[:a]), "va": set(ips[a:b]), "te": set(ips[b:])}
    return tuple([i for i, r in enumerate(rows) if r["ip_address"] in groups[k]] for k in ("tr", "va", "te"))

def report(name, y_true, score, thr, labels):
    pred = (score >= thr).astype(int)
    print(f"\n=== {name} ===")
    print(f"ROC-AUC: {roc_auc_score(y_true, score):.4f}")
    print(f"Precision: {precision_score(y_true, pred):.4f}  Recall: {recall_score(y_true, pred):.4f}  F1: {f1_score(y_true, pred):.4f}")
    normal_mask = y_true == 0
    print(f"Yanlış pozitif oranı (test normal): {pred[normal_mask].mean():.4f}")
    per = defaultdict(list)
    for lab, p in zip(labels, pred):
        per[lab].append(p)
    print("Saldırı varyasyonu başına yakalama (recall):")
    for lab in sorted(per):
        if lab.startswith("normal"):
            continue
        print(f"  {lab:26s} {np.mean(per[lab]):.3f}  (n={len(per[lab])})")

def main():
    n_rows, Xn = load("data/features_normal.csv")
    a_rows, Xa = load("data/features_attack.csv")
    tr, va, te = split_by_ip(n_rows)
    Xtr, Xva, Xte_n = prep(Xn[tr]), prep(Xn[va]), prep(Xn[te])
    Xte_a = prep(Xa)
    print(f"Eğitim (normal): {len(tr)} | Doğrulama (normal): {len(va)} | Test normal: {len(te)} | Test saldırı: {len(a_rows)}")

    X_test = np.vstack([Xte_n, Xte_a])
    y_test = np.concatenate([np.zeros(len(te)), np.ones(len(a_rows))])
    labels = [f"normal/{n_rows[i]['profile']}" for i in te] + [f"{r['label']}/{r['profile']}" for r in a_rows]

    # ---- Isolation Forest ----
    iforest = IsolationForest(n_estimators=300, max_samples=256, random_state=SEED, n_jobs=-1)
    iforest.fit(Xtr)
    s_train = -iforest.score_samples(Xtr)             # büyük = daha anormal
    thr = float(np.percentile(-iforest.score_samples(Xva), 99))
    s_test = -iforest.score_samples(X_test)
    report("Isolation Forest", y_test, s_test, thr, labels)

    # ---- kNN mesafesi (karşılaştırma) ----
    scaler = StandardScaler().fit(Xtr)
    knn = NearestNeighbors(n_neighbors=10).fit(scaler.transform(Xtr))
    def knn_score(X):
        return knn.kneighbors(scaler.transform(X))[0].mean(axis=1)   # büyük = daha anormal
    d_va = knn_score(Xva)
    k_thr = float(np.percentile(d_va, 99))
    k_test = knn_score(X_test)
    report("kNN mesafesi (k=10)", y_test, k_test, k_thr, labels)

    # ---- Ham kNN mesafesini [0,1] ml_score'a eşle (hibrit için) ----
    # Sadece doğrulama normalinden kalibre edilir; saldırı verisi kullanılmaz.
    d50 = float(np.percentile(d_va, 50))          # tipik normal (merkez), referans amaçlı
    scale = max(k_thr - d50, 1e-9)                 # normal bandın genişliği (rampanın eğimi)
    def ml_score(d):                               # eşiğe kadar 0, eşikten scale kadar uzakta 1
        return np.clip((np.asarray(d, float) - k_thr) / scale, 0.0, 1.0)
    ms_norm, ms_atk = ml_score(k_test[y_test == 0]), ml_score(k_test[y_test == 1])
    print(f"\nml_score (0-1) ortanca -> normal: {np.median(ms_norm):.3f} | saldırı: {np.median(ms_atk):.3f}")
    print(f"Kalibrasyon: eşik(d)={k_thr:.3f} -> 0.00 başlar | eşik+scale -> 1.00 | normal %99 altı -> 0")

    os.makedirs("models", exist_ok=True)
    joblib.dump(iforest, "models/iforest.joblib")                      # raporda karşılaştırma
    joblib.dump({"scaler": scaler, "nn": knn, "k": 10, "features": FEATURES,
                 "count_cols_log1p": COUNT_COLS, "threshold": k_thr,
                 "d50": d50, "scale": scale}, "models/knn_model.joblib")   # ana model
    meta = {
        "deployed_model": "knn",
        "features": FEATURES, "count_cols_log1p": COUNT_COLS, "seed": SEED,
        "knn": {"k": 10, "threshold_p99": k_thr, "d50": d50, "scale": scale,
                "ml_score": "clip((d-threshold)/scale,0,1); esige kadar 0, esik+scale -> 1"},
        "iforest_baseline": {"threshold_p99": thr,
                             "score_train_min": float(s_train.min()),
                             "score_train_max": float(s_train.max())},
    }
    json.dump(meta, open("models/model_meta.json", "w"), indent=2)
    print("\nKaydedildi: models/knn_model.joblib (ana), models/iforest.joblib (karşılaştırma), models/model_meta.json")

if __name__ == "__main__":
    main()