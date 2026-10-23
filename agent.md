# UniLink – Proje Arkadaşı Bulma Platformu (agent.md)

Bu dosya, projede çalışan bir geliştirici ya da yapay zekâ asistanı için bağlam özetidir:
projenin amacı, alınan kararlar, şu ana kadar yapılanlar ve sıradaki adımlar.

**Durum özeti (2026-10-17):** Veri katmanı, **auth modülü**, **profil/teknoloji modülü**, hata yönetimi, CORS, Swagger,
Docker ile çalıştırma ve README tamamlandı. **İlan modülü büyük ölçüde hazır:** oluşturma, detay, akış
(sayfalama + sıralama) ve tür/arama/teknoloji/rol/bölüm/üniversite filtreleri çalışıyor. **Sırada ilan düzenleme/iptal,
süresi dolan ilanları kapatan job ve başvuru modülü var.**

---

## 1. Proje özeti

Bitirme projesi. Sadece üniversite e-postasıyla (`.edu.tr`) hesap açılabilen, öğrencilerin ilan açarak
proje arkadaşı / ekip üyesi bulabildiği bir web sitesi.

- **Backend'i kullanıcı yazıyor; frontend'i bir arkadaşı yazacak.** Bu yüzden API'nin frontend'in bir adım önünde gitmesi,
  Swagger/README/Docker ile hazır tutulması öncelik.
- **Süre:** yaklaşık 3-4 ay. Kabaca: 1. ay backend çekirdeği, 2. ay frontend ile paralel entegrasyon, 3. ay mesajlaşma ve
  ek özellikler, 4. ay test/deploy/rapor/sunum + boşluk payı.
- **gRPC kullanılmayacak** (tek servis, tarayıcı istemcisi; gerek yok). **Redis** ancak gerçek bir ihtiyaç için eklenir
  (aday: giriş/başvuru rate limiting, ilan akışı önbelleği, mesajlaşma ölçekleme).

### Verilen kararlar
- **İlan türleri:** `PROJECT` (okul/kişisel proje), `COMPETITION` (yarışma/hackathon), `STARTUP` (girişim). Tek `postings` tablosu, ilan türü seçilebilir.
- **Kayıt:** Tüm `.edu.tr` uzantılı e-postalar kabul edilir (tek üniversiteyle sınırlı değil). E-posta doğrulaması zorunlu.
- **"Şirket" alanı:** `organization_name` – yarışma/organizasyon adı, **opsiyonel**.
- **Bildirim:** Başvuru gelince ilan sahibine e-posta gitmez, site içinden görür (bekleyen başvuru rozeti = `COUNT(status = PENDING)`). Spring Mail sadece doğrulama ve şifre sıfırlama için.
- **İlan süresi:** Süresi dolan ilan otomatik `CLOSED` olur. Akıştan (anasayfa) düşer ama başvuranlar "Başvurulan" sayfasında görmeye devam eder, üzerinde "Başvuru kapandı" yazar. İlan detayı kapalı ilanda da açılır (`open: false`).
- **Görünürlük:** İlanlar sadece giriş yapmış öğrencilere listelenir. Herkese açık liste yok. `CANCELLED` ilanı sadece sahibi görür (başkası 404 alır).
- **Teknolojiler (LinkedIn mantığı):** Kullanıcı mevcut listeden seçebilir; listede yoksa yazdığı teknoloji de eklenir (isme göre bul-ya-da-oluştur, büyük/küçük harf duyarsız). Profil ve ilanlar aynı `technologies` tablosunu kullanır.

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
| Güvenlik | Spring Security, BCrypt, stateless JWT (HS256); **Spring'in kendi resource-server desteği (Nimbus)**, `jjwt` ve `UserDetailsService` kullanılmıyor |
| API dokümantasyonu | springdoc-openapi 3.0.3 (`springdoc-openapi-starter-webmvc-ui`), Swagger UI |
| Mail (geliştirme) | Mailpit (SMTP `localhost:1025`, arayüz `http://localhost:8025`) |
| Diğer | Lombok, Bean Validation, DevTools |
| IDE / test aracı | IntelliJ IDEA 2025.2 (Windows), Postman |

- Proje klasörü: `Desktop/UniLink/backend`
- Ana paket: `com.hakkitaygun.unilink`, ana sınıf: `BackendApplication`
- Konfigürasyon: **`application.properties`** (yml kullanılmıyor)
- Önemli bağımlılık adı: `spring-boot-starter-security-oauth2-resource-server` (Boot 4'te starter adı değişti, eski ad çalışmaz)

### Docker
- `compose.yaml`: `db` (postgres:16, db/kullanıcı/şifre `unilink`, `5433:5432`, healthcheck), `mailpit`, ve **`profiles: ["full"]`** arkasında `app` (Dockerfile'dan derlenir, `SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/unilink`, `SPRING_MAIL_HOST=mailpit`).
- IntelliJ'den geliştirirken compose'taki ▶ sadece `db` + `mailpit`'i başlatır. Tam ortam (arkadaş için):
  `docker compose --profile full up --build`; durdurma: `docker compose --profile full down` (veri kalır, `-v` siler).
- `Dockerfile`: iki aşamalı (`eclipse-temurin:25-jdk` ile derle, `25-jre` ile çalıştır), root olmayan kullanıcı. `.dockerignore` var.
- `app` konteyneri imajı **derlendiği andaki koda** sahiptir; kod değişince `--build` gerekir. Portlar çakışmasın (8080, 8025, 1025, 5433).
- `README.md` arkadaşa yönelik: çalıştırma, token akışı, e-posta linkleri, hata biçimi, hazır uçlar.

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
```
(`app.verification-url` kaldırıldı.)

### Paket yapısı (katman bazlı)
Her modül kendi altında `controller`, `service`, `repository`, `entity`, `enums`, `dto`, `exception` klasörlerini kullanır
(`posting` ayrıca `specification`). Yeni kod bu yapıya uymalı. (`SavedPostingId` şu an `posting` kökünde; `posting.entity`'ye taşınması önerildi.)

```
com.hakkitaygun.unilink
├── common      (entity: BaseEntity, AuditableEntity | dto: PageResponse | exception: ApiException, ApiError, GlobalExceptionHandler)
├── config      (SecurityConfig, JwtConfig, CorsConfig, OpenApiConfig)
├── auth
├── user
├── technology
├── posting
└── application
```

---

## 3. Veritabanı şeması (Flyway)

- `V1__init.sql`: 12 tablo (aşağıda)
- `V2__seed_technologies.sql`: ~45 başlangıç teknolojisi (`ON CONFLICT DO NOTHING`)
- **Kural:** Uygulanmış bir migration dosyası asla düzenlenmez (Flyway checksum hatası verir). Yeni değişiklik için `V3__...`, `V4__...` yazılır.

Tablolar:
- `users` – e-posta (küçük harf + `.edu.tr` CHECK, admin muaf), password_hash, role (USER/ADMIN), email_verified, banned, deleted_at
- `profiles` – PK = FK `user_id` (users ile 1-1): full_name, university, department, bio, github_url, linkedin_url
- `technologies` – ortak teknoloji listesi (isim `lower(name)` ile unique)
- `profile_technologies`, `posting_technologies` – çoka-çok ara tablolar
- `verification_tokens` – EMAIL_VERIFICATION / PASSWORD_RESET (token'ın **hash'i** saklanır)
- `refresh_tokens` – JWT refresh, `revoked_at` ile iptal edilebilir (hash saklanır)
- `postings` – owner, type, title, description, organization_name, stage (sadece STARTUP), benefits_note, status (OPEN/CLOSED/CANCELLED), deadline
- `posting_roles` – rol bazlı kontenjan (`required_count` 1–50, `(posting_id, role)` unique)
- `applications` – posting, applicant, status (PENDING/ACCEPTED/REJECTED/WITHDRAWN), message, `accepted_role_id`, decided_at; `(posting_id, applicant_id)` unique
- `application_roles` – başvuranın seçtiği roller
- `saved_postings` – PK `(user_id, posting_id)`

DB seviyesindeki önemli kurallar:
- `ACCEPTED` olan başvurunun `accepted_role_id` değeri dolu, diğer durumlarda boş olmak zorunda.
- Kabul edilen rol, başvuranın seçtiği roller (`application_roles`) arasından olmak zorunda (composite FK).
- `stage` sadece `STARTUP` ilanlarında dolu olabilir.
- `updated_at` kolonları DB trigger'ı ile de güncellenir.
- Arama için `pg_trgm` GIN index'leri (title, organization_name) var ama şu an `lower(...)` ile arandığı için kullanılmıyor; akış için kısmi index'ler (`status = 'OPEN'`).

---

## 4. Şu ana kadar yapılanlar

Her adım küçük parça halinde yazıldı, uygulama çalıştırılıp (konsol ve Postman) doğrulandı.

**Altyapı**
- [x] IntelliJ'de proje, Docker (Postgres + Mailpit), Flyway V1 ve V2
- [x] CORS (`CorsConfig`, `SecurityConfig`'te `.cors(...)`; preflight 401'e düşmez)
- [x] Swagger/OpenAPI (`OpenApiConfig`, Bearer JWT şeması; `/swagger-ui.html`, `/v3/api-docs`). **Canlıda kapatılacak** (`springdoc.swagger-ui.enabled=false`, `springdoc.api-docs.enabled=false`)
- [x] Dockerfile + compose `full` profili, README.md

**Veri katmanı (9 repository, Hibernate `validate` ile doğrulandı)**
- [x] `common`: `BaseEntity` (UUID id, equals/hashCode), `AuditableEntity` (created_at, updated_at)
- [x] `technology`: `Technology`, `TechnologyRepository`
- [x] `user`: `Role`, `User` (`@Builder`, `normalizeEmail`, `isActive`), `Profile` (`@MapsId`, teknolojiler), repository'ler
- [x] `auth`: `TokenType`, `VerificationToken`, `RefreshToken`, repository'ler
- [x] `posting`: `PostingType`, `PostingStatus`, `PostingStage`, `Posting` (`technologies`, `roles` cascade + orphanRemoval, `addRole/removeRole`, `isOpen(now)`), `PostingRole`, `SavedPostingId` + `SavedPosting`, repository'ler
- [x] `application`: `ApplicationStatus`, `Application` (`appliedRoles`, `acceptedRole`, `accept/reject/withdraw`), `ApplicationRepository`

**Altyapı sınıfları**
- [x] `SecurityConfig`: BCrypt, CSRF kapalı, stateless, `/api/auth/**`, `/error` ve Swagger adresleri açık, diğerleri JWT ister; `role` claim'i `ROLE_` önekiyle yetkiye çevrilir
- [x] `JwtConfig`: `SecretKey`, `JwtEncoder`, `JwtDecoder` (HS256)
- [x] `common.exception`: `ApiException`, `ApiError`, `GlobalExceptionHandler` (ApiException, `@Valid` alan alan, bozuk JSON, parametre türü/eksik parametre → 400; `DataIntegrityViolationException` → 409; beklenmeyen hata → genel 500, detay log'a; Spring'in kendi 404/405 ve güvenlik hataları Spring'e bırakılır)

**Auth servisleri (`auth.service`)**
- [x] `AuthService`: register, verifyEmail, login, refresh, logout, resendVerification, forgotPassword, resetPassword
- [x] `JwtService`, `TokenGenerator`, `RefreshTokenService` (create/consume/revoke/revokeAll), `EmailService` (doğrulama linki: `<frontend>/verify-email?token=`, sıfırlama linki: `<frontend>/reset-password?token=`)

**Profil ve teknoloji**
- [x] `ProfileService`: `getMyProfile`, `updateMyProfile`, `updateMyTechnologies`
- [x] `TechnologyService`: `getAll`, `search`, `resolveOrCreate(isimler)`

**İlan modülü (`posting`)**
- [x] DTO'lar: `CreatePostingRequest` (compact constructor ile temizleme), `PostingRoleRequest`, `PostingResponse` (detay), `PostingSummaryResponse` (kart; açıklama ilk 200 karakter + `…`, `totalRemaining`), `PostingOwnerResponse`, `PostingRoleResponse` (`acceptedCount`, `remainingCount`), `PostingFilter`; `common.dto.PageResponse` (sayfa 0'dan başlar)
- [x] `PostingService`: `create` (sahip token'dan; `validate`: stage sadece STARTUP, son tarih en fazla 1 yıl sonra, aynı rol iki kez eklenemez), `getById` (CANCELLED sadece sahibine), `getFeed`
- [x] `PostingSpecifications` (`posting.specification`): `isOpen`, `hasType`, `matchesQuery`, `hasAnyTechnology`, `hasRole`, `ownerDepartmentContains`, `ownerUniversityContains`; `Specification.unrestricted()` ile başlar, alt sorgu (`subquery`) kullanır (tekrar eden satır/sayfalama bozulması olmaz). LIKE kaçışı (`%`, `_`), 100 karakter sınırı
- [x] `PostingSort` (`NEWEST`, `DEADLINE`; sıralamaya her zaman `id` eklenir)
- [x] `PostingController`: `POST /api/postings`, `GET /api/postings/{id}`, `GET /api/postings`
- [x] `InvalidPostingException` (400), `PostingNotFoundException` (404)

**Endpoint'ler** (`/api/auth/**` açık, diğerleri `Authorization: Bearer <accessToken>` ister)

| Endpoint | Başarı | Hatalar |
|---|---|---|
| `POST /api/auth/register` | 201 | 400 (`.edu.tr` değil / doğrulama), 409 (e-posta kayıtlı) |
| `POST /api/auth/verify-email` (`{token}`) | 204 | 400 (geçersiz/kullanılmış/süresi dolmuş). **GET değil POST** (mail tarayıcıları token tüketmesin) |
| `POST /api/auth/login` | 200 (`accessToken`, `refreshToken`, `tokenType`, `expiresInSeconds`) | 401, 403 (doğrulanmamış / devre dışı) |
| `POST /api/auth/refresh` | 200 (yeni token çifti) | 401, 403 |
| `POST /api/auth/logout` | 204 (her zaman) | 400 |
| `POST /api/auth/resend-verification` | 202 (her zaman) | 400 |
| `POST /api/auth/forgot-password` | 202 (her zaman) | 400 |
| `POST /api/auth/reset-password` | 204 | 400 |
| `GET /api/users/me` | 200 (`ProfileResponse`) | 401, 404 |
| `PUT /api/users/me` | 200 | 400, 401 |
| `PUT /api/users/me/technologies` (`{technologies:[isimler]}`) | 200 | 400, 401 |
| `GET /api/technologies?query=` | 200 (boşsa hepsi, doluysa ilk 10) | 401 |
| `POST /api/postings` | 201 (`PostingResponse`) | 400, 401 |
| `GET /api/postings/{id}` | 200 (`PostingResponse`) | 400 (geçersiz id), 401, 404 |
| `GET /api/postings` | 200 (`PageResponse<PostingSummaryResponse>`) | 400 (geçersiz sort/type), 401 |

`GET /api/postings` parametreleri: `page` (0'dan, negatif → 0), `size` (varsayılan 12, 1–50), `sort` (`NEWEST` | `DEADLINE`),
`type`, `query` (başlık/organizasyon), `technologies` (tekrarlı parametre, "herhangi biri", en fazla 10), `role`, `department`, `university`
(rol/bölüm/üniversite "içerir" mantığı, harf duyarsız). Sadece `status = OPEN` ve `deadline > şimdi` ilanlar gelir.

### Auth ve profil kararları ve davranışları
- **Token'lar DB'de hash:** doğrulama, sıfırlama ve refresh token'ları SHA-256 hash olarak saklanır, ham hâli sadece kullanıcıya gider.
- **Süreler:** access 15 dk, refresh 7 gün, doğrulama linki 24 saat, şifre sıfırlama linki 1 saat.
- **Refresh rotasyonu:** refresh token tek kullanımlık; kullanılınca iptal edilir, yeni token çifti verilir. Logout refresh token'ı iptal eder (token yoksa da 204). Şifre sıfırlanınca tüm refresh token'lar iptal edilir.
- **E-posta ifşası yok:** `resend-verification` ve `forgot-password` her zaman 202. Giriş hatası tek mesaj; 403 durumları sadece şifre doğruysa görünür.
- **Mail bombası önlemi:** aynı kullanıcı için son token 1 dakikadan yeniyse yeni mail gitmez; yeni token üretilince eskileri silinir.
- **Şifre:** 8–72 karakter (BCrypt 72 bayt sınırı).
- **Kimlik token'dan:** Kullanıcı id'si `@AuthenticationPrincipal Jwt jwt` → `jwt.getSubject()`; istekten (path/body) alınmaz. İlan sahibi, başvuran, kaydeden hep böyle belirlenir.
- **Profil güncelleme `PUT` semantiğinde;** GitHub/LinkedIn linkleri `@Pattern` ile sınırlı (DB CHECK ile aynı).
- **Teknoloji isimleri:** 2–50 karakter, harf/rakam ve `. + # / & -`; baş/son boşluk ve ardışık boşluklar temizlenir; listedeki yazım korunur; profil veya ilan başına en fazla 20.
- **Rol token'ın içinde:** rol/ban değişimi access token süresi dolana kadar (en fazla 15 dk) yansımaz. Ban refresh sırasında yakalanır.

---

## 5. Kod kuralları ve dikkat edilecekler

- Entity'lerde `@Data` yok; `@Getter/@Setter/@NoArgsConstructor` (User ve Profile'da ayrıca `@Builder`). İlişkiler `LAZY`. Koleksiyonlar `Set`. Enum'lar `@Enumerated(EnumType.STRING)`.
- **E-posta normalizasyonu:** `User.normalizeEmail(String)` (`trim().toLowerCase(Locale.ROOT)`). `findByEmail`/`existsByEmail` öncesi uygulanır. `@Builder` `setEmail`'i atlar.
- **Entity API'den döndürülmez;** DTO (Java `record`, `from(entity)` statik metodu). Lazy ilişkileri okuyan dönüşümler **`@Transactional` servis metodunun içinde** yapılır, yoksa `LazyInitializationException`. Listelerde N+1: `@EntityGraph` (tekil sorgu) veya `default_batch_fetch_size` (sayfalı liste; koleksiyonu sayfalamayla `JOIN FETCH` etme).
- Yeni iş kuralı hataları `ApiException`'dan türetilir; `@ResponseStatus` kullanılmaz.
- `@Transactional` = Spring'inki (`org.springframework.transaction.annotation`); sadece okuyanlarda `readOnly = true`.
- Doğrulama kuralları request record'unda; temizleme (trim vb.) compact constructor'da, doğrulamadan önce.
- Servis sorumlulukları ayrı; başka modülün mantığı o modülün servisinden çağrılır (örn. `TechnologyService.resolveOrCreate`).
- **Dinamik filtre = `Specification`** (`PostingSpecifications`); filtre yoksa `Specification.unrestricted()`. Listeye bağlı filtreler alt sorguyla (join ile satır çoğalır). Sıralama parametresi enum beyaz listesi; sayfa boyutu sınırlı (1–50).
- Uygulanmış Flyway migration'ı düzenlenmez, yenisi eklenir.
- Kimlik/sahiplik kontrolü serviste yapılır (`owner.id == jwt.sub`); sahibi olmayana yetki hatası yerine 404 vermek (varlığı sızdırmamak) tercih edilir.

### Serviste yapılması gerekenler (DB garanti etmiyor)
- Kendi ilanına başvuruyu engelle.
- Başvurulan rollerin gerçekten aynı ilana ait olduğunu doğrula (`posting_roles.posting_id`).
- Kabul işlemini transaction içinde ve rol satırını kilitleyerek yap (`PESSIMISTIC_WRITE`), kontenjan aşılmasın. Kontenjan: `required_count` ile `accepted_role_id = :role AND status = ACCEPTED` sayısı.
- `accept(role)` çağrılmadan önce rolün `appliedRoles` içinde olduğunu kontrol edip anlaşılır hata mesajı dön.
- İlanı sadece sahibi düzenleyebilir/iptal edebilir.
- Başvuru için rate limit, ilan şikayet mekanizması (spam önlemi) düşünülmeli.

### Bilinen sınırlamalar / sonra ele alınacaklar
- **Kontenjan sayıları geçici:** `PostingResponse.from(...)` ve `PostingSummaryResponse.from(...)` çağrılarında kabul edilen sayı haritası şimdilik `Map.of()` (hepsi `acceptedCount: 0`). Başvuru modülü gelince gerçek sayılar bağlanacak. "Kontenjan açık mı" filtresi de o zaman eklenecek.
- **Türkçe `İ`/`ı` arama sorunu:** Küçük harfe çevirme Türkçe kuralını bilmiyor; `department=MAKİNE` gibi büyük `İ` ile yazılan aramalar boş döner (`MAKINE` yazılırsa çalışır). Planlanan çözüm: hem Java hem veritabanı tarafında aynı "katlama" (`İ I ı → i`, `Ş ş → s`, `Ğ ğ → g`, `Ü ü → u`, `Ö ö → o`, `Ç ç → c`, sonra küçük harf; DB'de `translate` ile) uygulamak. Aynı mantık başlık, organizasyon, rol, bölüm ve üniversite aramalarına uygulanmalı.
- `pg_trgm` index'i şu an kullanılmıyor (`lower(...)` ile aranıyor); ilan sayısı artarsa index'li aramaya geçilir.
- Mail gönderimi DB transaction'ının içinde; ileride commit sonrasına (`@TransactionalEventListener`) taşınmalı.
- Aynı anda iki kayıt isteği / aynı yeni teknolojinin aynı anda eklenmesi: DB `UNIQUE` kısıtı yakalar, 409 döner.
- Teknoloji tablosu kullanıcı girdisiyle büyüyor; kirlenirse admin panelinde silme/birleştirme ya da `verified` işareti (V3 migration).
- Bilinmeyen adres (404), yanlış metot (405) ve güvenlik (401/403) yanıtları Spring'in varsayılan biçiminde; istenirse `ApiError`'a çevrilir.
- Süresi dolan token'ları temizleyen zamanlanmış iş yok (`deleteExpired` sorguları hazır). Süresi dolan **ilanları** `CLOSED` yapan job da henüz yok (akış sorgusu `deadline > now()` ile zaten gizliyor).
- Swagger şu an herkese açık; canlıda kapatılacak. Canlıya çıkarken `JWT_SECRET` ortam değişkeni rastgele, uzun bir değerle tanımlanmalı. `app.cors.allowed-origins` gerçek alan adıyla güncellenmeli.

---

## 6. Sıradaki adımlar

**Şimdi: ilan modülünü bitir**
1. (İsteğe bağlı, küçük) Türkçe `İ/ı` arama düzeltmesi
2. İlan düzenleme ve iptal: `PUT /api/postings/{id}`, `DELETE /api/postings/{id}` (`CANCELLED`), sadece sahibi; kuralların bir kısmı oluşturma ile aynı (`stage` sadece STARTUP, son tarih vb.)
3. Süresi dolan ilanları `CLOSED` yapan `@Scheduled` job (+ süresi dolan token'ları silen temizlik)
4. Kullanıcının kendi ilanları: `GET /api/users/me/postings` (kapalı ve iptal dahil)

**Sonra**
- Başvuru modülü: başvur, geri çek, kabul/red (kilitli kontenjan kontrolü), "Başvurulan" ve ilan sahibi başvuru listeleri, bekleyen sayısı; ilan yanıtlarındaki kontenjan haritasını gerçek veriye bağla; "kontenjan açık mı" filtresi
- Kaydedilenler endpoint'leri
- Admin hesabı seed'i, admin paneli (ban, ilan silme, teknoloji yönetimi)
- Testler (Testcontainers), CI (GitHub Actions), canlıya deploy
- Mesajlaşma (WebSocket/STOMP), site içi bildirimler
- Ek özellik adayları (zamana göre seç): rate limiting (Redis), ilan akışı önbelleği, profil-ilan teknoloji eşleştirmesiyle "sana uygun ilanlar", ilan şikayet tablosu, profil için ek alanlar (`V3`, `V4`)
- Frontend arkadaşla senkron: yeni uçlar Swagger'da görünür; README'nin "Hazır olanlar / Yakında" bölümü güncel tutulur

---

## 7. Çalışma biçimi

- **Adım adım ilerle.** Her mesajda küçük bir parça ver (tek seferde çok dosya, zip ya da büyük dosya verme). Bir adım bitip kullanıcı "temiz" dedikten sonra sonrakine geç.
- Dili **Türkçe** tut. Her adımda ne yapıldığını ve **neden** yapıldığını kısaca açıkla; kullanıcı kavramları sorabiliyor (örn. `@MapsId`, `@AuthenticationPrincipal`, `UserController` neden yazıldı), o zaman sade bir dille ve örnekle anlat. Kullanıcı tasarım önerileri getirebiliyor; iyi fikirse planı değiştir, sakıncalarını da söyle.
- IntelliJ arayüzünden anlat (sağ tık → New → Class/Interface/Enum/Record vb.), terminal gerekirse IntelliJ'in Terminal sekmesi. Import'lar için `Alt+Enter`, temizlik için `Ctrl+Alt+O` hatırlatılır.
- Her adımın sonunda "Kontrol" bölümü ver. Endpoint testleri için Postman'de adım adım örnek senaryo ver (istek, body, beklenen durum kodu); kullanıcı örnek verileri nereye yazacağını sorabiliyor, beklenen sonuçları ayrı belirt. **Test beklentileri koddaki kurallarla çelişmemeli**; test adresleri URL kodlaması gerektiriyorsa (`%` → `%25`) belirt.
- **Bir metodu "şununla değiştir" derken üstündeki annotation'ları (`@Transactional` vb.) da dahil et;** eksik kalırsa sessizce kırılır (örn. `getFeed`'te `LazyInitializationException`).
- Kullanıcı bir sınıfı toptan yeniden yazmamı isterse (örn. "AuthService'i güncelle") sınıfın tamamını ver.
- Kodu derleyemeyen bir ortamdaysan bunu açıkça söyle; doğrulamayı kullanıcının makinesine bırak. Kütüphane/artifact adlarından ve sürümlerinden emin değilsen doğrula (Boot 4'te çok şey yeniden adlandırıldı).

---

## 8. Hızlı komutlar / adresler

- Uygulama: `http://localhost:8080`, Swagger: `http://localhost:8080/swagger-ui.html`
- Mailpit arayüzü: `http://localhost:8025`
- Postgres: `localhost:5433`, db/kullanıcı/şifre `unilink`
- Geliştirme: compose'taki ▶ (`db` + `mailpit`), uygulama IntelliJ'den. Tam ortam: `docker compose --profile full up --build` (IntelliJ'deki uygulamayı önce durdur, 8080 çakışır)
- Başarılı açılış göstergeleri: `Schema "public" is up to date`, `Found 9 JPA repository interfaces`, `Started BackendApplication`
- Postman'de korumalı uç: **Authorization → Bearer Token**; `POST /api/auth/login` ile alınır, 15 dk geçerli
- Test verisi: farklı bölüm/üniversiteli ikinci kullanıcı ve birkaç ilan (PROJECT/STARTUP/COMPETITION) filtre testleri için kullanıldı