-- =====================================================================
-- V1__init.sql  |  Proje Arkadaşı Bulma Platformu  |  PostgreSQL 13+
-- Konvansiyonlar:
--   * PK: UUID (gen_random_uuid(), PG13+ yerleşik)
--   * Enum alanlar: VARCHAR + CHECK  (JPA'da @Enumerated(EnumType.STRING))
--   * Zaman: TIMESTAMPTZ (JPA'da Instant)
--   * Sayısal alanlar INTEGER (Hibernate ddl-auto=validate ile uyumlu)
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;   -- ilan başlığı/organizasyon araması için

-- ---------------------------------------------------------------------
-- updated_at'i otomatik güncelleyen ortak trigger fonksiyonu
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at() RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ---------------------------------------------------------------------
-- USERS
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
    email_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    banned          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT uq_users_email          UNIQUE (email),
    CONSTRAINT ck_users_role           CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT ck_users_email_lower    CHECK (email = lower(email)),
    -- Öğrenciler yalnızca .edu.tr ile kayıt olabilir; admin hesabı bu kuralın dışında
    CONSTRAINT ck_users_email_edu      CHECK (role = 'ADMIN' OR email ~ '\.edu\.tr$')
);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- PROFILES  (users ile 1-1, PK = FK)
-- ---------------------------------------------------------------------
CREATE TABLE profiles (
    user_id        UUID         PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    full_name      VARCHAR(100) NOT NULL,
    university     VARCHAR(150) NOT NULL,
    department     VARCHAR(150) NOT NULL,
    bio            VARCHAR(1000),
    github_url     VARCHAR(255),
    linkedin_url   VARCHAR(255),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_profiles_github   CHECK (github_url   IS NULL OR github_url   ~* '^https://(www\.)?github\.com/.+'),
    CONSTRAINT ck_profiles_linkedin CHECK (linkedin_url IS NULL OR linkedin_url ~* '^https://([a-z]{2,3}\.)?linkedin\.com/.+')
);

CREATE INDEX idx_profiles_university ON profiles (university);
CREATE INDEX idx_profiles_department ON profiles (department);

CREATE TRIGGER trg_profiles_updated_at
    BEFORE UPDATE ON profiles
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- VERIFICATION_TOKENS  (e-posta doğrulama + şifre sıfırlama)
-- Token'ın kendisi değil, SHA-256 hash'i saklanır.
-- ---------------------------------------------------------------------
CREATE TABLE verification_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL,
    type        VARCHAR(30)  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_verification_tokens_hash UNIQUE (token_hash),
    CONSTRAINT ck_verification_tokens_type CHECK (type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET'))
);

CREATE INDEX idx_verification_tokens_user    ON verification_tokens (user_id, type);
CREATE INDEX idx_verification_tokens_expires ON verification_tokens (expires_at);

-- ---------------------------------------------------------------------
-- REFRESH_TOKENS  (JWT refresh; logout / ban durumunda iptal edilebilsin)
-- ---------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_user    ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens (expires_at);

-- ---------------------------------------------------------------------
-- TECHNOLOGIES  (profil ve ilan için ortak taxonomy)
-- ---------------------------------------------------------------------
CREATE TABLE technologies (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(50)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- "Java" ile "java" ikinci kez eklenemesin
CREATE UNIQUE INDEX uq_technologies_name_lower ON technologies (lower(name));

-- ---------------------------------------------------------------------
-- PROFILE_TECHNOLOGIES  (profiles <-> technologies)
-- ---------------------------------------------------------------------
CREATE TABLE profile_technologies (
    profile_id     UUID NOT NULL REFERENCES profiles (user_id)  ON DELETE CASCADE,
    technology_id  UUID NOT NULL REFERENCES technologies (id)   ON DELETE RESTRICT,
    PRIMARY KEY (profile_id, technology_id)
);

CREATE INDEX idx_profile_technologies_tech ON profile_technologies (technology_id);

-- ---------------------------------------------------------------------
-- POSTINGS
-- ---------------------------------------------------------------------
CREATE TABLE postings (
    id                 UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id           UUID          NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    type               VARCHAR(20)   NOT NULL,
    title              VARCHAR(150)  NOT NULL,
    description        VARCHAR(5000) NOT NULL,
    organization_name  VARCHAR(150),            -- yarışma / organizasyon adı (opsiyonel)
    stage              VARCHAR(20),             -- sadece STARTUP ilanları için
    benefits_note      VARCHAR(500),            -- ekibe katılan ne kazanıyor (hisse/ücret/portfolyo)
    status             VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    deadline           TIMESTAMPTZ   NOT NULL,
    created_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_postings_type   CHECK (type   IN ('PROJECT', 'COMPETITION', 'STARTUP')),
    CONSTRAINT ck_postings_status CHECK (status IN ('OPEN', 'CLOSED', 'CANCELLED')),
    CONSTRAINT ck_postings_stage  CHECK (stage IS NULL OR stage IN ('IDEA', 'PROTOTYPE', 'PRODUCT')),
    CONSTRAINT ck_postings_stage_startup CHECK (stage IS NULL OR type = 'STARTUP'),
    CONSTRAINT ck_postings_title_not_blank CHECK (length(btrim(title)) > 0)
);

-- Anasayfa akışı: sadece açık ilanlar, yeniden eskiye
CREATE INDEX idx_postings_feed          ON postings (created_at DESC) WHERE status = 'OPEN';
-- Otomatik kapatma job'ı: süresi dolan açık ilanları bulur
CREATE INDEX idx_postings_open_deadline ON postings (deadline)        WHERE status = 'OPEN';
CREATE INDEX idx_postings_owner         ON postings (owner_id, status);
CREATE INDEX idx_postings_type          ON postings (type)            WHERE status = 'OPEN';
-- "Ara" çubuğu: ILIKE '%...%' sorguları için trigram
CREATE INDEX idx_postings_title_trgm    ON postings USING gin (title             gin_trgm_ops);
CREATE INDEX idx_postings_org_trgm      ON postings USING gin (organization_name gin_trgm_ops);

CREATE TRIGGER trg_postings_updated_at
    BEFORE UPDATE ON postings
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- POSTING_ROLES  (rol bazlı kontenjan, örn. 2 Backend, 1 ML)
-- ---------------------------------------------------------------------
CREATE TABLE posting_roles (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    posting_id      UUID        NOT NULL REFERENCES postings (id) ON DELETE CASCADE,
    role            VARCHAR(50) NOT NULL,
    required_count  INTEGER     NOT NULL,
    CONSTRAINT uq_posting_roles_posting_role UNIQUE (posting_id, role),
    CONSTRAINT ck_posting_roles_count        CHECK (required_count BETWEEN 1 AND 50)
);

CREATE INDEX idx_posting_roles_posting ON posting_roles (posting_id);

-- ---------------------------------------------------------------------
-- POSTING_TECHNOLOGIES  (postings <-> technologies)
-- ---------------------------------------------------------------------
CREATE TABLE posting_technologies (
    posting_id     UUID NOT NULL REFERENCES postings (id)     ON DELETE CASCADE,
    technology_id  UUID NOT NULL REFERENCES technologies (id) ON DELETE RESTRICT,
    PRIMARY KEY (posting_id, technology_id)
);

CREATE INDEX idx_posting_technologies_tech ON posting_technologies (technology_id);

-- ---------------------------------------------------------------------
-- APPLICATIONS
-- Başvurular silinmez: ilan CANCELLED olsa bile "Başvurulan" sayfasında kalır.
-- accepted_role_id: kabul edilen başvurunun hangi role yerleştirildiği.
--   Kontenjan = COUNT(applications WHERE accepted_role_id = :role AND status = 'ACCEPTED')
-- ---------------------------------------------------------------------
CREATE TABLE applications (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    posting_id        UUID          NOT NULL REFERENCES postings (id) ON DELETE RESTRICT,
    applicant_id      UUID          NOT NULL REFERENCES users (id)    ON DELETE RESTRICT,
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    message           VARCHAR(1000),
    accepted_role_id  UUID          REFERENCES posting_roles (id) ON DELETE RESTRICT,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    decided_at        TIMESTAMPTZ,
    CONSTRAINT uq_applications_posting_applicant UNIQUE (posting_id, applicant_id),
    CONSTRAINT ck_applications_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'WITHDRAWN')),
    -- ACCEPTED <=> accepted_role_id dolu
    CONSTRAINT ck_applications_accepted_role CHECK (
        (status = 'ACCEPTED' AND accepted_role_id IS NOT NULL) OR
        (status <> 'ACCEPTED' AND accepted_role_id IS NULL)
    )
);

-- "Başvurulan" sayfası
CREATE INDEX idx_applications_applicant     ON applications (applicant_id, created_at DESC);
-- İlan sahibinin başvuru listesi + bekleyen başvuru rozeti (COUNT)
CREATE INDEX idx_applications_posting       ON applications (posting_id, status);
-- Rol bazlı kontenjan sayımı
CREATE INDEX idx_applications_accepted_role ON applications (accepted_role_id) WHERE status = 'ACCEPTED';

CREATE TRIGGER trg_applications_updated_at
    BEFORE UPDATE ON applications
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------
-- APPLICATION_ROLES  (başvuran hangi role(ler)e başvurdu)
-- ---------------------------------------------------------------------
CREATE TABLE application_roles (
    application_id   UUID NOT NULL REFERENCES applications (id)  ON DELETE CASCADE,
    posting_role_id  UUID NOT NULL REFERENCES posting_roles (id) ON DELETE RESTRICT,
    PRIMARY KEY (application_id, posting_role_id)
);

CREATE INDEX idx_application_roles_role ON application_roles (posting_role_id);

-- Kabul edilen rol, başvurunun seçtiği roller arasından olmak zorunda
-- (accepted_role_id NULL iken kontrol uygulanmaz)
ALTER TABLE applications
    ADD CONSTRAINT fk_applications_accepted_among_applied
    FOREIGN KEY (id, accepted_role_id)
    REFERENCES application_roles (application_id, posting_role_id);

-- ---------------------------------------------------------------------
-- SAVED_POSTINGS  (Kaydedilenler)
-- ---------------------------------------------------------------------
CREATE TABLE saved_postings (
    user_id     UUID        NOT NULL REFERENCES users (id)    ON DELETE CASCADE,
    posting_id  UUID        NOT NULL REFERENCES postings (id) ON DELETE CASCADE,
    saved_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, posting_id)
);

CREATE INDEX idx_saved_postings_user_time ON saved_postings (user_id, saved_at DESC);
CREATE INDEX idx_saved_postings_posting   ON saved_postings (posting_id);
