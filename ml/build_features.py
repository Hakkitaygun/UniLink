#!/usr/bin/env python3
"""UniLink ML - ADIM 3: olay CSV'lerini istemci-dakikası özelliklerine çevirir.

features.sql ile BİREBİR aynı tanım (v1):
  * birim: (ip_address, 1 dakikalık pencere)
  * status_code = 429 satırları hiçbir özelliğe girmez
  * c401 yalnızca ACCESS_DENIED + 401; giriş hataları login_fail_count'ta
Kullanım:  python build_features.py
Girdi : data/normal_events.csv, data/attack_events.csv
Çıktı : data/features_normal.csv, data/features_attack.csv
"""
import argparse, csv, os
from collections import defaultdict

FEATURES = ["req_count", "distinct_paths", "err_4xx_ratio", "c401", "c403", "c404",
            "login_fail_count", "distinct_login_ids", "write_ratio", "admin_path_hits"]
WRITE_METHODS = {"POST", "PUT", "PATCH", "DELETE"}

def compute_features(events):
    """Tek bir (ip, dakika) penceresindeki olay listesinden 10 özelliği hesaplar."""
    n = len(events)
    return {
        "req_count": n,
        "distinct_paths": len({e["path_pattern"] for e in events}),
        "err_4xx_ratio": sum(400 <= int(e["status_code"]) <= 499 for e in events) / n if n else 0.0,
        "c401": sum(e["event_type"] == "ACCESS_DENIED" and int(e["status_code"]) == 401 for e in events),
        "c403": sum(int(e["status_code"]) == 403 for e in events),
        "c404": sum(int(e["status_code"]) == 404 for e in events),
        "login_fail_count": sum(e["event_type"] == "LOGIN_FAILURE" for e in events),
        "distinct_login_ids": len({e["login_identifier_hash"] for e in events if e["login_identifier_hash"]}),
        "write_ratio": sum(e["http_method"] in WRITE_METHODS for e in events) / n if n else 0.0,
        "admin_path_hits": sum(e["path_pattern"].startswith("/api/admin/") for e in events),
    }

def minute_key(occurred_at):
    # "2026-11-01T06:00:00.950340+00:00" -> "2026-11-01T06:00"
    return occurred_at[:16]

def convert(src, dst):
    groups = defaultdict(list)
    with open(src, newline="", encoding="utf-8") as f:
        for e in csv.DictReader(f):
            if int(e["status_code"]) == 429:
                continue
            groups[(e["ip_address"], minute_key(e["occurred_at"]))].append(e)
    rows = []
    for (ip, minute), ev in groups.items():
        r = {"ip_address": ip, "minute": minute}
        r.update(compute_features(ev))
        r["label"], r["profile"] = ev[0]["label"], ev[0]["profile"]
        rows.append(r)
    rows.sort(key=lambda r: (r["minute"], r["ip_address"]))
    cols = ["ip_address", "minute"] + FEATURES + ["label", "profile"]
    with open(dst, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=cols); w.writeheader(); w.writerows(rows)
    by_label = defaultdict(int)
    for r in rows:
        by_label[r["label"]] += 1
    print(f"{src} -> {dst}: {len(rows)} istemci-dakikası", dict(by_label))

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dir", default="data")
    a = ap.parse_args()
    convert(os.path.join(a.dir, "normal_events.csv"), os.path.join(a.dir, "features_normal.csv"))
    convert(os.path.join(a.dir, "attack_events.csv"), os.path.join(a.dir, "features_attack.csv"))

if __name__ == "__main__":
    main()