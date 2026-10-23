# UniLink Backend

Üniversite öğrencilerinin proje, yarışma ve girişim ilanlarıyla ekip arkadaşı bulduğu platformun API'si.
Java 25, Spring Boot 4, PostgreSQL.

## Çalıştırma

Gereken tek şey **Docker Desktop**.

```
docker compose --profile full up --build
```

İlk çalıştırma birkaç dakika sürer. Sonra:

| Ne | Adres |
|---|---|
| API | http://localhost:8080 |
| API dokümantasyonu (Swagger) | http://localhost:8080/swagger-ui.html |
| Giden e-postalar (Mailpit) | http://localhost:8025 |

- Durdurmak: `Ctrl+C`, sonra `docker compose --profile full down` (veriler kalır; silmek için sonuna `-v`).
- Backend kodu değişince `--build` ile yeniden çalıştır.
- Port çakışması varsa 8080, 8025, 1025, 5433 portlarının boş olduğundan emin ol.

## CORS

Frontend şu adreslerden istek atabilir: `http://localhost:3000`, `http://localhost:5173`.
Farklı bir port kullanıyorsan haber ver, `app.cors.allowed-origins` ayarına eklenir.

## Kimlik doğrulama

`/api/auth/**` dışındaki **tüm uçlar** token ister:

```
Authorization: Bearer <accessToken>
```

1. `POST /api/auth/login` → `accessToken` (15 dk) ve `refreshToken` (7 gün) döner.
2. Bir istek **401** dönerse `POST /api/auth/refresh` ile `{ "refreshToken": "..." }` gönder, yeni token çifti gelir.
   Refresh token **tek kullanımlıktır**: her yenilemede gelen yenisini sakla, eskisi geçersiz olur.
3. Yenileme de 401 dönerse kullanıcıyı giriş sayfasına yönlendir.
4. Çıkışta `POST /api/auth/logout` ile `{ "refreshToken": "..." }` gönder ve token'ları sil.

## E-postadaki linkler (frontend sayfaları gerekli)

| Sayfa | Ne yapar |
|---|---|
| `/verify-email?token=...` | Açılınca token'ı `POST /api/auth/verify-email` ile `{ "token": "..." }` olarak gönderir; 204 = doğrulandı, 400 = geçersiz/süresi dolmuş |
| `/reset-password?token=...` | Yeni şifreyi alır, `POST /api/auth/reset-password` ile `{ "token": "...", "newPassword": "..." }` gönderir |

Akışlar:
- **Kayıt:** `POST /api/auth/register` (201) → mail gelir → kullanıcı linke tıklar → doğrulama → giriş yapabilir. Doğrulanmamış hesap girişte 403 alır; `POST /api/auth/resend-verification` ile `{ "email": "..." }` yeniden mail istenir.
- **Şifremi unuttum:** `POST /api/auth/forgot-password` ile `{ "email": "..." }`. Hesap olsun olmasın her zaman 202 döner.
- Sadece `.edu.tr` e-postalar kayıt olabilir. Şifre 8-72 karakter.

## Hata biçimi

Tüm hatalar aynı JSON ile gelir:

```json
{
  "timestamp": "2026-10-17T11:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Gönderilen bilgilerde hatalar var.",
  "path": "/api/users/me",
  "fieldErrors": { "githubUrl": "GitHub bağlantısı https://github.com/ ile başlamalıdır." }
}
```

`message` kullanıcıya gösterilebilir, Türkçedir. `fieldErrors` sadece form doğrulama hatalarında bulunur
(anahtar = alan adı, liste elemanları için `roles[0].requiredCount` gibi).

## Genel kurallar

- Tarihler ISO-8601 UTC (`2026-12-31T20:59:59Z`).
- Sayfa yüklenince kullanıcının profili: `GET /api/users/me`.
- Teknoloji önerileri: `GET /api/technologies?query=spr` (boşsa hepsi). Profile/ilana teknoloji **isimle** gönderilir,
  listede yoksa yenisi eklenir (`PUT /api/users/me/technologies` ile `{ "technologies": ["Java", "Qdrant"] }`).

## Hazır olanlar

Kayıt, doğrulama, giriş, refresh, çıkış, şifre sıfırlama, profil (okuma/güncelleme), teknolojiler,
ilan oluşturma (`POST /api/postings`) ve ilan detayı (`GET /api/postings/{id}`).

**Yakında:** ilan listesi/filtre/arama, başvurular, kaydedilenler, mesajlaşma.
Hepsi Swagger'da görünür; yeni uç eklendikçe sayfa kendiliğinden güncellenir.