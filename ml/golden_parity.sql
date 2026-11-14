-- UniLink ML altın parite testi (golden parity)
-- Amaç: Java çalışma anı özellik SQL'i (MlRiskSweep, features.sql + NULLIF) ile Python build_features.py
-- AYNI sabit olaylarda birebir aynı 10 özelliği üretiyor mu? Beklenen değerler Python'dan (yetkili) üretildi.
-- Çalıştır: PostgreSQL konsolunda (DataGrip / psql). Sonuç 0 SATIR dönerse PARİTE TAM.
-- Satır dönerse o (ip, dakika) için SQL ile Python farklı; hangi sütun diye bakılır.

WITH ev(ip_address, occurred_at, event_type, http_method, path_pattern, status_code, login_identifier_hash) AS (
    VALUES
        ('10.0.0.1', TIMESTAMPTZ '2026-11-13T10:00:05+00:00', 'REQUEST', 'GET', '/api/postings', 200, ''),
        ('10.0.0.1', TIMESTAMPTZ '2026-11-13T10:00:05+00:00', 'REQUEST', 'GET', '/api/postings/{id}', 404, ''),
        ('10.0.0.1', TIMESTAMPTZ '2026-11-13T10:00:05+00:00', 'LOGIN_FAILURE', 'POST', '/api/auth/login', 401, 'hashA'),
        ('10.0.0.1', TIMESTAMPTZ '2026-11-13T10:00:05+00:00', 'LOGIN_FAILURE', 'POST', '/api/auth/login', 401, 'hashB'),
        ('10.0.0.1', TIMESTAMPTZ '2026-11-13T10:00:05+00:00', 'ACCESS_DENIED', 'GET', '/api/applications/{id}', 401, ''),
        ('10.0.0.1', TIMESTAMPTZ '2026-11-13T10:00:05+00:00', 'ACCESS_DENIED', 'GET', '/api/admin/users', 403, ''),
        ('10.0.0.1', TIMESTAMPTZ '2026-11-13T10:00:05+00:00', 'REQUEST', 'POST', '/api/postings', 201, ''),
        ('10.0.0.1', TIMESTAMPTZ '2026-11-13T10:00:05+00:00', 'REQUEST', 'GET', '/api/postings', 429, ''),
        ('10.0.0.2', TIMESTAMPTZ '2026-11-13T10:01:05+00:00', 'REQUEST', 'GET', '/api/postings', 200, ''),
        ('10.0.0.2', TIMESTAMPTZ '2026-11-13T10:01:05+00:00', 'REQUEST', 'GET', '/api/postings', 200, ''),
        ('10.0.0.3', TIMESTAMPTZ '2026-11-13T10:02:05+00:00', 'REQUEST', 'GET', '/api/postings', 200, ''),
        ('10.0.0.3', TIMESTAMPTZ '2026-11-13T10:02:05+00:00', 'LOGIN_FAILURE', 'POST', '/api/auth/login', 401, 'hashC')
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
    ('10.0.0.1', TIMESTAMPTZ '2026-11-13T10:00:00+00', 7, 5, 0.7142857142857143, 1, 1, 1, 2, 2, 0.42857142857142855, 1),
    ('10.0.0.2', TIMESTAMPTZ '2026-11-13T10:01:00+00', 2, 1, 0.0, 0, 0, 0, 0, 0, 0.0, 0),
    ('10.0.0.3', TIMESTAMPTZ '2026-11-13T10:02:00+00', 2, 2, 0.5, 0, 0, 0, 1, 1, 0.5, 0)
    )
SELECT
    coalesce(c.ip_address, e.ip_address)                              AS ip_address,
    coalesce(c.minute, e.minute)                                     AS minute,
  CASE WHEN c.ip_address IS NULL THEN 'SQL eksik'
       WHEN e.ip_address IS NULL THEN 'Python eksik'
       ELSE 'sutun farki' END                                      AS fark
FROM computed c
FULL OUTER JOIN expected e ON c.ip_address = e.ip_address AND c.minute = e.minute
WHERE c.ip_address IS NULL OR e.ip_address IS NULL
   OR c.req_count         <> e.req_count
   OR c.distinct_paths    <> e.distinct_paths
   OR abs(c.err_4xx_ratio  - e.err_4xx_ratio) > 1e-9
   OR c.c401              <> e.c401
   OR c.c403              <> e.c403
   OR c.c404              <> e.c404
   OR c.login_fail_count  <> e.login_fail_count
   OR c.distinct_login_ids<> e.distinct_login_ids
   OR abs(c.write_ratio    - e.write_ratio) > 1e-9
   OR c.admin_path_hits   <> e.admin_path_hits
ORDER BY ip_address, minute;