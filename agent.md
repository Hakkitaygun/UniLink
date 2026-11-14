# UniLink – Proje Arkadaşı Bulma Platformu (agent.md)

Bu dosya, projede çalışan bir geliştirici ya da yapay zekâ asistanı için bağlam özetidir:
projenin amacı, alınan kararlar, şu ana kadar yapılanlar ve sıradaki adımlar.

**Durum özeti (2026-11-13):** Backend çekirdeği **tamamlandı ve test edildi**: auth (parola değiştirme dahil), profil/teknoloji,
ilan modülü, başvuru modülü, kaydedilenler, `saved` bayrağı ve İlanlarım. Danışman isteğiyle eklenen **Uyarlanabilir Backend
Güvenlik Çerçevesi**nin kural tabanlı kısmı (olay toplama, 4 kurallı risk motoru, karar motoru, engelleme filtresi) **uygulandı
ve 81/81 doğrulamalı saldırı simülasyonuyla doğrulandı**. ML kısmı başladı: özellik sözleşmesi, sentetik veri üreticileri,
özellik tablosu ve ilk model eğitimi (Isolation Forest + kNN) hazır. Üç Postman koleksiyonu (235 istek) yeşil. Ara rapor
("Proje Kapsamı ve Gereksinimler Dokümanı") yazıldı. Arayüzü bir arkadaş yazıyor (React). **Teslim hedefi: 20 Aralık 2026.**
**Sırada:** normal sentetik veriyi zorlaştırma (v2), kural vs ML vs hibrit karşılaştırması, ML puanlama servisi + hibrit skor,
admin izleme API'leri, Testcontainers/CI/deploy, final rapor ve demo.

---

## 1. Proje özeti

Bitirme projesi. Sadece üniversite e-postasıyla (`.edu.tr`) hesap açılabilen, öğrencilerin ilan açarak
proje arkadaşı / ekip üyesi bulabildiği bir web sitesi.

- **Backend'i kullanıcı yazıyor; frontend'i (React) bir arkadaşı yazıyor.** API'nin frontend'in bir adım önünde gitmesi,
  Swagger/README/rehber/Docker ile hazır tutulması öncelik.
- **Süre:** teslim hedefi **20 Aralık 2026**. Plan: 6 Aralık özellik dondurma, 7–9 Aralık ayarlama/demo, 10–16 Aralık rapor ve sunum, 17–20 Aralık tampon.
- **Kapsam dışı (bu dönem):** mesajlaşma, profile CV/projeler (V3 profil migration'ı), hesap kapatma, admin paneli arayüzü, Redis, mikroservis.
- **Danışman isteği:** siber güvenlik uzmanı danışman, e-ticaret şablonundaki gibi bir "Adaptif Backend Güvenlik Çerçevesi (ML ile)" katmanı istedi
  (davranışsal olay toplama, kural tabanlı risk motoru, ML anomali tespiti, hibrit risk skoru, karar motoru, izleme). Bkz. bölüm 6.
- **gRPC kullanılmayacak** (tek servis, tarayıcı istemcisi; gerek yok). **Redis** şimdilik kullanılmıyor (risk skoru bellek içi tutuluyor; çok örnekli çalışmaya geçilirse aday).

### Verilen kararlar
- **İlan türleri:** `PROJECT`, `COMPETITION`, `STARTUP`. Tek `postings` tablosu.
- **Kayıt:** Tüm `.edu.tr` e-postaları; e-posta doğrulaması zorunlu.
- **"Şirket" alanı:** `organization_name` – yarışma/organizasyon adı, opsiyonel.
- **Bildirim:** Başvuru gelince e-posta gitmez; ilan sahibi site içinden görür (bekleyen sayı rozeti). Spring Mail sadece doğrulama ve şifre sıfırlama için.
- **İlan süresi:** Süresi dolan ilan otomatik `CLOSED` olur; akıştan düşer ama başvuranlar görmeye devam eder, detayı açılır (`open: false`).
- **Görünürlük:** İlanlar ve profiller sadece giriş yapmış kullanıcılara görünür. `CANCELLED` ilanı sadece sahibi görür (başkası 404).
- **Teknolojiler (LinkedIn mantığı):** Listeden seçilir ya da yazılan yeni teknoloji eklenir (isme göre bul-ya-da-oluştur).
- **Başvuru kuralları:** Kendi ilanına başvuru yok; sadece açık ilana; ilan başına tek başvuru (geri çekilen yeniden yapılabilir ve aynı kayıt yeniden açılır, reddedilen yeniden başvuramaz); başvuran 1-5 rol seçer; dolu role başvuru engellenmez, **kabul** engellenir.
- **Gizlilik:** Başvuranın e-postası ilan sahibine **sadece kabulden sonra** görünür.
- **Kaydedilenler:** Kapanmış ilan listede kalır (`open: false`), iptal edilen ilan listeden kalkar.
- **CV planı:** Sadece profil gösterimi yapıldı; `cvUrl`/Projeler bölümü ve PDF yükleme bu dönem **kapsam dışı**.

### Arayüz taslağı (wireframe)
- Sol sidebar: Anasayfa, Mesaj, Profil, Settings, İlan aç, Kaydedilenler, Başvurulan, Logout
- Üst bar: Filtrele butonu, Ara çubuğu
- Ana alan: 2x2 ilan kartı grid'i. Kart: Başlık, Bölüm, Teknoloji, Organizasyon adı (varsa), Açıklama, kalan kontenjan, son tarih, "İncele"

---

## 2. Teknik yığın

| Konu | Seçim |
|---|---|
| Dil / JDK | Java 25 (Temurin) |
| Framework | Spring Boot 4.1.1 (Spring Framework 7, Spring Security 7, Hibernate 7.4.5) |
| Build | Maven |
| Veritabanı | PostgreSQL 16 (Docker, **host portu 5433**) |
| Migration | Flyway 12.4 (`flyway-database-postgresql` bağımlılığı ekli) |
| ORM | Spring Data JPA (+ `JpaSpecificationExecutor`), `ddl-auto=validate` |
| Güvenlik | Spring Security, BCrypt, stateless JWT (HS256) — **Spring'in kendi resource-server desteği (Nimbus)**; `jjwt` ve `UserDetailsService` kullanılmıyor |
| API dokümantasyonu | springdoc-openapi 3.0.3 (`springdoc-openapi-starter-webmvc-ui`), Swagger UI |
| Mail (geliştirme) | Mailpit (SMTP `localhost:1025`, arayüz/API `http://localhost:8025`) |
| Diğer | Lombok, Bean Validation, DevTools |
| IDE / test aracı | IntelliJ IDEA 2025.2 (Windows), Postman |

- Proje klasörü: `Desktop/UniLink/backend`; ana paket `com.hakkitaygun.unilink`; ana sınıf `BackendApplication`
- Konfigürasyon: **`application.properties`** (yml yok)
- Önemli bağımlılık adı: `spring-boot-starter-security-oauth2-resource-server` (Boot 4'te starter adı değişti)

### Docker
- `compose.yaml`: `db` (postgres:16, db/kullanıcı/şifre `unilink`, `5433:5432`, healthcheck), `mailpit`, ve **`profiles: ["full"]`** arkasında `app`.
- Geliştirirken compose'taki ▶ sadece `db` + `mailpit`'i başlatır, uygulama IntelliJ'den çalışır. Tam ortam (arkadaş için):
  `docker compose --profile full up --build`; durdurma `docker compose --profile full down` (veri kalır, `-v` siler).
- `Dockerfile` iki aşamalı (`eclipse-temurin:25-jdk` / `25-jre`), root olmayan kullanıcı. `app` konteyneri derlendiği andaki koda sahiptir; kod değişince `--build`.
- Veritabanını sıfırlama: uygulamayı durdur → `docker compose --profile full down -v` → compose ▶ → uygulamayı çalıştır.

### Ayarlar (`application.properties`)
```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/unilink   # kullanıcı/şifre: unilink
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.default_batch_fetch_size=50    # liste sorgularında N+1'i önler
spring.mail.host=localhost
spring.mail.port=1025
server.error.include-stacktrace=never
app.frontend-url=http://localhost:3000                          # doğrulama ve şifre sıfırlama linkleri frontend sayfasına gider
app.cors.allowed-origins=http://localhost:3000,http://localhost:5173
app.jwt.secret=${JWT_SECRET:<geliştirme-için-varsayılan>}       # canlıda JWT_SECRET ortam değişkeni zorunlu, min. 32 karakter
app.jwt.access-token-minutes=15
app.jwt.refresh-token-days=7
springdoc.swagger-ui.persist-authorization=true
# isteğe bağlı: app.scheduling.close-expired-postings-ms (60000), app.scheduling.cleanup-tokens-ms (3600000)

# --- Güvenlik çerçevesi (varsayılanlar güvenli: gözlem modu, forwarded header'a güvenme) ---
app.security.events.enabled=true
app.security.events.retention-days=30
app.security.trust-forwarded-headers=false        # sadece simülasyon testinde true
app.security.enforcement.enabled=false            # false = gözlem modu; true = engelleme açık
app.security.enforcement.throttle-per-minute=30
app.security.enforcement.exempt-ips=              # boş olmalı (dolu olursa test IP'leri muaf kalır)
app.security.risk.half-life-minutes=10
app.security.risk.hold-seconds=60                 # puan 60 sn sabit kalır, sonra yarı ömürle azalır
app.security.risk.block-minutes=15                # skor >= 80 olunca engel damgası
app.security.risk.failed-login.threshold=5  / window-seconds=600 / points=20
app.security.risk.rate.threshold=100        / window-seconds=10  / points=20
app.security.risk.resource-probing.threshold=5 / window-seconds=60  / points=30
app.security.risk.admin-probe.threshold=3   / window-seconds=600 / points=40
# ML hibrit katmanı (gözlem modu: enabled=false; canlı skorlama için true + uvicorn açık)
app.security.ml.enabled=false
app.security.ml.base-url=http://127.0.0.1:8000
app.security.ml.timeout-ms=800
app.security.ml.sweep-ms=60000                    # dakikalık ML taraması (biten dakikayı skorlar)
```

### Paket yapısı (katman bazlı)
Her modül `controller`, `service`, `repository`, `entity`, `enums`, `dto`, `exception` kullanır; ek olarak `posting.specification`,
`posting.mapper`, `posting.scheduler`, `auth.scheduler`, `common.util`. Yeni kod bu yapıya uymalı.
(`SavedPostingId` `posting` kökünde duruyor; `posting.entity`'ye taşınabilir.)

```
com.hakkitaygun.unilink
├── common      (entity: BaseEntity, AuditableEntity | dto: PageResponse | exception: ApiException, ApiError, GlobalExceptionHandler | util: TextNormalizer)
├── config      (SecurityConfig, JwtConfig, CorsConfig, OpenApiConfig, SchedulingConfig)
├── auth        (AuthService, JwtService, TokenGenerator, RefreshTokenService, EmailService, TokenCleanupService + scheduler)
├── user        (User, Profile, Role, ProfileService, UserController)
├── technology  (Technology, TechnologyService, TechnologyController)
├── posting     (Posting, PostingRole, SavedPosting; PostingService, MyPostingService, SavedPostingService; PostingSpecifications; PostingSummaryMapper; PostingScheduler)
├── application (Application, ApplicationService, ApplicationController)
├── (AccountService + AccountController: parola değiştirme)
└── security    (entity: SecurityEvent | enums: SecurityEventType | repository | service: SecurityEventService, AsyncConfig |
                 filter: SecurityEventFilter, RiskEnforcementFilter | risk: RiskScoreStore, SlidingWindowCounter, kurallar, DecisionEngine, RiskBand,
                 ClientRequestInfo | ml: MlFeatures, MlScoringClient, MlRiskSweep | admin: AdminSecurityController |
                 scheduler: SecurityEventCleanupScheduler)
```
Proje kökünde (`backend/ml/`, `src` dışında): ML betikleri + FastAPI servisi (`ml_service.py`) + `models/` (knn_model.joblib, iforest.joblib, model_meta.json). `.gitignore`: `ml/data/`, `ml/__pycache__/`; `.dockerignore`: `ml/`.

---

## 3. Veritabanı şeması (Flyway)

- `V1__init.sql`: 12 tablo (aşağıda); `V2__seed_technologies.sql`: ~45 başlangıç teknolojisi; `V3__security_events.sql`: güvenlik olayı günlüğü (13 tablo toplam).
- **Kural:** Uygulanmış bir migration dosyası asla düzenlenmez (checksum hatası). Yeni değişiklik için `V3__...`.

Tablolar:
- `users` – e-posta (küçük harf + `.edu.tr` CHECK, admin muaf), password_hash, role (USER/ADMIN), email_verified, banned, deleted_at
- `profiles` – PK = FK `user_id`: full_name, university, department, bio, github_url, linkedin_url
- `technologies` – ortak liste (isim `lower(name)` ile unique)
- `profile_technologies`, `posting_technologies` – çoka-çok
- `verification_tokens` (EMAIL_VERIFICATION / PASSWORD_RESET), `refresh_tokens` – token'ların **hash'i** saklanır
- `postings` – owner, type, title, description, organization_name, stage (sadece STARTUP), benefits_note, status (OPEN/CLOSED/CANCELLED), deadline
- `posting_roles` – rol bazlı kontenjan (`required_count` 1–50, `(posting_id, role)` unique)
- `applications` – posting, applicant, status (PENDING/ACCEPTED/REJECTED/WITHDRAWN), message, `accepted_role_id`, decided_at; `(posting_id, applicant_id)` unique
- `application_roles` – başvuranın seçtiği roller
- `saved_postings` – PK `(user_id, posting_id)`, saved_at
- `security_events` – 11 sütun: `id` (uuid), `occurred_at`, `event_type` (REQUEST/LOGIN_SUCCESS/LOGIN_FAILURE/ACCESS_DENIED), `user_id` (anonimse boş),
  `ip_address` (45 kr, IPv6 sığar; ham IP), `http_method`, `path_pattern` (şablon, bkz. bölüm 6), `status_code`, `duration_ms`, `user_agent` (200),
  `login_identifier_hash` (SHA-256, 64 kr; düz e-posta tutulmaz). 5 indeks. Gövde/sorgu/parola/e-posta **kaydedilmez** (KVKK); 30 gün sonra silinir.

DB seviyesindeki kurallar: `ACCEPTED` ⇔ `accepted_role_id` dolu; kabul edilen rol başvuranın seçtiği roller arasında (composite FK);
`stage` sadece `STARTUP`; `updated_at` trigger'ları; `pg_trgm` GIN index'leri (şu an `lower()`/`translate()` ile arandığı için kullanılmıyor);
akış için kısmi index'ler.

---

## 4. Yapılanlar

Her adım küçük parça halinde yazıldı, uygulama çalıştırılıp doğrulandı.

**Altyapı:** Docker (Postgres + Mailpit + tam ortam profili), Flyway V1/V2, CORS, Swagger, README, `SchedulingConfig` (+ `@Scheduled` işler)

**Güvenlik ve auth:** `SecurityConfig` (BCrypt, CSRF kapalı, stateless; `/api/auth/**`, `/error`, Swagger açık; `role` claim'i `ROLE_` önekiyle yetkiye çevrilir), `JwtConfig`, `JwtService`,
`TokenGenerator` (SecureRandom 32 bayt hex + SHA-256), `RefreshTokenService` (create/consume/revoke/revokeAll; rotasyon), `EmailService`, `AuthService`, `TokenCleanupService`.

**Hata yönetimi:** `ApiException` (HttpStatus taşır), `ApiError` (`fieldErrors` ile), `GlobalExceptionHandler` (`ApiException`, `@Valid`, bozuk JSON, parametre türü/eksik parametre → 400; `DataIntegrityViolationException` → 409; beklenmeyen → genel 500, detay log'a).

**Profil/teknoloji:** `ProfileService`, `TechnologyService` (`getAll`, `search`, `resolveOrCreate`).

**İlan modülü:** `PostingService` (create, getById, getFeed, update, cancel, closeExpired), `PostingSpecifications` (isOpen, hasType, matchesQuery, hasAnyTechnology, hasRole,
ownerDepartmentContains, ownerUniversityContains, **hasVacancy**; Türkçe harf duyarsız arama), `PostingSummaryMapper` (kart + gerçek kontenjan sayıları + `saved`),
`MyPostingService` (İlanlarım + başvuru sayıları), `SavedPostingService`, `PostingScheduler` (süresi dolanları `CLOSED` yapar, dakikada bir).

**Başvuru modülü:** `ApplicationService` (apply, getMine, getForPosting, countPending, accept, reject, withdraw). Kabul: başvuru ve rol satırı `PESSIMISTIC_WRITE` ile kilitlenir, kabul sayısı kilitten sonra sayılır (kontenjan aşılamaz).
`Application.reapply` ile geri çekilen başvuru yeniden açılır.

**Ayarlar:** `AccountService.changePassword` — mevcut parola yanlışsa **400** (frontend 401'i oturum bitti sanmasın), başarıda tüm refresh token'lar iptal edilir ve yeni token çifti döner.

**Güvenlik çerçevesi:** bkz. bölüm 6 (olay toplama, risk motoru, karar motoru, ML).

### Endpoint'ler (`/api/auth/**` açık, diğerleri `Authorization: Bearer <accessToken>` ister)

| Endpoint | Başarı | Önemli hatalar |
|---|---|---|
| `POST /api/auth/register` | 201 | 400 (`.edu.tr` değil), 409 |
| `POST /api/auth/verify-email` `{token}` | 204 | 400 |
| `POST /api/auth/resend-verification` | 202 (her zaman) | 400 |
| `POST /api/auth/login` | 200 (token çifti) | 401, 403 (doğrulanmamış / devre dışı) |
| `POST /api/auth/refresh` | 200 | 401, 403 |
| `POST /api/auth/logout` | 204 | 400 |
| `POST /api/auth/forgot-password` | 202 (her zaman) | 400 |
| `POST /api/auth/reset-password` | 204 | 400 |
| `GET /api/users/me` | 200 | 401, 404 |
| `PUT /api/users/me` | 200 | 400 |
| `PUT /api/users/me/technologies` `{technologies:[isimler]}` | 200 | 400 |
| `POST /api/users/me/password` `{currentPassword,newPassword}` | 200 (yeni token çifti) | 400 (yanlış mevcut parola / zayıf yeni), 401 |
| `GET /api/users/me/postings?status=&page=&size=` | 200 `Page<MyPostingResponse>` (`posting`, `status`, `applicationCounts`) | 400 (status), 401 |
| `GET /api/technologies?query=` | 200 | 401 |
| `GET /api/postings` (+ filtreler) | 200 `Page<PostingSummaryResponse>` | 400 (sort/type/hasVacancy) |
| `GET /api/postings/{id}` | 200 `PostingResponse` | 400, 404 |
| `POST /api/postings` | 201 | 400 |
| `PUT /api/postings/{id}` | 200 | 400, 403, 404 |
| `DELETE /api/postings/{id}` | 204 (iptal, geri alınamaz) | 403, 404 |
| `POST /api/postings/{id}/applications` `{message?, roleIds}` | 201 | 400, 404, 409 |
| `GET /api/applications/mine` | 200 | 401 |
| `GET /api/postings/{id}/applications?status=` | 200 `Page<IncomingApplicationResponse>` | 403, 404 |
| `GET /api/applications/incoming/pending-count` | 200 `{pendingCount}` | 401 |
| `POST /api/applications/{id}/accept` `{roleId}` | 200 | 400, 404, 409 (kontenjan dolu / sonuçlanmış) |
| `POST /api/applications/{id}/reject` | 200 | 400, 404, 409 |
| `POST /api/applications/{id}/withdraw` | 200 | 404, 409 |
| `PUT /api/saved-postings/{postingId}` | 204 (tekrar çağrılabilir) | 400, 404 (iptal/yok) |
| `DELETE /api/saved-postings/{postingId}` | 204 (her zaman) | 400 |
| `GET /api/saved-postings` | 200 `Page<PostingSummaryResponse>` | 401 |

**429 (güvenlik katmanı):** engelleme modu açıkken yüksek riskli istemciler `429` + `Retry-After` alır (gövdede skor/risk bilgisi yok). Frontend 429'da
`Retry-After` kadar bekleyip yeniden denemeli; 429 yanıtı CORS başlıklarıyla gelir. Yüksek bandda çıkış (`logout`) ve `refresh` hariç yazma istekleri engellenir.

`GET /api/postings` parametreleri: `page` (0'dan), `size` (varsayılan 12, 1–50), `sort` (`NEWEST` | `DEADLINE`), `type`, `query` (başlık/organizasyon),
`technologies` (tekrarlı parametre, "herhangi biri", en fazla 10), `role`, `department`, `university`, `hasVacancy`. Sadece `status = OPEN` ve `deadline > şimdi`.
Yanıtlarda `saved` (isteği atan kullanıcıya göre) bulunur: `PostingSummaryResponse` ve `PostingResponse`.

### Kararlar ve davranışlar (özet)
- **Token'lar DB'de hash**; süreler: access 15 dk, refresh 7 gün, doğrulama linki 24 saat, şifre sıfırlama 1 saat. **Refresh rotasyonu**: tek kullanımlık. Şifre sıfırlanınca tüm refresh token'lar iptal.
- **E-posta ifşası yok** (`resend-verification`, `forgot-password` her zaman 202; giriş hatası tek mesaj). Mail sıklık sınırı: aynı kullanıcı için 1 dakikada bir.
- **Kimlik token'dan:** `@AuthenticationPrincipal Jwt jwt` → `jwt.getSubject()`; istekten (path/body) alınmaz.
- **404 ile gizleme:** sahibi olmayanın başvuru kararı/geri çekme denemesi ve iptal edilmiş ilana erişim 404 döner (varlığı sızdırmamak için); sahibi olmayanın ilan düzenleme/iptal/gelen başvuru listesi 403 döner.
- **Rol token'ın içinde:** ban/rol değişimi access token süresi dolana kadar yansımaz; ban refresh sırasında yakalanır.
- **Teknoloji isimleri:** 2–50 karakter, harf/rakam ve `. + # / & -`; yazım listedekine uydurulur; en fazla 20.

---

## 5. Kod kuralları

- Entity'lerde `@Data` yok; `@Getter/@Setter/@NoArgsConstructor` (User ve Profile'da `@Builder`). İlişkiler `LAZY`. Koleksiyonlar `Set`. Enum'lar `@Enumerated(EnumType.STRING)`.
- **E-posta normalizasyonu:** `User.normalizeEmail` (`Locale.ROOT`); `findByEmail` öncesi uygulanır; `@Builder` `setEmail`'i atlar.
- **Entity API'den döndürülmez;** DTO (Java `record`, `from(...)` statik metodu). Lazy ilişkileri okuyan dönüşümler **`@Transactional` servis metodunun içinde** yapılır; mapper'lar `@Transactional` değildir, çağıran servis işlemi açık tutar.
- Liste sorgularında N+1: tekil sorguda `@EntityGraph`; sayfalı listede `default_batch_fetch_size` (koleksiyonu sayfalamayla `JOIN FETCH` etme).
- Yeni iş kuralı hataları `ApiException`'dan türetilir; `@ResponseStatus` kullanılmaz.
- `@Transactional` = Spring'inki; sadece okuyanlarda `readOnly = true`.
- Doğrulama kuralları request record'unda; temizleme (trim vb.) compact constructor'da, doğrulamadan önce.
- Servis sorumlulukları ayrı; başka modülün mantığı o modülün servisinden çağrılır.
- **Dinamik filtre = `Specification`**; filtre yoksa `Specification.unrestricted()`. Listeye bağlı filtreler alt sorguyla. Arama metinleri `TextNormalizer.fold` (Java) ve `translate(...)` (DB) ile aynı biçimde katlanır. Sıralama parametreleri enum beyaz listesi; sayfa boyutu 1–50.
- **Kritik işlemler satır kilidiyle** (`findByIdForUpdate`); tekrar çağrılabilir uçlar (`kaydet`, `kaydı kaldır`, `logout`) hata vermez. Toplu "yoksa ekle" için `insert ... on conflict do nothing`.
- Uygulanmış Flyway migration'ı düzenlenmez.

---

## 6. Adaptif Backend Güvenlik Çerçevesi (danışman isteği)

Davranışsal güvenlik: istek **içeriğine** değil, istemci davranışına (hız, hata oranı, erişim örüntüsü) bakar.
**İstek yolculuğu:** `RiskEnforcementFilter` (CORS'tan sonra) istemcinin güncel skoruna bakıp geçirir/yavaşlatır/engeller → uygulama işler →
`SecurityEventFilter` (en dışta, `HIGHEST_PRECEDENCE`) yanıttan sonra olayı kaydeder, kuralları çalıştırır, skoru günceller, DB'ye **asenkron** yazar
(sınırlı kuyruk, `DiscardPolicy`: dolarsa olay atılır, istek bloklanmaz). Skorlama bellek içi ve senkron.

**Kurallar** (kayan pencere sayaçları): `FailedLoginRule` (5/600 sn → +20), `HighRequestRateRule` (100/10 sn → +20),
`ResourceProbingRule` (5/60 sn 403/404 → +30), `AdminProbeRule` (3/600 sn yetkisiz admin → +40).
**Skor** (`RiskScoreStore`): puanlar birikir; 60 sn sabit kalır, sonra yarı ömürle (10 dk) azalır; ≥80 → 15 dk engel damgası.
**Karar** (`DecisionEngine`, bantlar `RiskBand`): LOW 0-29 izin; MEDIUM 30-59 dakikada >30 istek → 429 throttle; HIGH 60-79 yazma engeli (logout/refresh hariç); CRITICAL 80+ hepsi engelle. (Engel damgası eşiği de 80 — band ile tutarlı.)
**Gözlem modu varsayılan** (`enforcement.enabled=false`); önce yanlış pozitifler ölçülür.

**Canlı ML entegrasyonu (hibrit, çalışıyor):** `MlRiskSweep` (`@Scheduled`, dakikada bir, `app.security.ml.sweep-ms`) biten dakikanın olaylarından **aynı `features.sql` mantığıyla** (JdbcTemplate, tek dakika penceresi + `NULLIF(login_identifier_hash,'')`) her IP için 10 özelliği çeker → `MlScoringClient` FastAPI servisine sorar → `ml_score > 0` olanlara `0.4 × ml_score × 100` puanını `RiskScoreStore.add("ip:" + ip, ...)` ile **kural puanıyla aynı kovaya** ekler (en çok +40). Servis kapalı/erişilemezse `ml_score=0` döner, sistem yalnızca kural skoruyla çalışır (fallback). `app.security.ml.enabled=false` iken tarama hiç çalışmaz (gözlem modu). Kimlik anahtarı kuralların yazdığı `ip:<ip>` ile birebir aynı olmalı (yoksa hibrit birleşmez — 2026-11-13'te bu yüzden düzeltildi).
**İzleme:** `AdminSecurityController` `GET /api/admin/security/risky-clients` (ADMIN) → `RiskScoreStore.snapshot(now)`: riskli/engelli IP'ler, birleşik skor, band, engel bitişi. Hibritin canlı kanıtı burada görünür.
**`path_pattern` kuralı** (`SecurityEventFilter.resolvePathPattern`): Spring eşleşmesi varsa şablon (`/api/postings/{id}`); yoksa sınırlı kaba yol:
`/api/admin/**`, `/api/<bilinen kaynak>/**`, `/api/**` (anonim bilinmeyen), `/**` (kimlikli bilinmeyen). Böylece saldırgan DB'ye keyfi metin yazamaz
ve benzersiz yol sayısı patlamaz (2026-11-12'de düzeltildi).

### ML hattı (`backend/ml/`, Python 3.13, `python -m pip install scikit-learn numpy`)
| Dosya | Görev |
|---|---|
| `features.sql` | **Özellik sözleşmesi v1:** birim (ip, 1 dk); 10 özellik: `req_count`, `distinct_paths`, `err_4xx_ratio`, `c401` (yalnızca ACCESS_DENIED+401), `c403`, `c404`, `login_fail_count`, `distinct_login_ids`, `write_ratio`, `admin_path_hits`. **429 satırları özelliklere girmez** (kendi kararımız, geri besleme olmasın). |
| `generate_normal.py` | Normal profiller. `--v2` modu zor veri üretir: meşru 403 (yetki hataları), 2-3 yazım hatalı giriş, yoğun gezinme (`busy_browser`), kampüs NAT ortak IP'leri. `--clients 450 --seed 42 --v2` → 234.877 olay / 23.469 istemci-dakikası (v2 ana veri) |
| `generate_attacks.py` | 12 saldırı varyasyonu: kaba kuvvet (loud/stealth/stuffing), IDOR (auth/anon/stealth), tarama (auth/anon), admin yoklama (anon/user/stealth), scraper. `--seed 7` → 2.159 dakika |
| `build_features.py` | Olaylardan `features_normal.csv` / `features_attack.csv` (features.sql ile aynı tanım) |
| `train_model.py` | IP bazlı %60/%20/%20 bölme; yalnızca normalle eğitim; eşik doğrulamanın %99'u. **Ana model kNN** → `models/knn_model.joblib`; Isolation Forest karşılaştırma için → `iforest.joblib`. Ham kNN mesafesini [0,1] `ml_score`'a eşler (eşiğe kadar 0, rampayla 1); `model_meta.json`'da `deployed_model: knn`. |
| `compare_layers.py` | **KURAL vs ML vs HİBRİT** kapsama karşılaştırması: her (ip,dk) için kural tekrar-oynatması + ML + birleşim; saldırı varyantı başına recall ve normal yanlış pozitif. Hocanın istediği karşılaştırma. |
| `ml_service.py` | **FastAPI skorlama servisi**: `knn_model.joblib`'i yükler, 10 özelliği alır, `ml_score` döner. Uçlar `/health`, `/score`, `/score/batch`. Çalıştırma: `python -m uvicorn ml_service:app --host 127.0.0.1 --port 8000`. |

**Sonuçlar (v2 zor veri, test = 4.459 normal + 2.159 saldırı dakikası):**
| Model | ROC-AUC | Precision | Recall | F1 | Yanlış pozitif |
|---|---|---|---|---|---|
| Isolation Forest | 0,991 | 0,981 | 0,789 | 0,875 | %0,74 |
| **kNN mesafesi (k=10, ana model)** | 0,999 | 0,979 | 0,969 | 0,974 | %1,03 |

**Kural vs ML vs hibrit (compare_layers, recall):** kural tek başına 0,70 — `idor/anon`, `scan/anon`, `scraper/fast`'i tamamen kaçırıyor (bunlar 401/200 alıyor, `ResourceProbing` ise sadece 403/404 sayıyor). ML 0,97, **hibrit (birleşim) 0,98**. En açıklayıcı satır `idor/stealth`: kural 0,52 + ML 0,87 → hibrit 0,92 (ikisi de tek başına yetmiyor). Normal yanlış pozitif: kural %0,1, ML %1,0, hibrit %1,1.
Bulgular: Isolation Forest hâlâ **`scraper/fast`** (recall 0) ve **`admin_probe/stealth`**'te zayıf; kNN hepsinde güçlü (tek zayıf nokta `idor/stealth` 0,87, o da dakika başına — oturum düzeyinde artar). v2 zor veride bile yanlış pozitif ~%1 → model "yoğun ama meşru" ile saldırganı ayırabiliyor (stres testi). LOF denendi, birebir aynı satırlar yüzünden bozuldu → kNN.

**Hibrit skor (çalışıyor):** `risk = min(100, kuralSkoru + 0,4 × mlSkoru × 100)`; Java özellikleri `features.sql` ile hesaplayıp FastAPI'ye gönderir, servis erişilemezse yalnızca kural skoru (fallback). Canlı entegrasyon bölüm 6'da (`MlRiskSweep` + `MlScoringClient` + admin uç). Java/Python özellik paritesi için "altın test" hâlâ planlı.

---

## 7. Test

Her adım Postman'de doğrulandı. Üç **kendi doğrulamalarını içeren** koleksiyon var (kullanıcı kendi verisini oluşturur, DB sıfırlamadan tekrar çalıştırılabilir):

| Koleksiyon | Kapsam |
|---|---|
| **UniLink Başvuru Testi** (96 istek, 185 doğrulama) | Başvuru yapma, listeler, rozet, kabul/red, kontenjan, geri çekme, yeniden başvuru, iptal/kapanmış ilan, `hasVacancy` |
| **UniLink Kaydedilenler ve İlanlarım Testi** (72 istek, ~127 doğrulama) | Kaydedilenler, `saved` bayrağı, İlanlarım ve başvuru sayıları |
| **UniLink Güvenlik Simülasyonu** (67 istek, **81 doğrulama, 81/81 geçti 2026-11-13**) | Kaba kuvvet, IDOR, admin yoklama, istek seli/yavaşlatma, 429 biçimi (Retry-After, skor sızmıyor) |

- Koleksiyonlar Python betikleriyle üretildi; sahte sunucu emülatörüyle (Node) sınandı. Kod değişince **üçünü de regresyon olarak çalıştır** (hedef: hepsi yeşil).
- **Güvenlik simülasyonunu çalıştırma:** IntelliJ Run Configuration → Environment variables:
  `APP_SECURITY_ENFORCEMENT_ENABLED=true;APP_SECURITY_TRUST_FORWARDED_HEADERS=true;SPRING_DEVTOOLS_RESTART_ENABLED=false`, uygulamayı yeniden başlat, koleksiyonu
  baştan sona çalıştır (Delay 0, Save responses kapalı, Postman masaüstü). **Bitince üç değişkeni sil ve yeniden başlat** (`trust-forwarded-headers` açık kalmamalı).
  Koleksiyondaki sel istekleri (4.1/4.2) 10'arlı gruplarla gönderilir (220 paralel istek bağlantı hatası veriyordu).
- E-posta doğrulama Mailpit API'siyle otomatik; süresi dolan ilan testi için istek öncesi `setTimeout` ile bekleme kullanılır (çalışmazsa SQL ile `deadline` geçmişe çekilir).
- **Henüz yok (test borcu):** otomatik birim/entegrasyon testleri (Testcontainers), CI; kontenjan eşzamanlılık testi; `RiskScoreStore` 15 dk engel kalıcılığı için birim testi; "block eşiği 80" mutasyonu simülasyonda yakalanmıyor; **ML altın parite testi** (Java `features.sql` vs Python `build_features.py` aynı olayda birebir aynı özellik).

### Bilinen sınırlamalar / sonra ele alınacaklar
- `GET /api/technologies?query=` araması Türkçe harf katlaması yapmaz (ilan aramaları yapar).
- `pg_trgm` index'i kullanılmıyor; ilan sayısı artarsa index'li aramaya geçilir.
- Mail gönderimi DB transaction'ının içinde; ileride commit sonrasına (`@TransactionalEventListener`).
- Aynı yeni teknolojinin aynı anda iki kullanıcıca eklenmesi: DB `UNIQUE` yakalar, 409; tekrar denenir.
- Teknoloji tablosu kullanıcı girdisiyle büyür; kirlenirse admin panelinde silme/birleştirme ya da `verified` işareti.
- 404 (bilinmeyen adres), 405 ve güvenlik (401/403) yanıtları Spring'in varsayılan biçiminde; istenirse `ApiError`'a çevrilir.
- Swagger herkese açık; canlıda kapatılacak (`springdoc.swagger-ui.enabled=false`, `springdoc.api-docs.enabled=false`). Canlıda `JWT_SECRET` ortam değişkeni ve gerçek `app.cors.allowed-origins`.
- Üniversite ve bölüm serbest metin; filtreler "içerir" mantığıyla çalışır (ileride seçilebilir liste).
- `FRONTEND_REHBERI.md` 2026-11-13'te güncellendi (saved bayrağı, İlanlarım, şifre değiştirme, 429/Retry-After; "yakında" notları kalktı). Rehberdeki şifre kuralı 8–72; `RegisterRequest`'te `@Size(max = 72)` olduğundan emin ol.

---

## 8. Sıradaki adımlar (öneri sırası)

**2026-11-13'te tamamlananlar:** v2 zor veri + kNN ana model; kural vs ML vs hibrit karşılaştırması (`compare_layers.py`); FastAPI servisi; Java `MlScoringClient` + `MlRiskSweep` + hibrit skor + fallback; admin izleme ucu (`/api/admin/security/risky-clients`) — hibrit canlı doğrulandı (kural 40 + ML 40 = 80 CRITICAL, engel).

1. **ML altın parite testi:** Java'nın `features.sql`'i ile Python `build_features.py` aynı olay kümesinde birebir aynı 10 özelliği üretiyor mu? (Örn. aynı DB'den çekilen olaylar → iki tarafın çıktısını karşılaştıran bir test.) Rapor için sağlamlık kanıtı.
2. **İzleme + seed:** düzgün **admin hesabı seed'i** (Flyway migration ya da config'ten okuyan seeder; şu an elle SQL ile `role='ADMIN'`); admin ucunu zenginleştir (olay özeti, engel listesi, zaman aralığı).
3. **Kalite ve yayın:** Testcontainers (kontenjan eşzamanlılığı, RiskScoreStore blok kalıcılığı, ML parite), CI (GitHub Actions), deploy, **engelleme modunu açma kararı** (yanlış pozitif ölçümüyle; ML ve enforcement şu an `false`).
4. **Dokümanlar:** Çözüm Tasarımı ve Teknik Analiz dokümanı; final raporu ve demo (demoda compare_layers tablosu + admin uç ekranı güçlü).
5. Hocaya sorulacaklar: ML canlı mı toplu mu (biz dakikalık toplu yaptık), içerik tabanlı saldırılar kapsamda mı, beklenen değerlendirme ölçütleri, 20 Aralık teslim mi sunum mu.
6. Her yeni uç eklenince: Swagger otomatik güncellenir; `FRONTEND_REHBERI.md` ve README güncel tutulur.

---

## 9. Çalışma biçimi

- **Adım adım ilerle.** Her mesajda küçük bir parça ver (tek seferde çok dosya, zip ya da büyük dosya verme). Bir adım bitip kullanıcı "temiz" dedikten sonra sonrakine geç. Kullanıcı testleri sık sık **sonraya erteler**; bir **test borcu listesi** tut, koleksiyonlarla toplu kapat.
- Dili **Türkçe** tut. Her adımda ne yapıldığını ve **neden** yapıldığını kısaca açıkla; kullanıcı kavramları sorabiliyor (örn. `@MapsId`, `@AuthenticationPrincipal`), o zaman sade bir dille ve örnekle anlat. Kullanıcı tasarım önerileri getirebiliyor; iyi fikirse planı değiştir, sakıncalarını da söyle.
- IntelliJ arayüzünden anlat (sağ tık → New → Class/Interface/Enum/Record), terminal gerekirse IntelliJ'in Terminal sekmesi. Import'lar için `Alt+Enter`, temizlik için `Ctrl+Alt+O`.
- Her adımın sonunda "Kontrol" bölümü ver. Endpoint testlerini mümkünse **Postman koleksiyonu olarak üret** (kendi doğrulamaları, değişken zinciri, Mailpit ile otomatik doğrulama) ve önce sahte sunucu emülatörüyle sına. Test beklentileri koddaki kurallarla çelişmemeli; `%` → `%25` gibi URL kodlamalarını belirt.
- **Bir metodu "şununla değiştir" derken üstündeki annotation'ları (`@Transactional` vb.) da dahil et.**
- Kullanıcı bir sınıfı toptan yeniden yazmamı isterse sınıfın tamamını ver.
- Kodu derleyemeyen bir ortamdaysan bunu açıkça söyle. Kütüphane/artifact adlarından ve sürümlerinden emin değilsen doğrula (Boot 4'te çok şey yeniden adlandırıldı).
- Dokümanlar: `README.md` (kurulum), `FRONTEND_REHBERI.md` (arkadaş için kapsamlı rehber), `agent.md` (bu dosya), ara rapor (`UniLink_Ara_Raporu_Duzeltilmis.docx`). Büyük bir kilometre taşında `agent.md`'yi güncelle.
- Kullanıcı Windows/PowerShell kullanıyor; `pip` PATH'te yok, `python -m pip` kullan. Gerçek veride denemeden varsayım yapma (path_pattern, özellik tanımı gibi sorunlar böyle bulundu).

---

## 10. Hızlı komutlar / adresler

- Uygulama: `http://localhost:8080`, Swagger: `http://localhost:8080/swagger-ui.html`
- Mailpit: `http://localhost:8025`
- Postgres: `localhost:5433`, db/kullanıcı/şifre `unilink`
- Geliştirme: compose ▶ (`db` + `mailpit`), uygulama IntelliJ'den. Tam ortam: `docker compose --profile full up --build` (IntelliJ'deki uygulamayı önce durdur)
- Başarılı açılış göstergeleri: `Successfully validated 3 migrations`, `Schema "public" is up to date`, `Found 10 JPA repository interfaces`, `Started BackendApplication`
- ML eğitim (Windows, `backend\ml`): `python generate_normal.py --clients 450 --seed 42 --v2 --out data/normal_events.csv`, `python generate_attacks.py --seed 7 --out data/attack_events.csv`,
  `python build_features.py`, `python train_model.py`, `python compare_layers.py` (pip yoksa `python -m pip ...`)
- ML servisi (`backend\ml`): `python -m uvicorn ml_service:app --host 127.0.0.1 --port 8000` — `uvicorn` PATH'te yok, `python -m uvicorn` kullan. Sağlık: `http://127.0.0.1:8000/health`, Swagger: `/docs`
- Admin izleme: `GET /api/admin/security/risky-clients` (ADMIN token gerekir; test için `UPDATE users SET role='ADMIN', email_verified=true WHERE email='...'`)
- Postman'de korumalı uç: **Authorization → Bearer Token**; `POST /api/auth/login` ile alınır, 15 dk geçerli