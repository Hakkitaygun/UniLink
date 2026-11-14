#!/usr/bin/env python3
"""UniLink ML altın parite üreticisi: sabit "altın" olaylardan golden_parity.sql'i yeniden üretir.

Amaç: Java çalışma anı özellik SQL'i (MlRiskSweep) ile Python build_features.py AYNI olaylarda
birebir aynı 10 özelliği üretiyor mu? Beklenen değerler Python'dan (yetkili taraf) hesaplanır,
feature SQL'iyle birlikte karşılaştırma sorgusu olarak golden_parity.sql'e yazılır.
Çalıştır:  python make_golden_parity.py   (build_features.py ile aynı klasörde)
Sonra golden_parity.sql'i PostgreSQL konsolunda çalıştır → 0 satır = parite tam.
"""
import csv, os
import build_features as bf

COLS = ["id","occurred_at","event_type","user_id","ip_address","http_method",
        "path_pattern","status_code","duration_ms","user_agent","login_identifier_hash","label","profile"]

def ev(ip, mm, et, method, path, code, lh=""):
    return {"id":"","occurred_at":f"2026-11-13T10:{mm}:05+00:00","event_type":et,"user_id":"",
            "ip_address":ip,"http_method":method,"path_pattern":path,"status_code":code,
            "duration_ms":10,"user_agent":"UA","login_identifier_hash":lh,"label":"golden","profile":"golden"}

# Kenar durumlarını kapsayan altın olaylar:
EVENTS = [
    # Kova A (10.0.0.1 @ 10:00): her kenar durumu bir arada
    ev("10.0.0.1","00","REQUEST","GET","/api/postings",200),
    ev("10.0.0.1","00","REQUEST","GET","/api/postings/{id}",404),            # c404
    ev("10.0.0.1","00","LOGIN_FAILURE","POST","/api/auth/login",401,"hashA"),# login_fail + distinct_login
    ev("10.0.0.1","00","LOGIN_FAILURE","POST","/api/auth/login",401,"hashB"),
    ev("10.0.0.1","00","ACCESS_DENIED","GET","/api/applications/{id}",401),  # c401 (ACCESS_DENIED+401)
    ev("10.0.0.1","00","ACCESS_DENIED","GET","/api/admin/users",403),        # c403 + admin_path_hits
    ev("10.0.0.1","00","REQUEST","POST","/api/postings",201),                # write
    ev("10.0.0.1","00","REQUEST","GET","/api/postings",429),                 # 429 HARİÇ (hiç sayılmaz)
    # Kova B (10.0.0.2 @ 10:01): sade iki istek
    ev("10.0.0.2","01","REQUEST","GET","/api/postings",200),
    ev("10.0.0.2","01","REQUEST","GET","/api/postings",200),
    # Kova C (10.0.0.3 @ 10:02): boş login hash sayılmamalı (NULLIF testi)
    ev("10.0.0.3","02","REQUEST","GET","/api/postings",200),
    ev("10.0.0.3","02","LOGIN_FAILURE","POST","/api/auth/login",401,"hashC"),
]

def q(s): return "'" + str(s).replace("'","''") + "'"

def main():
    os.makedirs("gdata", exist_ok=True)
    with open("gdata/golden_events.csv","w",newline="",encoding="utf-8") as f:
        w=csv.DictWriter(f,fieldnames=COLS); w.writeheader(); w.writerows(EVENTS)
    bf.convert("gdata/golden_events.csv","gdata/golden_features.csv")      # Python = yetkili beklenen
    feat=list(csv.DictReader(open("gdata/golden_features.csv")))

    ev_rows=[f"  ({q(e['ip_address'])}, TIMESTAMPTZ {q(e['occurred_at'])}, {q(e['event_type'])}, "
             f"{q(e['http_method'])}, {q(e['path_pattern'])}, {e['status_code']}, {q(e['login_identifier_hash'])})"
             for e in EVENTS]
    exp_rows=[f"  ({q(r['ip_address'])}, TIMESTAMPTZ {q(r['minute']+':00+00')}, {r['req_count']}, {r['distinct_paths']}, "
              f"{r['err_4xx_ratio']}, {r['c401']}, {r['c403']}, {r['c404']}, {r['login_fail_count']}, "
              f"{r['distinct_login_ids']}, {r['write_ratio']}, {r['admin_path_hits']})" for r in feat]

    sql = f"""-- UniLink ML altın parite testi (otomatik üretildi: make_golden_parity.py)
-- Java çalışma anı özellik SQL'i (MlRiskSweep, features.sql + NULLIF) == Python build_features.py ?
-- Beklenen değerler Python'dan üretildi. PostgreSQL konsolunda çalıştır. 0 SATIR = PARİTE TAM.
-- Satır dönerse o (ip, dakika) için SQL ve Python farklı demektir.

WITH ev(ip_address, occurred_at, event_type, http_method, path_pattern, status_code, login_identifier_hash) AS (
VALUES
{",\n".join(ev_rows)}
),
computed AS (
  SELECT ip_address,
         date_trunc('minute', occurred_at)                                          AS minute,
         count(*)                                                                    AS req_count,
         count(DISTINCT path_pattern)                                                AS distinct_paths,
         coalesce(count(*) FILTER (WHERE status_code BETWEEN 400 AND 499)::float
                  / nullif(count(*), 0), 0)                                          AS err_4xx_ratio,
         count(*) FILTER (WHERE event_type = 'ACCESS_DENIED' AND status_code = 401)  AS c401,
         count(*) FILTER (WHERE status_code = 403)                                   AS c403,
         count(*) FILTER (WHERE status_code = 404)                                   AS c404,
         count(*) FILTER (WHERE event_type = 'LOGIN_FAILURE')                        AS login_fail_count,
         count(DISTINCT NULLIF(login_identifier_hash, ''))                           AS distinct_login_ids,
         coalesce(count(*) FILTER (WHERE http_method IN ('POST','PUT','PATCH','DELETE'))::float
                  / nullif(count(*), 0), 0)                                          AS write_ratio,
         count(*) FILTER (WHERE path_pattern LIKE '/api/admin/%')                    AS admin_path_hits
  FROM ev
  WHERE status_code <> 429
  GROUP BY ip_address, date_trunc('minute', occurred_at)
),
expected(ip_address, minute, req_count, distinct_paths, err_4xx_ratio, c401, c403, c404,
         login_fail_count, distinct_login_ids, write_ratio, admin_path_hits) AS (
VALUES
{",\n".join(exp_rows)}
)
SELECT coalesce(c.ip_address, e.ip_address) AS ip_address,
       coalesce(c.minute, e.minute)         AS minute,
       CASE WHEN c.ip_address IS NULL THEN 'SQL eksik'
            WHEN e.ip_address IS NULL THEN 'Python eksik'
            ELSE 'sutun farki' END          AS fark
FROM computed c
FULL OUTER JOIN expected e ON c.ip_address = e.ip_address AND c.minute = e.minute
WHERE c.ip_address IS NULL OR e.ip_address IS NULL
   OR c.req_count <> e.req_count OR c.distinct_paths <> e.distinct_paths
   OR abs(c.err_4xx_ratio - e.err_4xx_ratio) > 1e-9
   OR c.c401 <> e.c401 OR c.c403 <> e.c403 OR c.c404 <> e.c404
   OR c.login_fail_count <> e.login_fail_count OR c.distinct_login_ids <> e.distinct_login_ids
   OR abs(c.write_ratio - e.write_ratio) > 1e-9
   OR c.admin_path_hits <> e.admin_path_hits
ORDER BY ip_address, minute;
"""
    open("golden_parity.sql","w",encoding="utf-8").write(sql)
    print(f"{len(EVENTS)} altın olay, {len(feat)} (ip,dakika) -> golden_parity.sql")

if __name__ == "__main__":
    main()