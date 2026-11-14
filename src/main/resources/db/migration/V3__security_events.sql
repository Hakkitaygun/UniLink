-- =====================================================================
-- V3__security_events.sql  |  Davranışsal güvenlik katmanı: olay deposu
-- Her API isteğinin güvenlik açısından önemli özeti burada tutulur.
-- KVKK gereği: istek gövdesi, sorgu parametreleri, parola ve e-posta SAKLANMAZ;
-- yol "şablon" hâlinde tutulur (/api/postings/{id}); giriş denemesinde sadece e-postanın hash'i.
-- Log tablosu olduğu için users'a yabancı anahtar yoktur (hızlı yazım, kullanıcı silinse de olay kalır).
-- =====================================================================
CREATE TABLE security_events (
                                 id                     UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                                 occurred_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                 event_type             VARCHAR(40)  NOT NULL,
                                 user_id                UUID,
                                 ip_address             VARCHAR(45)  NOT NULL,
                                 http_method            VARCHAR(10)  NOT NULL,
                                 path_pattern           VARCHAR(200) NOT NULL,
                                 status_code            INTEGER      NOT NULL,
                                 duration_ms            INTEGER      NOT NULL,
                                 user_agent             VARCHAR(200),
                                 login_identifier_hash  VARCHAR(64)
);

-- Kayan pencere sorguları: "bu IP / kullanıcı / e-posta, son N dakikada ne yaptı?"
CREATE INDEX idx_security_events_ip_time    ON security_events (ip_address, occurred_at DESC);
CREATE INDEX idx_security_events_user_time  ON security_events (user_id, occurred_at DESC) WHERE user_id IS NOT NULL;
CREATE INDEX idx_security_events_login_time ON security_events (login_identifier_hash, occurred_at DESC)
    WHERE login_identifier_hash IS NOT NULL;
-- Saklama süresi temizliği ve raporlama
CREATE INDEX idx_security_events_time       ON security_events (occurred_at DESC);
CREATE INDEX idx_security_events_type_time  ON security_events (event_type, occurred_at DESC);