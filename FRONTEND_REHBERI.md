# UniLink – Frontend Geliştirici Rehberi

Bu belge, UniLink'in React arayüzünü yazacak kişi için hazırlandı. **Projeyi hiç bilmediğini varsayar.**
Sırayla şunları anlatır: proje ne işe yarıyor, backend'i nasıl çalıştırırsın, API'nin genel kuralları neler,
hangi ekranda hangi isteği ne zaman atmalısın, her endpoint'in tam ayrıntısı ve sık yapılan hatalar.

> **Güncel kaynak:** Endpoint'lerin canlı ve her zaman güncel listesi Swagger sayfasındadır
> (`http://localhost:8080/swagger-ui.html`). Bu belge ise "neyi, ne zaman, neden" sorularını cevaplar.
> İkisi çelişirse Swagger doğrudur; bana haber ver, belgeyi düzeltirim.

**İçindekiler**

1. [UniLink nedir? Temel kavramlar](#1-unilink-nedir-temel-kavramlar)
2. [Hızlı başlangıç (ilk gün)](#2-hızlı-başlangıç-ilk-gün)
3. [API'nin genel kuralları](#3-apinin-genel-kuralları)
4. [Kimlik doğrulama: token'lar nasıl çalışır](#4-kimlik-doğrulama-tokenlar-nasıl-çalışır)
5. [Ekranlar ve akışlar: hangi sayfada hangi istek](#5-ekranlar-ve-akışlar-hangi-sayfada-hangi-istek)
6. [Endpoint referansı](#6-endpoint-referansı)
7. [Durumlar, kurallar ve yetkiler](#7-durumlar-kurallar-ve-yetkiler)
8. [Form doğrulama kuralları (alan sınırları)](#8-form-doğrulama-kuralları-alan-sınırları)
9. [React için öneriler ve hazır kod](#9-react-için-öneriler-ve-hazır-kod)
10. [Sık sorulan sorular ve tuzaklar](#10-sık-sorulan-sorular-ve-tuzaklar)
11. [Henüz hazır olmayanlar](#11-henüz-hazır-olmayanlar)
12. [Sözlük](#12-sözlük)

---

## 1. UniLink nedir? Temel kavramlar

**UniLink**, üniversite öğrencilerinin **proje, yarışma ve girişim fikirleri için ekip arkadaşı bulduğu**
bir web sitesidir. Sadece üniversite e-postasıyla (`.edu.tr` ile biten adres) kayıt olunabilir, yani kullanıcılar
gerçek öğrencilerdir. Bir öğrenci **ilan açar** ("Mobil uygulama için 2 backend, 1 tasarımcı arıyorum"),
diğer öğrenciler ilana **başvurur**, ilan sahibi başvuruları inceleyip **kabul ya da reddeder**.

### Temel kavramlar

| Kavram | Ne demek? |
|---|---|
| **Kullanıcı** | E-posta + şifreyle giriş yapan öğrenci. E-postasını doğrulamadan giriş yapamaz. |
| **Profil** | Kullanıcının herkese görünen bilgileri: ad, üniversite, bölüm, kısa tanıtım (bio), GitHub, LinkedIn, teknolojileri. Kayıt sırasında otomatik oluşur. |
| **İlan** | Ekip arayan kişinin yayınladığı kayıt. Başlık, açıklama, son tarih, aranan roller ve teknolojiler içerir. |
| **İlan türü** | `PROJECT` (okul/kişisel proje), `COMPETITION` (yarışma, hackathon), `STARTUP` (girişim). |
| **Rol** | İlandaki aranan pozisyon. Serbest metindir: `Backend`, `UI/UX`, `Mobil`... Bir ilanda birden fazla rol olur. |
| **Kontenjan** | Her rol için "kaç kişi aranıyor" sayısı. Örnek: `Backend x2, UI/UX x1`. Kabul edilen kişi sayısı kontenjana ulaşınca o rol **dolar**. |
| **Teknoloji** | Java, React, Docker gibi etiketler. Hem profilde hem ilanda kullanılır. Kullanıcı listeden seçebilir ya da yenisini yazarak ekleyebilir (LinkedIn'deki yetenekler gibi). |
| **Başvuru** | Bir kullanıcının bir ilana, bir ya da birkaç role yaptığı istek. İsteğe bağlı mesaj içerir. |
| **İlan sahibi** | İlanı açan kullanıcı. Başvuruları o görür ve karara bağlar. |
| **Başvuran** | Bir ilana başvuran kullanıcı. Aynı kişi bir ilanda sahip, başka bir ilanda başvuran olabilir. |
| **Kaydedilenler** | Kullanıcının daha sonra bakmak için işaretlediği ilanlar (favoriler). |

### Büyük resim: sistemin işleyişi

```
 Kayıt ──► E-posta doğrulama ──► Giriş ──► Anasayfa (ilan akışı)
                                              │
              ┌───────────────────────────────┼───────────────────────────┐
              ▼                               ▼                           ▼
        İlan aç / yönet               İlan detayı ► Başvur         Kaydedilenler
              │                               │
              ▼                               ▼
     Gelen başvurular ◄──────────────  Başvurularım (Başvurulan)
     (kabul / red)                      (bekliyor / kabul / red / geri çek)
```

1. Kullanıcı kayıt olur, e-postasına gelen linkle hesabını doğrular, giriş yapar.
2. Anasayfada **açık ilanları** görür; tür, teknoloji, rol, bölüm, üniversite ile filtreler, kelimeyle arar.
3. Beğendiği ilanı **kaydeder** ya da **başvurur** (hangi rol(ler)e başvuracağını seçer, mesaj yazar).
4. İlan sahibi, gelen başvuruları görür: başvuranın profili, mesajı, seçtiği roller. **Kabul** eder (hangi role yerleştirdiğini seçerek) ya da **reddeder**.
5. Başvuran, "Başvurulan" sayfasında durumunu takip eder; istediği zaman başvurusunu **geri çekebilir**.
6. İlanın **son tarihi** geçince ilan otomatik kapanır: anasayfadan kalkar ama başvuranlar ilanı hâlâ görebilir ("Başvuru kapandı").

---

## 2. Hızlı başlangıç (ilk gün)

### 2.1 Backend'i çalıştır

Gerekli olan tek şey **Docker Desktop** (Java/Maven kurmana gerek yok). Backend deposunu indir, klasörde:

```bash
docker compose --profile full up --build
```

İlk seferde birkaç dakika sürer. Sonra:

| Ne | Adres |
|---|---|
| API | `http://localhost:8080` |
| **Swagger** (tüm endpoint'ler, deneme arayüzü) | `http://localhost:8080/swagger-ui.html` |
| **Mailpit** (sistemin gönderdiği e-postalar) | `http://localhost:8025` |

- Durdurmak: `Ctrl+C`, sonra `docker compose --profile full down` (veriler kalır; sıfırlamak için sonuna `-v`).
- Backend kodu değişince `--build` ile yeniden çalıştır.
- Portlar boş olmalı: 8080, 8025, 1025, 5433.

### 2.2 Test hesabı oluştur

Gerçek e-posta göndermiyoruz; tüm e-postalar **Mailpit**'e düşer (`http://localhost:8025`).

1. Kayıt ol: `POST /api/auth/register` (Swagger'dan ya da kendi formundan). E-posta `...@...edu.tr` ile bitmeli.
   Örnek: `ali@test.edu.tr`, şifre `Sifre12345`.
2. Mailpit'i aç, gelen "UniLink e-posta doğrulama" mailindeki linke bak:
   `http://localhost:3000/verify-email?token=...`. Bu link **senin yazacağın sayfaya** gider (bkz. 5.2).
   Sayfan hazır değilse `token=` sonrasındaki değeri kopyalayıp Swagger'dan `POST /api/auth/verify-email` ile doğrula.
3. Giriş yap: `POST /api/auth/login`.

İlan açma, başvuru gibi akışları denemek için **en az iki kullanıcı** gerekir (biri ilan açar, biri başvurur).
Backend tarafında hazır bir **Postman koleksiyonu** var (`UniLink Başvuru Testi`): tek tıkla dört kullanıcı, birkaç ilan ve
tüm başvuru senaryosunu oluşturur. Bana sor, dosyayı sana göndereyim; verileri arayüzünü denemek için de kullanabilirsin.

### 2.3 CORS: tarayıcıdan istek atabilmek

Backend şu an sadece şu adreslerden gelen tarayıcı isteklerine izin veriyor:
`http://localhost:3000` (Create React App / Next.js) ve `http://localhost:5173` (Vite).
Farklı bir port kullanırsan **CORS hatası** alırsın; bana söyle, ayara eklerim.
İzin verilen başlıklar: `Authorization` ve `Content-Type`. Başka özel başlık göndermeyi planlıyorsan haber ver.

### 2.4 İlk gün kontrol listesi

- [ ] Backend çalışıyor, Swagger açılıyor.
- [ ] Mailpit açılıyor.
- [ ] İki test kullanıcısı oluşturdum ve giriş yaptım.
- [ ] Bu belgenin 3. ve 4. bölümünü okudum (hata biçimi, token mantığı).
- [ ] Axios/fetch istemcisini 9. bölümdeki gibi kurdum (özellikle **dizi parametreleri** ve **token yenileme**).

---

## 3. API'nin genel kuralları

### 3.1 Adres ve biçim

- Temel adres: `http://localhost:8080` (canlıda değişecek; adresi bir ortam değişkeninde tut: `VITE_API_URL` ya da `REACT_APP_API_URL`).
- Tüm adresler `/api/` ile başlar.
- İstek ve yanıt gövdeleri **JSON**'dur. Gövdeli isteklerde başlık: `Content-Type: application/json`.
- Alan adları **camelCase**'tir (`fullName`, `githubUrl`).
- Mesajlar ve hata metinleri **Türkçe**'dir, kullanıcıya olduğu gibi gösterilebilir.

### 3.2 Kimlik doğrulama özeti

`/api/auth/` ile başlayan adresler **herkese açıktır** (giriş yapmadan çağrılır). **Diğer her adres** giriş ister:

```
Authorization: Bearer <accessToken>
```

Token'ı nasıl alacağın, süresi dolunca ne yapacağın 4. bölümde.
İlanlar bile giriş gerektirir: giriş yapmamış biri hiçbir ilanı göremez. Bu bilinçli bir karar.

### 3.3 Tarih ve saat

- Tüm zamanlar **ISO 8601, UTC**: `2026-12-30T20:59:59Z`. Sonundaki `Z` UTC demektir.
- Kullanıcıya gösterirken yerel saate çevir (`new Date(deadline).toLocaleString("tr-TR")`).
- Tarih seçici (datetime-local) değerini göndermeden önce `new Date(value).toISOString()` ile çevir.
- İlan son tarihi **gelecekte** olmalı ve **en fazla 1 yıl** sonrası olabilir.

### 3.4 Kimlikler

Tüm kayıtların kimliği **UUID**'dir (`"0f98e878-2941-4e02-af53-5d0432385962"`). Sayı değil, metin olarak sakla ve adreste aynen kullan.
Geçersiz biçimde bir kimlik gönderirsen (`/api/postings/abc`) **400** döner: `'id' parametresi geçersiz.`

### 3.5 Sayfalama

Liste döndüren endpoint'ler **sayfalıdır**. İstek parametreleri:

| Parametre | Anlamı | Varsayılan |
|---|---|---|
| `page` | Sayfa numarası. **0'dan başlar** | `0` |
| `size` | Sayfa başına kayıt (1-50; dışındaysa sınıra çekilir) | endpoint'e göre 12 veya 20 |

Yanıt şekli (hepsinde aynı):

```json
{
  "content": [ /* kayıtlar */ ],
  "page": 0,
  "size": 12,
  "totalElements": 37,
  "totalPages": 4,
  "hasNext": true
}
```

- "Daha fazla yükle" (sonsuz kaydırma) için `hasNext` yeterlidir.
- Numaralı sayfa düğmeleri için `totalPages` kullan (unutma: ilk sayfa `0`).
- Boş liste hata değildir: `content: []`, `totalElements: 0`.

### 3.6 Hata biçimi

Uygulamanın **kendi ürettiği** tüm hatalar aynı biçimdedir:

```json
{
  "timestamp": "2026-10-19T10:15:30.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Gönderilen bilgilerde hatalar var.",
  "path": "/api/users/me",
  "fieldErrors": {
    "githubUrl": "GitHub bağlantısı https://github.com/ ile başlamalıdır."
  }
}
```

- **`message`**: Türkçe, kullanıcıya gösterilebilir.
- **`fieldErrors`**: Sadece form doğrulama hatalarında bulunur. Anahtar alan adıdır; hangi input'un altında hata göstereceğini buradan bulursun.
  İç içe/liste alanlarda anahtar şöyle görünür: `roles[0].requiredCount`, `technologies[]` (listedeki bir elemanın hatası).
- **Önemli istisna:** `401` yanıtının gövdesi **boştur**; bilinmeyen adres (404) ve yanlış metot (405) gibi framework hatalarının gövdesi de farklı olabilir.
  Bu yüzden her zaman önce **HTTP durum koduna** bak, mesaj okumayı ikinci sıraya koy (9. bölümdeki yardımcı fonksiyon bunu yapar).

### 3.7 Durum kodları: ne anlama geliyor, ne yapmalısın

| Kod | Anlamı | Arayüzde ne yapmalı |
|---|---|---|
| **200** | Başarılı, gövdede sonuç var | Sonucu göster |
| **201** | Oluşturuldu (kayıt, ilan, başvuru) | Başarı mesajı, yönlendir |
| **202** | Kabul edildi (şifre/doğrulama maili isteği) | "Eğer kayıtlıysa mail gönderdik" mesajı göster |
| **204** | Başarılı, gövde yok | Başarı göster |
| **400** | İstek geçersiz: form hatası ya da kural ihlali | `fieldErrors` varsa alanların altında, yoksa `message`'ı genel hata olarak göster |
| **401** | Giriş yapılmamış ya da **token süresi dolmuş** | Önce token'ı yenilemeyi dene (4.4). Yenileme de başarısızsa giriş sayfasına yönlendir |
| **403** | Yetkin yok (örn. ilanın sahibi değilsin) ya da giriş yaparken hesap durumu uygun değil | `message`'ı göster. Giriş sırasında 403 = e-posta doğrulanmamış ya da hesap kapalı (5.3) |
| **404** | Bulunamadı. **Bazı durumlarda "yetkin yok" yerine bilerek 404 döner** (bir ilan iptal edildiyse başkaları için yokmuş gibi davranılır) | "Bulunamadı" sayfası |
| **409** | Çakışma: "zaten başvurdun", "kontenjan dolu", "bu başvuru zaten sonuçlandırılmış" | `message`'ı göster, ilgili listeyi yenile |
| **500** | Sunucu hatası | "Bir sorun oluştu, tekrar dene" göster |

### 3.8 Dizi parametreleri (sık yapılan hata!)

Anasayfa filtresinde `technologies` birden fazla değer alabilir. Backend bunu **tekrarlı parametre** olarak bekler:

```
GET /api/postings?technologies=Java&technologies=React
```

**Axios'un varsayılanı farklıdır** (`technologies[]=Java&technologies[]=React`) ve **backend onu anlamaz**, filtre sessizce çalışmaz.
Çözüm axios örneğinde `paramsSerializer: { indexes: null }` ayarını kullanmak (9. bölüm). `fetch` ile `URLSearchParams.append` kullanırsan sorun olmaz.

---

## 4. Kimlik doğrulama: token'lar nasıl çalışır

### 4.1 İki token

Giriş yapınca backend **iki token** verir. Bunları "giriş kartı" gibi düşün:

| Token | Süresi | Ne işe yarar |
|---|---|---|
| **`accessToken`** | **15 dakika** | Her API isteğinde `Authorization: Bearer ...` başlığıyla gönderilir. Kısa ömürlüdür ki çalınsa bile zararı az olsun. |
| **`refreshToken`** | 7 gün | Sadece `accessToken` süresi dolunca yenisini almak için kullanılır. Başka hiçbir yerde gönderilmez. |

Süre dolunca kullanıcıya şifre sormak yerine arka planda token yenilenir; kullanıcı hiçbir şey fark etmez.

### 4.2 Giriş

`POST /api/auth/login`

```json
{ "email": "ali@test.edu.tr", "password": "Sifre12345" }
```

Yanıt (200):

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "9f3a1c7e5b0d...",
  "tokenType": "Bearer",
  "expiresInSeconds": 900
}
```

İki token'ı da sakla (4.3). Sonra kullanıcının bilgisini almak için `GET /api/users/me` çağır.

### 4.3 Token'ları nerede saklamalı?

| Seçenek | Artısı | Eksisi |
|---|---|---|
| `localStorage` | Sayfa yenilenince giriş korunur, basit | Sayfaya zararlı bir script girerse (XSS) okunabilir |
| Sadece bellek (React state) | Daha güvenli | Sayfa yenilenince oturum kaybolur (kullanıcı tekrar giriş yapar) |

Bitirme projesi için `localStorage` yeterlidir ve pratiktir. Ama kullanıcıdan gelen metni hiçbir zaman
`dangerouslySetInnerHTML` ile basma (XSS'in ana kapısı).

### 4.4 Süre dolunca: otomatik yenileme (en önemli kısım)

`accessToken` süresi dolduktan sonra yapılan istek **401** alır. Doğru davranış:

1. İsteği yapan kod 401 alır.
2. `refreshToken` ile `POST /api/auth/refresh` çağırır:
   ```json
   { "refreshToken": "9f3a1c7e5b0d..." }
   ```
   Yanıt, girişle **aynı şekilde yeni bir token çiftidir**.
3. **Yeni iki token'ı da kaydeder** (eskisini atar).
4. Başarısız olan ilk isteği yeni token'la **tekrar dener**. Kullanıcı hiçbir şey görmez.
5. Yenileme de **401** dönerse oturum bitmiştir: token'ları sil, giriş sayfasına yönlendir.

**Çok önemli iki kural:**

- **Refresh token tek kullanımlıktır.** Her yenilemede eski token iptal olur ve yenisi verilir. Yeni gelen `refreshToken`'ı kaydetmezsen sonraki yenileme başarısız olur ve kullanıcı sebepsiz yere çıkış yapmış olur.
- **Aynı anda sadece bir yenileme yapılmalı.** Sayfa açılırken 3 istek birden 401 alırsa, üçü de yenileme isterse ilki başarılı olur, diğer ikisi (artık iptal olmuş token'la) başarısız olur. Çözüm: devam eden bir yenileme varsa diğer istekler onu **bekler** (9. bölümdeki `refreshPromise` kalıbı).

### 4.5 Çıkış

`POST /api/auth/logout` gövde `{ "refreshToken": "..." }` → her zaman `204`. Sonra iki token'ı sil ve giriş sayfasına git.
Bu çağrı refresh token'ı sunucuda iptal eder, böylece çalınmış olsa bile işe yaramaz.
Not: Çıkıştan sonra `accessToken` süresi dolana kadar (en fazla 15 dk) teknik olarak geçerli kalır; arayüzde sildiğin için sorun olmaz.

### 4.6 Uygulama ilk açıldığında

Sayfa yenilenince (ya da ilk açılışta):

1. Saklı token yoksa → giriş sayfası.
2. Token varsa → `GET /api/users/me` çağır. Başarılıysa kullanıcı giriş yapmış sayılır (yanıt "ben kimim" bilgisini de taşır: `userId`, `fullName`...).
   401 dönerse yenileme akışı (4.4) devreye girer; o da başarısızsa giriş sayfası.
3. Bu bilgiyi **global bir yerde** tut (Context ya da Zustand/Redux). "Bu ilanın sahibi ben miyim?" kontrolünde `me.userId`'yi kullanacaksın.

### 4.7 Hesap kapatılırsa

Bir kullanıcı yönetici tarafından banlanırsa mevcut `accessToken`'ı en fazla 15 dakika çalışmaya devam eder, ama yenileme istediğinde **403** alır
(`Hesabınız devre dışı bırakılmış.`). Yenilemede 403 gelirse de kullanıcıyı çıkış yaptır ve mesajı göster.

---

## 5. Ekranlar ve akışlar: hangi sayfada hangi istek

Bu bölüm sayfa sayfa **"ne zaman hangi isteği atmalısın"** sorusunu cevaplar. Önerilen sayfa adresleri tabloda:

| Sayfa | Önerilen adres | Giriş gerekli mi? |
|---|---|---|
| Kayıt | `/register` | Hayır |
| Giriş | `/login` | Hayır |
| E-posta doğrulama | `/verify-email?token=...` | Hayır |
| Şifremi unuttum | `/forgot-password` | Hayır |
| Şifre sıfırlama | `/reset-password?token=...` | Hayır |
| Anasayfa (ilan akışı) | `/` | Evet |
| İlan detayı | `/postings/:id` | Evet |
| İlan aç | `/postings/new` | Evet |
| İlan düzenle | `/postings/:id/edit` | Evet (sadece sahibi) |
| Bir ilanın başvuruları | `/postings/:id/applications` | Evet (sadece sahibi) |
| Başvurulan (başvurularım) | `/applications` | Evet |
| Kaydedilenler | `/saved` | Evet |
| Profil | `/profile` | Evet |

> **İki adres sabit:** E-postadaki linkler backend tarafından şu iki adrese üretilir ve **sen bu sayfaları aynı adreslerle yazmak zorundasın**:
> `/verify-email?token=...` ve `/reset-password?token=...`. (Adres ayarı backend'de `app.frontend-url`.)

### 5.1 Kayıt sayfası (`/register`)

**Form alanları:** e-posta, şifre, ad soyad, üniversite, bölüm.

**Akış:**
1. Kullanıcı formu doldurur. Sayfada ön kontrol yap: e-posta `.edu.tr` ile bitiyor mu, şifre 8-72 karakter mi (kural listesi 8. bölümde).
2. `POST /api/auth/register` → **201**. Yanıtta `emailVerified: false` gelir.
3. Kullanıcıya "**E-postana doğrulama linki gönderdik**" ekranı göster (giriş sayfasına yönlendirme).

**Hatalar:**
- `400` + `fieldErrors` → alanların altında göster. `.edu.tr` değilse `message`: "Kayıt için .edu.tr uzantılı bir e-posta adresi gereklidir."
- `409` → "Bu e-posta adresi zaten kayıtlı." (giriş sayfasına yönlendiren bir link ekle).

### 5.2 E-posta doğrulama sayfası (`/verify-email?token=...`)

Kullanıcı mailindeki linke tıklayınca bu sayfa açılır.

**Akış:**
1. Sayfa açılınca adresteki `token` değerini oku.
2. `POST /api/auth/verify-email` gövde `{ "token": "..." }`.
3. **204** → "Hesabın doğrulandı" + "Giriş yap" düğmesi.
4. **400** → "Doğrulama bağlantısı geçersiz veya süresi dolmuş" + yeni link isteme formu (`POST /api/auth/resend-verification`, gövde `{ "email": "..." }`, her zaman 202).

**Tuzak (React):** Geliştirme modunda (`StrictMode`) efektler **iki kez** çalışır. Doğrulama isteğini iki kez atarsan
ikincisi **400** alır (token ilkinde kullanıldı) ve kullanıcı "geçersiz" mesajı görür. İsteği bir `useRef` bayrağıyla **sadece bir kere** at.

**Bilmen gerekenler:**
- Doğrulama linki **24 saat** geçerli.
- Yeni doğrulama maili isteği `202` döner ve e-posta kayıtlı olsun olmasın aynı yanıtı verir (güvenlik için). Aynı adrese **1 dakikada birden** istek atılırsa yine 202 döner ama yeni mail gönderilmez; arayüzde "tekrar gönder" düğmesine 60 saniyelik bekleme koyman iyi olur.

### 5.3 Giriş sayfası (`/login`)

1. `POST /api/auth/login` gövde `{ email, password }`.
2. **200** → token'ları sakla (4.3), `GET /api/users/me` ile kullanıcı bilgisini yükle, anasayfaya git.
3. Hatalar:
    - **401** → "E-posta veya şifre hatalı." (iki durumu bilerek ayırmıyoruz; kullanıcı hangi e-postanın kayıtlı olduğunu öğrenemesin).
    - **403**, mesaj `E-posta adresiniz henüz doğrulanmadı.` → "Doğrulama mailini tekrar gönder" düğmesi göster (`POST /api/auth/resend-verification`).
    - **403**, mesaj `Hesabınız devre dışı bırakılmış.` → mesajı göster, başka seçenek yok.
    - **400** → form hatası (`email` geçerli değil, şifre boş).
4. Sayfada "Şifremi unuttum" linki olsun (5.4).

### 5.4 Şifremi unuttum ve şifre sıfırlama

**`/forgot-password`:**
1. Kullanıcı e-postasını girer. `POST /api/auth/forgot-password` gövde `{ "email": "..." }`.
2. Her durumda **202**. Mesaj: "Bu adres kayıtlıysa şifre sıfırlama bağlantısı gönderdik." (kayıtlı olup olmadığını **asla** söyleme).

**`/reset-password?token=...`** (e-postadaki link bu sayfaya gelir):
1. Kullanıcı yeni şifreyi girer (8-72 karakter, "şifre tekrar" alanı sadece arayüzde).
2. `POST /api/auth/reset-password` gövde `{ "token": "...", "newPassword": "..." }`.
3. **204** → "Şifren değişti" + giriş sayfasına yönlendir. (Şifre değişince kullanıcının tüm açık oturumları kapanır.)
4. **400** → "Bağlantı geçersiz veya süresi dolmuş" ya da şifre kuralı hatası (`fieldErrors.newPassword`).

Sıfırlama linki **1 saat** geçerlidir ve **tek kullanımlıktır**. (Burada da aynı `StrictMode` tuzağı yok, çünkü istek bir düğmeye basınca atılır.)

### 5.5 Uygulama iskeleti: sol menü ve rozet

Tasarım taslağındaki sol menü maddeleri ve bağlandıkları yerler:

| Menü | Sayfa | Not |
|---|---|---|
| Anasayfa | `/` | İlan akışı |
| Mesaj | — | **Henüz yok** (11. bölüm) |
| Profil | `/profile` | |
| Settings | — | Şimdilik yok (şifre değiştirme, hesap silme uçları yok) |
| İlan aç | `/postings/new` | |
| Kaydedilenler | `/saved` | |
| Başvurulan | `/applications` | |
| Logout | — | 4.5'teki çıkış |

**Bekleyen başvuru rozeti:** İlan sahibinin "henüz cevaplamadığı başvuru sayısı" menüde (örn. "Gelen başvurular" ya da profil/ilan menüsünde) bir rozet olarak görünür.

- İstek: `GET /api/applications/incoming/pending-count` → `{ "pendingCount": 3 }`
- **Ne zaman çağır:** uygulama ilk yüklendiğinde (giriş yapıldıktan sonra), kullanıcı bir başvuruyu kabul/red ettikten sonra ve istersen 60 saniyede bir. (Anlık bildirim/WebSocket henüz yok.)
- Sayı, ilan sahibinin **iptal edilmemiş** ilanlarına gelen **bekleyen** başvuruların toplamıdır.
- Rozet için bir **ilan bazında** sayı yoktur (sadece toplam).

### 5.6 Anasayfa: ilan akışı (`/`)

Giriş yapmış kullanıcının ilk gördüğü yer. Sadece **açık** ilanlar listelenir (durumu açık ve son tarihi geçmemiş).

**İstek:** `GET /api/postings` + filtre parametreleri (hepsi isteğe bağlı):

| Arayüz öğesi | Parametre | Örnek |
|---|---|---|
| Arama çubuğu ("Ara") | `query` | `query=mobil` (başlıkta ve organizasyon adında arar) |
| Tür filtresi | `type` | `type=STARTUP` (`PROJECT`, `COMPETITION`, `STARTUP`) |
| Teknoloji filtresi (çoklu seçim) | `technologies` (tekrarlı) | `technologies=Java&technologies=React` (**herhangi biri** olan ilanlar) |
| Rol filtresi | `role` | `role=backend` (rol adında geçen metin) |
| Bölüm filtresi | `department` | `department=bilgisayar` (**ilan sahibinin** bölümü) |
| Üniversite filtresi | `university` | `university=ege` (**ilan sahibinin** üniversitesi) |
| "Sadece boş yeri olanlar" | `hasVacancy` | `hasVacancy=true` |
| Sıralama | `sort` | `NEWEST` (en yeni, varsayılan) ya da `DEADLINE` (son tarihi en yakın) |
| Sayfalama | `page`, `size` | `page=0&size=12` |

**Arama davranışı:**
- Büyük/küçük harf ve **Türkçe harfler** önemsizdir: `OGRENCI`, `öğrenci`, `ÖĞRENCİ` aynı sonucu verir.
- `query`, `role`, `department`, `university` **"içerir"** mantığıyla çalışır (`makine` → "Makine Mühendisliği").
- `technologies` **tam isimle** eşleşir (`Java` ≠ `Javascript`).
- Filtreler birlikte kullanılabilir; hepsi birbirini **daraltır**.

**Ne zaman çağır:**
- Sayfa açılınca (filtresiz).
- Kullanıcı bir filtreyi ya da sıralamayı değiştirince (sayfayı 0'a sıfırla).
- Arama kutusuna yazarken **300-500 ms gecikmeli (debounce)**; her tuşta istek atma.
- Sayfa değişince ya da "daha fazla yükle"ye basılınca.

**Teknoloji filtresi için öneri kutusu:** Kullanıcı yazarken `GET /api/technologies?query=spr` çağır (en fazla 10 öneri, `[{ "id", "name" }]`).
Filtre için sadece **isim** (`name`) kullanılır. Boş `query` tüm teknolojileri döndürür (yaklaşık 45; açılır liste için uygun).

**Her ilan kartında gösterilecekler** (tasarım taslağıyla eşleşme):

| Taslaktaki alan | Yanıttaki karşılığı |
|---|---|
| Başlık | `title` |
| Bölüm | `owner.department` (ilan sahibinin bölümü) |
| Teknoloji | `technologies[].name` |
| Şirket | `organizationName` (**boş olabilir**, doluysa göster; yarışma/organizasyon adı) |
| Açıklama | `shortDescription` (ilk 200 karakter, uzunsa sonunda `…`) |
| İncele düğmesi | `/postings/{id}` sayfasına gider |
| Ek: tür rozeti | `type` |
| Ek: kalan kontenjan | `totalRemaining` (tüm rollerdeki boş yer toplamı; `0` ise "Kontenjan dolu" rozeti) |
| Ek: roller | `roles[]` (`role`, `remainingCount`) |
| Ek: son tarih | `deadline` |
| Ek: sahibi | `owner.fullName`, `owner.university` |

Kartta **`open: false`** gelen bir ilan akışta normalde görünmez (akış sadece açık ilanları listeler); bu alan "Kaydedilenler" listesinde önemlidir (5.12).

**Boş durum:** `content: []` ise "Aramana uygun ilan bulunamadı" göster.

### 5.7 İlan detay sayfası (`/postings/:id`)

**İstek:** `GET /api/postings/{id}`. Yanıt karttan daha ayrıntılıdır: tam `description`, `stage` (sadece girişim), `benefitsNote`
("ekibe katılan ne kazanır"), `status`, `open`, roller (`requiredCount`, `acceptedCount`, `remainingCount`) ve `createdAt`.

**Sayfada neye göre ne gösterilir:**

| Durum | Nasıl anlaşılır | Ne göster |
|---|---|---|
| Ben ilanın sahibiyim | `owner.id === me.userId` | "Düzenle", "İlanı iptal et", "Başvurular" düğmeleri. **"Başvur" düğmesi gösterme** (kendi ilanına başvurulamaz). |
| İlan açık | `open === true` | "Başvur" düğmesi (sahibi değilse) |
| İlan kapalı | `open === false` ve `status !== "CANCELLED"` | "**Başvuru kapandı**" bandı; "Başvur" düğmesi yok |
| İlan iptal edilmiş | `status === "CANCELLED"` | "**İlan iptal edildi**" bandı. (Bu ilanı sadece sahibi görebilir; başkaları 404 alır.) |
| Rol dolmuş | rolde `remainingCount === 0` | O rolün yanında "Dolu" rozeti. (Backend dolu role başvuruyu **reddetmez**; sahibi kabul edemez. Dolu rolü seçtirmemek arayüz kararıdır, öneririm.) |

> **`status` mı, `open` mı?** Durumu göstermek için **`open`** alanına güven. `status` alanı `OPEN` görünse bile son tarih geçmişse
> (arka plan işi en geç 1 dakikada günceller) `open` zaten `false` olur.

**Sayfa açılınca ek olarak ne çağırmalı?**

- **Kullanıcının bu ilana daha önce başvurup başvurmadığını** göstermek için `GET /api/applications/mine?size=50` çağırıp
  listede `posting.id === ilanId` olanı bul (varsa durumunu göster: Bekliyor/Kabul edildi...). Şimdilik ilan yanıtında bu bilgi yok.
- **İlanın kaydedilmiş olup olmadığını** göstermek (kalp ikonu): ilan yanıtına `saved` bayrağı **yakında eklenecek**.
  O zamana kadar `GET /api/saved-postings?size=50` ile kaydedilen ilanların id listesini alıp kontrol edebilirsin.

**Kaydet / kaydı kaldır:** `PUT /api/saved-postings/{id}` ve `DELETE /api/saved-postings/{id}` (ikisi de `204`; tekrar çağırmak hata vermez).

### 5.8 İlan aç ve düzenle formu (`/postings/new`, `/postings/:id/edit`)

**Form alanları:**

| Alan | Zorunlu | Not |
|---|---|---|
| Tür (`type`) | Evet | `PROJECT` / `COMPETITION` / `STARTUP`. **Düzenlemede değiştirilemez.** |
| Başlık (`title`) | Evet | En fazla 150 karakter |
| Açıklama (`description`) | Evet | En fazla 5000 karakter |
| Organizasyon/yarışma adı (`organizationName`) | Hayır | Örn. "Teknofest". Sadece yarışma ilanlarında anlamlı, ama her türde yazılabilir |
| Aşama (`stage`) | Hayır | **Sadece girişim (`STARTUP`) türünde** gösterilsin: `IDEA` (Fikir), `PROTOTYPE` (Prototip), `PRODUCT` (Ürün). Diğer türlerde gönderirsen 400 alırsın |
| Fayda notu (`benefitsNote`) | Hayır | "Ekibe katılan ne kazanır" (hisse, ücret, portfolyo). En fazla 500 karakter. Girişim ilanlarında özellikle teşvik et |
| Son tarih (`deadline`) | Evet | Gelecekte ve en fazla 1 yıl sonrası. ISO UTC olarak gönder |
| Roller (`roles`) | Evet (oluştururken) | 1-10 satır. Her satır: rol adı + kişi sayısı (1-50). **Aynı rol adı iki kez yazılamaz.** Düzenlemede roller değiştirilemez |
| Teknolojiler (`technologies`) | Hayır | En fazla 20; LinkedIn tarzı giriş (aşağıda) |

**Rol satırı arayüzü:** "+ Rol ekle" düğmesi, satır başına silme düğmesi. Hazır öneriler sunabilirsin
(`Backend`, `Frontend`, `Mobil`, `UI/UX`, `DevOps`, `Veri`, `ML Mühendisi`...) ama kullanıcı serbestçe yazabilmeli.

**Teknoloji giriş arayüzü (LinkedIn'deki yetenek ekleme mantığı):**
1. Kullanıcı yazarken `GET /api/technologies?query=...` ile öneri listesi göster.
2. Listeden birini seçerse o ismi ekle.
3. Listede olmayan bir şey yazıp Enter'a basarsa **yazdığı metni olduğu gibi** ekle. Backend yeni teknolojiyi kendisi oluşturur.
4. Seçilen teknolojiler etiket (chip) olarak görünür, çarpıyla silinir.
5. Backend'e **id değil, isim listesi** gönderilir: `"technologies": ["Java", "React", "Qdrant"]`.
   (Aynı isim daha önce varsa büyük/küçük harfe bakmadan mevcut olanı kullanır: `spring boot` yazan kullanıcı `Spring Boot` etiketini alır.)

**İstekler:**
- Oluşturma: `POST /api/postings` → **201**, yanıt tam ilan (`PostingResponse`). Yanıttaki `id` ile detay sayfasına yönlendir.
- Düzenleme: `PUT /api/postings/{id}` → **200**. Önce `GET /api/postings/{id}` ile formu doldur. **`PUT` tüm alanları değiştirir**, yani forma ait her alanı (`title`, `description`, `organizationName`, `stage`, `benefitsNote`, `deadline`, `technologies`) **göndermek zorundasın**. Göndermediğin isteğe bağlı alan silinir (boşalır).
- İptal: `DELETE /api/postings/{id}` → **204**. **Geri alınamaz.** Onay penceresi göster ("Bu ilan yayından kalkar, başvurular sonuçlandırılamaz").

**Düzenlemenin özel kuralları:**
- Süresi dolmuş (kapanmış) bir ilanın son tarihini **ileri bir tarihe** alırsan ilan **otomatik yeniden açılır**.
- İptal edilmiş ilan düzenlenemez (400: "İptal edilmiş ilan düzenlenemez.").
- İlanın sahibi değilsen 403.

**Hata gösterimi:** `400` + `fieldErrors` anahtarları: `title`, `description`, `deadline`, `roles`, `roles[0].requiredCount`, `technologies[]`.
Kural ihlalleri (`fieldErrors` olmadan, sadece `message`): "Aşama bilgisi sadece girişim ilanlarında kullanılabilir.",
"Son tarih en fazla 1 yıl sonrası olabilir.", "Aynı rol birden fazla kez eklenemez: X". Bunları formun üstünde genel hata olarak göster.

### 5.9 Başvur (ilan detayında açılan pencere)

Sadece ilan açıksa (`open === true`) ve kullanıcı ilanın sahibi değilse gösterilir.

**Pencere içeriği:**
1. İlanın **rollerini** onay kutularıyla listele (`roles[]`: `id`, `role`, `remainingCount`). Kullanıcı **1 ile 5 arası** rol seçer.
2. İsteğe bağlı mesaj alanı (en fazla 1000 karakter): "Kendini kısaca tanıt".
3. "Başvur" düğmesi.

**İstek:** `POST /api/postings/{postingId}/applications`

```json
{
  "message": "Merhaba, backend rolüne ilgi duyuyorum.",
  "roleIds": ["<rolun-id-si>", "<digerrol-id>"]
}
```

`roleIds` rollerin **`id`'leridir** (ilan detayındaki `roles[].id`), rol adı değil.

- **201** → başvuru oluştu. Yanıtta `status: "PENDING"` (bekliyor). Düğmeyi "Başvuruldu (Bekliyor)" yap ya da Başvurulan sayfasına yönlendir.
- **409**, `Bu ilana zaten başvurdunuz.` → mevcut başvurunu göster.
- **409**, `Başvurunuz reddedildiği için bu ilana tekrar başvuramazsınız.` → reddedilen kullanıcı yeniden başvuramaz.
- **400**, `Bu ilan artık başvuru kabul etmiyor.` → ilan kapanmış; sayfayı yenile.
- **400**, `Kendi ilanınıza başvuramazsınız.` → (arayüz bu düğmeyi zaten göstermemeli.)
- **400**, `Seçilen roller bu ilana ait değil.` → (beklenmez; yanlış id gönderilmiştir.)
- **404** → ilan yok ya da iptal edilmiş.

**Geri çekilen başvuruyu yeniden yapmak:** Kullanıcı başvurusunu geri çekmişse (`WITHDRAWN`) aynı ilana **tekrar başvurabilir**;
aynı istek yine `201` döner ve **eski başvuru yeniden açılır** (aynı `id`).

### 5.10 Başvurulan sayfası (`/applications`)

Kullanıcının yaptığı tüm başvurular.

**İstek:** `GET /api/applications/mine?page=0&size=20` (en son güncellenen başvuru en üstte).

**Her satırda göster:**

| Ne | Alan |
|---|---|
| İlan başlığı (ilana link) | `posting.title`, `posting.id` |
| Başvuru durumu | `status` → Bekliyor / Kabul edildi / Reddedildi / Geri çekildi (rozet) |
| Yerleştirildiği rol (kabulde) | `acceptedRole.role` |
| Başvurduğu roller | `appliedRoles[].role` |
| Yazdığı mesaj | `message` |
| İlanın durumu | `posting.open`, `posting.status` |

**İlan durumu uyarıları:**
- `posting.status === "CANCELLED"` → "İlan iptal edildi".
- `posting.open === false` (ve iptal değil) → "**Başvuru kapandı**" (ilanın süresi doldu; mevcut başvuru yine de görünür).
- Başvuru ilan kapanmadan önce yapıldıysa da **listede kalır**; hiçbir şey kaybolmaz.

**Geri çekme:** `status` `PENDING` ya da `ACCEPTED` ise "Geri çek" düğmesi göster (onay penceresiyle).
`POST /api/applications/{id}/withdraw` → **200**, yanıt güncel başvuru (`status: "WITHDRAWN"`).
Kabul edilmiş bir başvuruyu geri çekmek **rolün kontenjanını tekrar açar**. `REJECTED` ve `WITHDRAWN` başvurular geri çekilemez (409: "Bu başvuru geri çekilemez.").
Bu işlem iptal edilmiş ilanda da yapılabilir.

### 5.11 Gelen başvurular (ilan sahibi için, `/postings/:id/applications`)

İlan sahibi, kendi ilanına gelen başvuruları burada inceler. İlan detay sayfasındaki "Başvurular" düğmesinden gelinir.

**İstek:** `GET /api/postings/{postingId}/applications?status=PENDING&page=0&size=20`
(`status` isteğe bağlı: `PENDING`, `ACCEPTED`, `REJECTED`, `WITHDRAWN`; sekmeler için kullan: Bekleyen / Kabul edilen / Reddedilen / Hepsi.)
İlanın sahibi değilsen **403** alırsın.

**Her başvuru kartında göster:**

| Ne | Alan |
|---|---|
| Başvuranın adı, üniversitesi, bölümü | `applicant.fullName`, `.university`, `.department` |
| Kısa tanıtımı | `applicant.bio` |
| GitHub / LinkedIn bağlantıları | `applicant.githubUrl`, `.linkedinUrl` (boş olabilir; yeni sekmede aç) |
| Teknolojileri | `applicant.technologies[].name` |
| Başvuru mesajı | `message` |
| Başvurduğu roller | `appliedRoles[]` (`id`, `role`) |
| Durum | `status` |
| E-posta | `applicant.email` (**sadece kabul edilmiş başvuruda dolu**, aksi hâlde `null`; aşağıya bak) |

> **E-posta gizliliği:** Başvuranın e-postası, başvuru **kabul edilene kadar** gizlidir (`null`). Kabul edildikten sonra dolu gelir;
> ilan sahibi ekibi kurmak için iletişime geçebilir. Arayüzde `null` ise e-posta alanını hiç gösterme.
> Başvuranın profili **canlıdır**: başvuru yapıldıktan sonra profilini güncellerse yeni hâli görünür.

**Kabul etme:**
1. "Kabul et" düğmesine basınca, başvuranın **başvurduğu rollerden** birini seçtir (`appliedRoles`). Tek rol başvurmuşsa doğrudan onu kullan.
   İpucu: rolün dolu olup olmadığını önceden göstermek için ilanın `roles[].remainingCount` değerini (`GET /api/postings/{id}`) kullanabilirsin.
2. `POST /api/applications/{id}/accept`, gövde `{ "roleId": "<secilen-rol-id>" }` → **200**, yanıt güncel başvuru (`status: "ACCEPTED"`, `acceptedRole`, `applicant.email` dolu).

**Reddetme:** `POST /api/applications/{id}/reject` (gövdesiz) → **200**, `status: "REJECTED"`.

**Hatalar ve ne yapmalı:**
- **409**, `Bu rolün kontenjanı dolu.` → başka bir rol seçtir ya da reddet. İlan verisini yenile.
- **409**, `Bu başvuru zaten sonuçlandırılmış.` → listeyi yenile (başvuran bu sırada geri çekmiş ya da başka bir sekmeden karar verilmiş olabilir).
- **400**, `Seçilen rol, başvuranın başvurduğu roller arasında değil.` → yanlış rol gönderilmiş.
- **400**, `İptal edilmiş ilandaki başvurular sonuçlandırılamaz.`
- **404**, `Başvuru bulunamadı.` → id yanlış ya da bu başvuru senin ilanında değil.

Bir karar verdikten sonra **menüdeki rozeti de güncelle** (`GET /api/applications/incoming/pending-count`).
Süresi dolmuş (kapanmış) ilanda da karar verilebilir; iptal edilmiş ilanda verilemez.

### 5.12 Kaydedilenler (`/saved`)

**İstek:** `GET /api/saved-postings?page=0&size=12`. Anasayfadaki **kartlarla aynı biçimde** gelir; aynı kart bileşenini kullan.

- En son kaydedilen en üstte.
- Kaydedilen bir ilan **sonradan kapanırsa listede kalır**, kartta `open: false` olur; "Başvuru kapandı" rozeti göster.
- Kaydedilen bir ilan **iptal edilirse listeden kalkar**.
- Kayıttan çıkarma: `DELETE /api/saved-postings/{id}` → 204, sonra listeden kaldır.
- İlan kaydetmek: `PUT /api/saved-postings/{id}` → 204 (kartlardaki kalp ikonu).

### 5.13 Profil sayfası (`/profile`)

**İstek:** `GET /api/users/me` → profil bilgileri:
`userId`, `email`, `fullName`, `university`, `department`, `bio`, `githubUrl`, `linkedinUrl`, `technologies[]`.

**Profil bilgisini güncelleme:** `PUT /api/users/me` → **200**, güncel profil. **Tüm alanları** gönder
(`fullName`, `university`, `department` zorunlu; `bio`, `githubUrl`, `linkedinUrl` isteğe bağlı ve **gönderilmezse ya da boş gönderilirse silinir**).
- GitHub bağlantısı `https://github.com/...` ile, LinkedIn `https://linkedin.com/...` ya da `https://www.linkedin.com/...` ile başlamalı. Boş bırakmak serbest.

**Teknolojileri güncelleme:** `PUT /api/users/me/technologies` gövde `{ "technologies": ["Java", "Spring Boot"] }` → **200**, güncel profil.
Bu liste mevcut listeyi **tamamen değiştirir** (boş liste gönderirsen hepsi silinir). Giriş arayüzü ilan formundakiyle aynı (5.8).

**E-posta ve şifre değiştirme:** Şimdilik yok.

**Başka kullanıcıların profili:** Ayrı bir "kullanıcı profili sayfası" endpoint'i **yok**. Bir kullanıcının bilgisi sadece iki yerde görünür:
ilan kartı/detayındaki `owner` (ad, üniversite, bölüm) ve ilan sahibine gelen başvurulardaki `applicant` (tam profil).

### 5.14 Çıkış

Menüdeki "Logout": `POST /api/auth/logout` (gövde `{ "refreshToken": "..." }`), sonra saklı token'ları ve kullanıcı bilgisini sil, `/login`'e git.
Çıkış isteği hata verse bile (ağ vb.) token'ları yerelde sil.

---

## 6. Endpoint referansı

### 6.1 Tüm endpoint'ler bir bakışta

| # | Metot ve adres | Giriş | Ne yapar | Başarı |
|---|---|---|---|---|
| 1 | `POST /api/auth/register` | Hayır | Kayıt ol | 201 |
| 2 | `POST /api/auth/verify-email` | Hayır | E-postayı doğrula | 204 |
| 3 | `POST /api/auth/resend-verification` | Hayır | Doğrulama mailini yeniden iste | 202 |
| 4 | `POST /api/auth/login` | Hayır | Giriş yap, token al | 200 |
| 5 | `POST /api/auth/refresh` | Hayır | Token'ı yenile | 200 |
| 6 | `POST /api/auth/logout` | Hayır | Çıkış yap (refresh token'ı iptal et) | 204 |
| 7 | `POST /api/auth/forgot-password` | Hayır | Şifre sıfırlama maili iste | 202 |
| 8 | `POST /api/auth/reset-password` | Hayır | Yeni şifre belirle | 204 |
| 9 | `GET /api/users/me` | Evet | Kendi profilim | 200 |
| 10 | `PUT /api/users/me` | Evet | Profil bilgilerimi güncelle | 200 |
| 11 | `PUT /api/users/me/technologies` | Evet | Profil teknolojilerimi değiştir | 200 |
| 12 | `GET /api/technologies?query=` | Evet | Teknoloji ara / listele | 200 |
| 13 | `GET /api/postings` | Evet | İlan akışı (filtre, arama, sayfalama) | 200 |
| 14 | `GET /api/postings/{id}` | Evet | İlan detayı | 200 |
| 15 | `POST /api/postings` | Evet | İlan oluştur | 201 |
| 16 | `PUT /api/postings/{id}` | Evet (sahibi) | İlanı düzenle | 200 |
| 17 | `DELETE /api/postings/{id}` | Evet (sahibi) | İlanı iptal et | 204 |
| 18 | `POST /api/postings/{postingId}/applications` | Evet | İlana başvur | 201 |
| 19 | `GET /api/applications/mine` | Evet | Başvurularım | 200 |
| 20 | `GET /api/postings/{postingId}/applications` | Evet (sahibi) | İlanıma gelen başvurular | 200 |
| 21 | `GET /api/applications/incoming/pending-count` | Evet | Bekleyen başvuru sayım (rozet) | 200 |
| 22 | `POST /api/applications/{id}/accept` | Evet (ilan sahibi) | Başvuruyu kabul et | 200 |
| 23 | `POST /api/applications/{id}/reject` | Evet (ilan sahibi) | Başvuruyu reddet | 200 |
| 24 | `POST /api/applications/{id}/withdraw` | Evet (başvuran) | Başvurumu geri çek | 200 |
| 25 | `PUT /api/saved-postings/{postingId}` | Evet | İlanı kaydet | 204 |
| 26 | `DELETE /api/saved-postings/{postingId}` | Evet | Kaydı kaldır | 204 |
| 27 | `GET /api/saved-postings` | Evet | Kaydettiğim ilanlar | 200 |

Aşağıdaki tüm "Hatalar" listeleri, **o endpoint'e özgü** hataları gösterir. Her korumalı endpoint ayrıca token yoksa/süresi dolmuşsa **401**,
geçersiz kimlik biçiminde **400** döndürür. Bozuk JSON gönderirsen **400** (`İstek gövdesi okunamadı veya biçimi hatalı.`).

### 6.2 Kimlik doğrulama endpoint'leri

#### `POST /api/auth/register`
```json
{
  "email": "ali@test.edu.tr", "password": "Sifre12345", "fullName": "Ali Demir",
  "university": "Test Üniversitesi", "department": "Bilgisayar Mühendisliği"
}
```
**201:** `{ "userId": "...", "email": "ali@test.edu.tr", "emailVerified": false }`
**Hatalar:** 400 (alan hataları ya da `.edu.tr` değil), 409 (`Bu e-posta adresi zaten kayıtlı.`).
E-posta küçük harfe çevrilip saklanır.

#### `POST /api/auth/verify-email`
`{ "token": "..." }` → **204**. **400:** `Doğrulama bağlantısı geçersiz veya süresi dolmuş.` (yanlış, kullanılmış ya da 24 saati geçmiş token).

#### `POST /api/auth/resend-verification`
`{ "email": "..." }` → **202** (her zaman). Hesap yoksa, zaten doğrulanmışsa ya da son mailden 1 dakika geçmediyse mail gönderilmez ama yanıt aynıdır.

#### `POST /api/auth/login`
`{ "email": "...", "password": "..." }` → **200** `{ accessToken, refreshToken, tokenType: "Bearer", expiresInSeconds: 900 }`
**Hatalar:** 401 `E-posta veya şifre hatalı.`, 403 `E-posta adresiniz henüz doğrulanmadı.`, 403 `Hesabınız devre dışı bırakılmış.`

#### `POST /api/auth/refresh`
`{ "refreshToken": "..." }` → **200**, `login` ile aynı biçimde **yeni token çifti** (eski refresh token iptal olur).
**Hatalar:** 401 `Oturum yenilenemedi, lütfen tekrar giriş yapın.`, 403 `Hesabınız devre dışı bırakılmış.`

#### `POST /api/auth/logout`
`{ "refreshToken": "..." }` → **204** (token geçersiz ya da zaten iptal olsa bile). **400** yalnızca alan boşsa.

#### `POST /api/auth/forgot-password`
`{ "email": "..." }` → **202** (her zaman, hesap olsa da olmasa da).

#### `POST /api/auth/reset-password`
`{ "token": "...", "newPassword": "..." }` → **204**. **400:** `Şifre sıfırlama bağlantısı geçersiz veya süresi dolmuş.` ya da şifre kuralı (`fieldErrors.newPassword`).
Başarılı olunca kullanıcının **tüm oturumları kapanır** (refresh token'lar iptal olur).

### 6.3 Profil ve teknoloji endpoint'leri

#### `GET /api/users/me` → 200 `Profil`
```json
{
  "userId": "c7dbe98b-2669-489e-88b8-e1bcd1dd9bfd",
  "email": "ali@test.edu.tr",
  "fullName": "Ali Demir",
  "university": "Test Üniversitesi",
  "department": "Bilgisayar Mühendisliği",
  "bio": "Backend geliştirmeyi seviyorum.",
  "githubUrl": "https://github.com/alidemir",
  "linkedinUrl": "https://www.linkedin.com/in/alidemir",
  "technologies": [ { "id": "3dd78c90-...", "name": "Java" }, { "id": "a1b2c3...", "name": "Spring Boot" } ]
}
```
`technologies` alfabetik sıralıdır. `bio`, `githubUrl`, `linkedinUrl` `null` olabilir.

#### `PUT /api/users/me` → 200 `Profil`
```json
{
  "fullName": "Ali Demir", "university": "Test Üniversitesi", "department": "Bilgisayar Mühendisliği",
  "bio": "Merhaba!", "githubUrl": "https://github.com/alidemir", "linkedinUrl": ""
}
```
Boş ya da eksik `bio` / `githubUrl` / `linkedinUrl` **silinir** (`null` olur). **400** alan kuralları (8. bölüm).

#### `PUT /api/users/me/technologies` → 200 `Profil`
`{ "technologies": ["Java", "Spring Boot", "Qdrant"] }` — Mevcut listeyi **tamamen değiştirir**. Listede olmayan isimler yeni teknoloji olarak eklenir.
**400:** 20'den fazla, ya da geçersiz isim (`fieldErrors["technologies[]"]`).

#### `GET /api/technologies?query=spr` → 200 `Teknoloji[]`
`[ { "id": "...", "name": "Spring Boot" } ]`. `query` boş/eksikse **tüm teknolojiler** (alfabetik), doluysa isminde geçen **en fazla 10** teknoloji.
(Bu arama büyük/küçük harfe duyarsızdır; Türkçe harf eşlemesi ilan aramasındaki kadar kapsamlı değildir.)

### 6.4 İlan endpoint'leri

#### `GET /api/postings` → 200 `Sayfa<İlanKartı>`
Parametreler 5.6'daki tabloda. Örnek: `GET /api/postings?type=PROJECT&technologies=Java&technologies=Docker&hasVacancy=true&sort=DEADLINE&page=0&size=12`

Yalnızca **açık** ilanlar gelir. Hatalı `sort`/`type`/`hasVacancy` değeri → **400** (`'sort' parametresi geçersiz.`).

**İlan kartı (`İlanKartı`):**
```json
{
  "id": "0f98e878-2941-4e02-af53-5d0432385962",
  "type": "PROJECT",
  "title": "Robotik Kol Projesi",
  "shortDescription": "Üniversite laboratuvarı için küçük bir robotik kol geliştiriyoruz.",
  "organizationName": null,
  "open": true,
  "deadline": "2026-12-25T20:59:59Z",
  "createdAt": "2026-10-17T17:17:05.324780Z",
  "owner": { "id": "c7dbe98b-...", "fullName": "Ayşe Yılmaz", "university": "Test Üniversitesi", "department": "Bilgisayar Mühendisliği" },
  "roles": [
    { "id": "77789299-...", "role": "Gömülü Yazılım", "requiredCount": 1, "acceptedCount": 0, "remainingCount": 1 }
  ],
  "totalRemaining": 1,
  "technologies": [ { "id": "558c262e-...", "name": "Arduino" }, { "id": "3dd78c90-...", "name": "Java" } ]
}
```
Kartta **`stage`, `benefitsNote` ve tam `description` yoktur**; bunlar detay sayfasındadır.
Roller ve teknolojiler alfabetik sıralıdır.

#### `GET /api/postings/{id}` → 200 `İlan`
```json
{
  "id": "0f98e878-...",
  "type": "STARTUP",
  "title": "Öğrenci Odaklı Staj Platformu",
  "description": "Öğrencileri staj ilanlarıyla eşleştiren bir girişim kuruyoruz...",
  "organizationName": null,
  "stage": "IDEA",
  "benefitsNote": "Hisse ve portfolyo",
  "status": "OPEN",
  "open": true,
  "deadline": "2026-11-30T20:59:59Z",
  "createdAt": "2026-10-17T17:00:00Z",
  "owner": { "id": "...", "fullName": "Ayşe Yılmaz", "university": "Test Üniversitesi", "department": "Bilgisayar Mühendisliği" },
  "roles": [ { "id": "...", "role": "Backend", "requiredCount": 2, "acceptedCount": 1, "remainingCount": 1 } ],
  "technologies": [ { "id": "...", "name": "Java" } ]
}
```
**Hatalar:** 404 `İlan bulunamadı.` (olmayan id ya da **iptal edilmiş ilan, sahibi değilsen**).
Kapalı ilan hâlâ açılır (`open: false`). İptal edilmiş ilanı sahibi `status: "CANCELLED"` ile görür.

#### `POST /api/postings` → 201 `İlan`
```json
{
  "type": "STARTUP",
  "title": "Öğrenci Odaklı Staj Platformu",
  "description": "Fikir aşamasındayız, ekip arıyoruz.",
  "organizationName": null,
  "stage": "IDEA",
  "benefitsNote": "Hisse ve portfolyo",
  "deadline": "2026-11-30T20:59:59Z",
  "roles": [ { "role": "Backend", "requiredCount": 2 }, { "role": "UI/UX", "requiredCount": 1 } ],
  "technologies": ["Java", "Spring Boot", "Figma"]
}
```
Sahibi, token'daki kullanıcıdır (gövdede sahip alanı yok). **400** alan hataları ve kurallar (5.8, 8. bölüm).

#### `PUT /api/postings/{id}` → 200 `İlan`
```json
{
  "title": "Güncel başlık",
  "description": "Güncel açıklama",
  "organizationName": "Teknofest",
  "stage": null,
  "benefitsNote": null,
  "deadline": "2026-12-20T20:59:59Z",
  "technologies": ["Python", "Git"]
}
```
`type` ve `roles` **gönderilmez ve değişmez**. Tüm alanlar zorunlu bir "tam değiştirme"dir (5.8).
**Hatalar:** 400 (alan hataları, `Aşama bilgisi sadece girişim ilanlarında kullanılabilir.`, `İptal edilmiş ilan düzenlenemez.`), 403 (`Bu ilan üzerinde işlem yapma yetkiniz yok.`), 404.
Kapanmış ilanın son tarihini ileri alırsan ilan yeniden **açılır**.

#### `DELETE /api/postings/{id}` → 204
İlanı **iptal eder** (silmez; durumu `CANCELLED` olur). Geri alınamaz. Sahibi tekrar çağırırsa yine 204.
**Hatalar:** 403 (sahibi değil), 404. İptal edilen ilan akıştan, kaydedilenlerden ve gelen başvuru rozetinden kalkar; başvuranlar "Başvurulan" sayfasında ilanın iptal edildiğini görür.

### 6.5 Başvuru endpoint'leri

#### `POST /api/postings/{postingId}/applications` → 201 `Başvuru`
```json
{ "message": "Backend rolüne başvuruyorum.", "roleIds": ["<rol-id-1>", "<rol-id-2>"] }
```
`message` isteğe bağlı. `roleIds` 1-5 arası, **o ilana ait** rol kimlikleri.

**201 yanıtı `Başvuru`:**
```json
{
  "id": "5e6f...",
  "status": "PENDING",
  "message": "Backend rolüne başvuruyorum.",
  "createdAt": "2026-10-18T12:00:00Z",
  "decidedAt": null,
  "posting": { "id": "0f98e878-...", "title": "Robotik Kol Projesi", "type": "PROJECT", "status": "OPEN", "open": true, "deadline": "2026-12-25T20:59:59Z" },
  "appliedRoles": [ { "id": "<rol-id-1>", "role": "Backend" } ],
  "acceptedRole": null
}
```
**Hatalar (sırayla kontrol edilir):** 404 (ilan yok ya da iptal), 400 `Kendi ilanınıza başvuramazsınız.`, 400 `Bu ilan artık başvuru kabul etmiyor.`,
400 `Seçilen roller bu ilana ait değil.`, 409 `Bu ilana zaten başvurdunuz.`, 409 `Başvurunuz reddedildiği için bu ilana tekrar başvuramazsınız.`.
Geri çekilmiş başvuru varsa **yeniden açılır** (aynı `id`, `PENDING`), yine 201.

#### `GET /api/applications/mine?page=0&size=20` → 200 `Sayfa<Başvuru>`
Başvuranın kendi başvuruları, **son güncellenen üstte**. Yukarıdaki `Başvuru` biçimi. İlan kapanmış ya da iptal edilmiş olsa da başvuru listede kalır.

#### `GET /api/postings/{postingId}/applications?status=&page=0&size=20` → 200 `Sayfa<GelenBaşvuru>`
Sadece ilanın **sahibi** çağırabilir. `status` isteğe bağlı filtre (`PENDING` / `ACCEPTED` / `REJECTED` / `WITHDRAWN`).

**`GelenBaşvuru`:**
```json
{
  "id": "5e6f...",
  "status": "PENDING",
  "message": "Backend rolüne başvuruyorum.",
  "createdAt": "2026-10-18T12:00:00Z",
  "decidedAt": null,
  "applicant": {
    "userId": "d8fe781e-...",
    "fullName": "Ali Demir",
    "university": "Test Üniversitesi",
    "department": "Bilgisayar Mühendisliği",
    "bio": "Backend geliştirmeyi seviyorum.",
    "githubUrl": "https://github.com/alidemir",
    "linkedinUrl": "https://www.linkedin.com/in/alidemir",
    "technologies": [ { "id": "...", "name": "Docker" }, { "id": "...", "name": "Java" } ],
    "email": null
  },
  "appliedRoles": [ { "id": "...", "role": "Backend" } ],
  "acceptedRole": null
}
```
`applicant.email` yalnızca `status === "ACCEPTED"` iken dolu, **diğer tüm durumlarda `null`**.
**Hatalar:** 403 `Bu ilan üzerinde işlem yapma yetkiniz yok.`, 404 (ilan yok ya da başkası için iptal edilmiş), 400 (`status` geçersiz).

#### `GET /api/applications/incoming/pending-count` → 200
`{ "pendingCount": 3 }`. İlan sahibinin, iptal edilmemiş ilanlarındaki bekleyen (`PENDING`) başvurularının toplam sayısı. İlanı olmayan kullanıcı için `0`.

#### `POST /api/applications/{id}/accept` → 200 `GelenBaşvuru`
`{ "roleId": "<basvuranin-basvurdugu-rollerden-biri>" }`. Yanıt: `status: "ACCEPTED"`, `acceptedRole` dolu, `decidedAt` dolu, `applicant.email` dolu.
**Hatalar:** 404 `Başvuru bulunamadı.` (yok ya da senin ilanında değil), 409 `Bu başvuru zaten sonuçlandırılmış.`,
400 `Seçilen rol, başvuranın başvurduğu roller arasında değil.`, 409 `Bu rolün kontenjanı dolu.`, 400 `İptal edilmiş ilandaki başvurular sonuçlandırılamaz.`, 400 (`roleId` eksik, `fieldErrors.roleId`).

#### `POST /api/applications/{id}/reject` → 200 `GelenBaşvuru`
Gövdesiz. `status: "REJECTED"`, `acceptedRole: null`, `applicant.email: null`. Hataları `accept` ile aynı (rol/kontenjan hataları hariç).

#### `POST /api/applications/{id}/withdraw` → 200 `Başvuru`
Gövdesiz. Sadece **başvuran** çağırabilir. Yanıt: `status: "WITHDRAWN"`, `acceptedRole: null`.
**Hatalar:** 404 `Başvuru bulunamadı.` (yok ya da senin başvurun değil), 409 `Bu başvuru geri çekilemez.` (reddedilmiş ya da zaten geri çekilmiş).

### 6.6 Kaydedilenler endpoint'leri

- `PUT /api/saved-postings/{postingId}` → **204**. Zaten kayıtlıysa yine 204. İptal edilmiş ilan için **404** `İlan bulunamadı.`; kapanmış ilan kaydedilebilir.
- `DELETE /api/saved-postings/{postingId}` → **204**. Kayıtlı değilse de 204.
- `GET /api/saved-postings?page=0&size=12` → **200** `Sayfa<İlanKartı>` (anasayfadaki kartın aynısı). En son kaydedilen üstte. İptal edilmiş ilanlar listede görünmez.

### 6.7 TypeScript tipleri (kopyala-yapıştır)

```ts
export type PostingType = "PROJECT" | "COMPETITION" | "STARTUP";
export type PostingStage = "IDEA" | "PROTOTYPE" | "PRODUCT";
export type PostingStatus = "OPEN" | "CLOSED" | "CANCELLED";
export type ApplicationStatus = "PENDING" | "ACCEPTED" | "REJECTED" | "WITHDRAWN";

export interface ApiError {
  timestamp: string; status: number; error: string; message: string; path: string;
  fieldErrors?: Record<string, string>;
}

export interface Page<T> {
  content: T[]; page: number; size: number; totalElements: number; totalPages: number; hasNext: boolean;
}

export interface LoginResponse {
  accessToken: string; refreshToken: string; tokenType: "Bearer"; expiresInSeconds: number;
}

export interface Technology { id: string; name: string }

export interface Profile {
  userId: string; email: string; fullName: string; university: string; department: string;
  bio: string | null; githubUrl: string | null; linkedinUrl: string | null; technologies: Technology[];
}

export interface PostingOwner { id: string; fullName: string; university: string; department: string }

export interface PostingRole {
  id: string; role: string; requiredCount: number; acceptedCount: number; remainingCount: number;
}

export interface PostingSummary {            // ilan kartı (liste)
  id: string; type: PostingType; title: string; shortDescription: string;
  organizationName: string | null; open: boolean; deadline: string; createdAt: string;
  owner: PostingOwner; roles: PostingRole[]; totalRemaining: number; technologies: Technology[];
}

export interface Posting {                   // ilan detayı
  id: string; type: PostingType; title: string; description: string;
  organizationName: string | null; stage: PostingStage | null; benefitsNote: string | null;
  status: PostingStatus; open: boolean; deadline: string; createdAt: string;
  owner: PostingOwner; roles: PostingRole[]; technologies: Technology[];
}

export interface RoleRef { id: string; role: string }

export interface Application {               // başvuranın gördüğü
  id: string; status: ApplicationStatus; message: string | null; createdAt: string; decidedAt: string | null;
  posting: { id: string; title: string; type: PostingType; status: PostingStatus; open: boolean; deadline: string };
  appliedRoles: RoleRef[]; acceptedRole: RoleRef | null;
}

export interface IncomingApplication {       // ilan sahibinin gördüğü
  id: string; status: ApplicationStatus; message: string | null; createdAt: string; decidedAt: string | null;
  applicant: {
    userId: string; fullName: string; university: string; department: string;
    bio: string | null; githubUrl: string | null; linkedinUrl: string | null;
    technologies: Technology[]; email: string | null;
  };
  appliedRoles: RoleRef[]; acceptedRole: RoleRef | null;
}
```

---

## 7. Durumlar, kurallar ve yetkiler

### 7.1 İlan durumları

| `status` | Anlamı | Akışta görünür mü? | Başvuru alır mı? |
|---|---|---|---|
| `OPEN` | Yayında | Evet (son tarih geçmediyse) | Evet |
| `CLOSED` | Son tarihi geçti, otomatik kapandı | Hayır | Hayır |
| `CANCELLED` | Sahibi iptal etti (geri alınamaz) | Hayır (sahibi hariç kimse göremez) | Hayır |

Geçişler:
- `OPEN → CLOSED`: Son tarih geçince **otomatik** (arka plan işi her dakika çalışır).
- `CLOSED → OPEN`: Sahibi ilanı düzenleyip **son tarihi ileriye alınca**.
- `OPEN / CLOSED → CANCELLED`: Sahibi ilanı iptal edince. Bundan sonra düzenlenemez, başvuru kabul/red edilemez.

> Arayüz için tek gerçek soru: **"Bu ilana şu an başvurulabilir mi?"** → `open === true`.

### 7.2 Başvuru durumları ve geçişler

| `status` | Anlamı | Türkçe etiket önerisi |
|---|---|---|
| `PENDING` | İlan sahibi henüz karar vermedi | Bekliyor |
| `ACCEPTED` | Kabul edildi, bir role yerleştirildi | Kabul edildi |
| `REJECTED` | Reddedildi | Reddedildi |
| `WITHDRAWN` | Başvuran geri çekti | Geri çekildi |

```
PENDING ──(ilan sahibi kabul)──► ACCEPTED ──(başvuran geri çeker)──► WITHDRAWN
   │                                                                      │
   ├──(ilan sahibi reddeder)──► REJECTED  (son durum; yeniden başvuru yok)│
   └──(başvuran geri çeker)───► WITHDRAWN ◄───(yeniden başvurur)── PENDING┘
```

- **Reddedilen** kullanıcı aynı ilana **tekrar başvuramaz**.
- **Geri çeken** kullanıcı tekrar başvurabilir; eski kayıt yeniden `PENDING` olur.
- Kabul edilmiş başvuru geri çekilirse **rol kontenjanı yeniden açılır**.

### 7.3 Kontenjan nasıl hesaplanır?

- Her rolün `requiredCount` değeri vardır (ilan açılırken verilir).
- `acceptedCount` = o role **kabul edilmiş** (`ACCEPTED`) başvuru sayısı. Bekleyen, reddedilen, geri çekilen **sayılmaz**.
- `remainingCount` = `requiredCount - acceptedCount` (asla negatif olmaz).
- Kartlardaki `totalRemaining` = ilanın tüm rollerinin `remainingCount` toplamı.
- Dolu bir role **başvuru yapılabilir** (backend engellemez) ama ilan sahibi o role **kabul edemez** (409). Sahibi başvuranı başka bir (başvurduğu) role yerleştirebilir ya da reddedebilir.
- Aynı anda iki kişi son kontenjanı kabul edilmeye çalışılsa bile **kontenjan aşılmaz**; backend bunu kilitlerle garanti eder. İkinci istek 409 alır.

### 7.4 Kim ne yapabilir?

| İşlem | Kim yapabilir | Yetkisiz denerse |
|---|---|---|
| İlanları görmek (liste, detay) | Giriş yapmış herkes | 401 |
| İptal edilmiş ilanı görmek | Sadece ilanın sahibi | 404 |
| İlan düzenlemek / iptal etmek | Sadece sahibi | 403 |
| İlana başvurmak | Sahibi **olmayan** herkes (ilan açıksa) | 400 |
| İlana gelen başvuruları görmek | Sadece ilan sahibi | 403 |
| Başvuruyu kabul/red etmek | Sadece ilan sahibi | 404 |
| Başvuruyu geri çekmek | Sadece başvuran | 404 |
| Kaydedilenler | Herkes kendi listesini yönetir | — |

Yetkisiz bir başvuruya karar vermeye çalışmak **403 değil 404** döner (başkasının başvurusunun var olup olmadığı sızmasın diye).

### 7.5 Gizlilik

- Başvuranın **e-postası**, başvuru **kabul edilene kadar** ilan sahibine gösterilmez (`null`).
- Giriş ve şifre unuttum akışları **bir e-postanın kayıtlı olup olmadığını** açığa vurmaz.
- İlanlar ve profiller yalnızca **giriş yapmış** kullanıcılara görünür.

### 7.6 Zamanla olan şeyler

- İlan son tarihi geçince en geç **1 dakika** içinde `CLOSED` olur.
- Doğrulama linki 24 saat, şifre sıfırlama linki 1 saat, `accessToken` 15 dakika, `refreshToken` 7 gün geçerli.
- Süresi dolan token'lar arka planda saatte bir temizlenir (arayüzü ilgilendirmez).

---

## 8. Form doğrulama kuralları (alan sınırları)

Backend bu kuralları **her zaman** uygular; arayüzde aynı kuralları uygularsan kullanıcı hatayı göndermeden önce görür.
(Metin alanlarında baş ve sondaki boşluklar backend tarafından temizlenir.)

| Form | Alan | Kural |
|---|---|---|
| **Kayıt** | `email` | Zorunlu, geçerli e-posta, en fazla 255, **`.edu.tr` ile bitmeli** |
| | `password` | Zorunlu, **8-72 karakter** |
| | `fullName` | Zorunlu, en fazla 100 |
| | `university`, `department` | Zorunlu, en fazla 150 |
| **Giriş** | `email`, `password` | Zorunlu; `email` geçerli biçimde |
| **Şifre sıfırlama** | `token` | Zorunlu |
| | `newPassword` | Zorunlu, **8-72 karakter** |
| **Profil** | `fullName` | Zorunlu, en fazla 100 |
| | `university`, `department` | Zorunlu, en fazla 150 |
| | `bio` | Opsiyonel, en fazla 1000 |
| | `githubUrl` | Opsiyonel, `https://github.com/...` ile başlamalı, en fazla 255 |
| | `linkedinUrl` | Opsiyonel, `https://linkedin.com/...` ya da `https://www.linkedin.com/...` ile başlamalı, en fazla 255 |
| **Teknoloji listesi** (profil ve ilan) | liste | En fazla **20** eleman |
| | her isim | 2-50 karakter; harf, rakam, boşluk ve `. + # / & -` karakterleri; ilk karakter harf, rakam ya da nokta (`C++`, `C#`, `.NET`, `Node.js`, `HTML/CSS` geçerli; `<script>` geçersiz) |
| **İlan** | `type` | Zorunlu: `PROJECT` / `COMPETITION` / `STARTUP` |
| | `title` | Zorunlu, en fazla 150 |
| | `description` | Zorunlu, en fazla 5000 |
| | `organizationName` | Opsiyonel, en fazla 150 |
| | `stage` | Opsiyonel; **sadece `STARTUP`** için; `IDEA` / `PROTOTYPE` / `PRODUCT` |
| | `benefitsNote` | Opsiyonel, en fazla 500 |
| | `deadline` | Zorunlu, **gelecekte**, en fazla **1 yıl** sonrası |
| | `roles` | Zorunlu (oluştururken), **1-10** eleman; aynı rol adı iki kez yazılamaz (büyük/küçük harf farkı sayılmaz) |
| | `roles[].role` | Zorunlu, en fazla 50 |
| | `roles[].requiredCount` | Zorunlu, **1-50** |
| **Başvuru** | `message` | Opsiyonel, en fazla 1000 |
| | `roleIds` | Zorunlu, **1-5** eleman, o ilanın rol kimlikleri |
| **Kabul** | `roleId` | Zorunlu; başvuranın başvurduğu rollerden biri |
| **Arama** | `query`, `role`, `department`, `university` | En fazla 100 karakter (fazlası kesilir) |

---

## 9. React için öneriler ve hazır kod

Bu bölümdeki kod **öneridir**, zorunlu değildir; ama 4. bölümdeki token mantığını doğru kurmak için kritik parçalar burada.
Örnekler TypeScript'tir (`6.7`'deki tipleri `src/api/types.ts` dosyasına koyduğunu varsayar). JavaScript kullanıyorsan tip kısımlarını sil.

### 9.1 Önerilen araçlar

| Amaç | Öneri |
|---|---|
| HTTP istemcisi | `axios` (aşağıdaki kod axios içindir) |
| Sayfa yönlendirme | `react-router-dom` |
| Sunucu verisi (liste, detay, önbellek, yeniden deneme) | `@tanstack/react-query` |
| Formlar | `react-hook-form` (+ isteğe bağlı `zod`) |
| Global kullanıcı bilgisi | React Context (ya da Zustand) |

### 9.2 Token saklama

```ts
// src/api/tokens.ts
const ACCESS = "unilink.access";
const REFRESH = "unilink.refresh";

export const tokens = {
  getAccess: () => localStorage.getItem(ACCESS),
  getRefresh: () => localStorage.getItem(REFRESH),
  set: (access: string, refresh: string) => {
    localStorage.setItem(ACCESS, access);
    localStorage.setItem(REFRESH, refresh);
  },
  clear: () => {
    localStorage.removeItem(ACCESS);
    localStorage.removeItem(REFRESH);
  },
};
```

### 9.3 Axios istemcisi: token ekleme ve otomatik yenileme

Bu dosya, 4.4'te anlatılan davranışın tamamını yapar: her isteğe token ekler, 401'de **bir kere** yenileyip isteği tekrarlar,
aynı anda gelen birden fazla 401'de **tek bir yenileme** yapar, dizi parametrelerini doğru biçimde gönderir.

```ts
// src/api/client.ts
import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";
import { tokens } from "./tokens";
import type { LoginResponse } from "./types";

// Vite: import.meta.env.VITE_API_URL   |   Create React App: process.env.REACT_APP_API_URL
const baseURL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export const api = axios.create({
  baseURL,
  headers: { "Content-Type": "application/json" },
  // ÖNEMLİ: technologies=Java&technologies=React biçimi için (varsayılan technologies[]=Java backend'de çalışmaz)
  paramsSerializer: { indexes: null },
});

api.interceptors.request.use((config) => {
  const access = tokens.getAccess();
  if (access) config.headers.Authorization = `Bearer ${access}`;
  return config;
});

let refreshPromise: Promise<void> | null = null;

async function refreshTokens(): Promise<void> {
  const refreshToken = tokens.getRefresh();
  if (!refreshToken) throw new Error("Refresh token yok");
  // Burada `api` değil düz `axios` kullanılıyor: yenileme isteği kendi interceptor'una takılmasın.
  const { data } = await axios.post<LoginResponse>(`${baseURL}/api/auth/refresh`, { refreshToken });
  tokens.set(data.accessToken, data.refreshToken); // yeni refresh token'ı KAYDETMEK şart
}

type RetryConfig = InternalAxiosRequestConfig & { _retry?: boolean };

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetryConfig | undefined;
    const isAuthEndpoint = original?.url?.startsWith("/api/auth/");

    if (error.response?.status === 401 && original && !original._retry && !isAuthEndpoint && tokens.getRefresh()) {
      original._retry = true;
      try {
        // Devam eden bir yenileme varsa onu bekle; yoksa yenisini başlat.
        refreshPromise ??= refreshTokens().finally(() => { refreshPromise = null; });
        await refreshPromise;
        return api(original); // aynı isteği yeni token'la tekrarla
      } catch {
        tokens.clear();
        window.location.assign("/login");
      }
    }
    return Promise.reject(error);
  }
);
```

### 9.4 Hata okuma yardımcısı

```ts
// src/api/errors.ts
import axios from "axios";
import type { ApiError } from "./types";

export function parseError(error: unknown) {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as Partial<ApiError> | undefined;
    return {
      status: error.response?.status ?? 0,           // 0 = sunucuya hiç ulaşılamadı
      message: data?.message ?? (error.response ? "Beklenmeyen bir hata oluştu." : "Sunucuya ulaşılamadı."),
      fieldErrors: data?.fieldErrors ?? {},          // { "githubUrl": "..." }
    };
  }
  return { status: 0, message: "Beklenmeyen bir hata oluştu.", fieldErrors: {} as Record<string, string> };
}
```

Form hatasını alanlara dağıtma (react-hook-form ile):

```ts
try {
  await updateProfile(values);
} catch (e) {
  const { message, fieldErrors } = parseError(e);
  Object.entries(fieldErrors).forEach(([field, msg]) => setError(field as any, { message: msg }));
  if (Object.keys(fieldErrors).length === 0) setFormError(message); // genel hata
}
```

### 9.5 API fonksiyonları (örnekler)

```ts
// src/api/postings.ts
import { api } from "./client";
import type { Application, Page, Posting, PostingSummary, PostingType } from "./types";

export interface FeedParams {
  page?: number; size?: number; sort?: "NEWEST" | "DEADLINE";
  type?: PostingType; query?: string; technologies?: string[];
  role?: string; department?: string; university?: string; hasVacancy?: boolean;
}

// Boş metinleri ayıkla ki adrese `query=` gibi gereksiz parametreler eklenmesin
const clean = <T extends object>(obj: T) =>
  Object.fromEntries(Object.entries(obj).filter(([, v]) => v !== "" && v !== undefined && v !== null)) as T;

export const getFeed = (params: FeedParams) =>
  api.get<Page<PostingSummary>>("/api/postings", { params: clean(params) }).then((r) => r.data);

export const getPosting = (id: string) =>
  api.get<Posting>(`/api/postings/${id}`).then((r) => r.data);

export const applyToPosting = (postingId: string, body: { message?: string; roleIds: string[] }) =>
  api.post<Application>(`/api/postings/${postingId}/applications`, body).then((r) => r.data);

export const acceptApplication = (applicationId: string, roleId: string) =>
  api.post(`/api/applications/${applicationId}/accept`, { roleId }).then((r) => r.data);

export const rejectApplication = (applicationId: string) =>
  api.post(`/api/applications/${applicationId}/reject`).then((r) => r.data);

export const withdrawApplication = (applicationId: string) =>
  api.post<Application>(`/api/applications/${applicationId}/withdraw`).then((r) => r.data);

export const saveCall = (postingId: string) => api.put(`/api/saved-postings/${postingId}`);
export const unsaveCall = (postingId: string) => api.delete(`/api/saved-postings/${postingId}`);
export const getPendingCount = () =>
  api.get<{ pendingCount: number }>("/api/applications/incoming/pending-count").then((r) => r.data.pendingCount);
```

### 9.6 Giriş durumu (Context) ve korumalı sayfalar

```tsx
// src/auth/AuthContext.tsx (özet)
const AuthContext = createContext<{ me: Profile | null; loading: boolean; login: ...; logout: ... } | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [me, setMe] = useState<Profile | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!tokens.getAccess()) { setLoading(false); return; }
    api.get<Profile>("/api/users/me")
      .then((r) => setMe(r.data))
      .catch(() => tokens.clear())          // 401 + yenileme de başarısızsa buraya düşer
      .finally(() => setLoading(false));
  }, []);

  // login: POST /api/auth/login → tokens.set(...) → GET /api/users/me → setMe
  // logout: POST /api/auth/logout (hata verse bile devam) → tokens.clear() → setMe(null)
  ...
}

// Korumalı sayfa sarmalayıcısı
export function RequireAuth({ children }: { children: ReactElement }) {
  const { me, loading } = useAuth();
  if (loading) return <Spinner />;
  return me ? children : <Navigate to="/login" replace />;
}
```

### 9.7 React Query ile liste ve işlem örneği

```tsx
// Anasayfa: filtre değişince yeniden çeker, yeni veri gelene kadar eskisini gösterir (titreme olmaz)
const feed = useQuery({
  queryKey: ["feed", filters],
  queryFn: () => getFeed(filters),
  placeholderData: keepPreviousData,
});

// Başvuru yapma: başarılı olunca ilgili önbellekleri tazele
const apply = useMutation({
  mutationFn: (body: { message?: string; roleIds: string[] }) => applyToPosting(postingId, body),
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: ["my-applications"] });
    queryClient.invalidateQueries({ queryKey: ["posting", postingId] });
  },
});

// Kabul/red sonrası: gelen başvurular listesi ve rozet
onSuccess: () => {
  queryClient.invalidateQueries({ queryKey: ["incoming", postingId] });
  queryClient.invalidateQueries({ queryKey: ["pending-count"] });
}
```

Rozeti 60 saniyede bir tazelemek için: `useQuery({ queryKey: ["pending-count"], queryFn: getPendingCount, refetchInterval: 60_000 })`.

### 9.8 Doğrulama sayfasında `StrictMode` tuzağı

```tsx
// /verify-email
const [params] = useSearchParams();
const started = useRef(false);

useEffect(() => {
  if (started.current) return;      // geliştirme modunda efekt iki kez çalışır; isteği tek sefer at
  started.current = true;
  const token = params.get("token");
  if (!token) { setState("invalid"); return; }
  api.post("/api/auth/verify-email", { token })
    .then(() => setState("ok"))
    .catch(() => setState("invalid"));
}, []);
```

### 9.9 Arama kutusu için gecikme (debounce)

```ts
export function useDebounce<T>(value: T, delay = 400) {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return debounced;
}
// const query = useDebounce(searchText);  → filters.query olarak kullan
```

### 9.10 Etiket sözlükleri ve tarih yardımcıları

```ts
export const postingTypeLabel: Record<PostingType, string> = {
  PROJECT: "Proje", COMPETITION: "Yarışma", STARTUP: "Girişim",
};
export const stageLabel: Record<PostingStage, string> = {
  IDEA: "Fikir", PROTOTYPE: "Prototip", PRODUCT: "Ürün",
};
export const applicationStatusLabel: Record<ApplicationStatus, string> = {
  PENDING: "Bekliyor", ACCEPTED: "Kabul edildi", REJECTED: "Reddedildi", WITHDRAWN: "Geri çekildi",
};

// Gösterim: UTC → yerel saat
export const formatDateTime = (iso: string) => new Date(iso).toLocaleString("tr-TR");
export const formatDate = (iso: string) => new Date(iso).toLocaleDateString("tr-TR");

// Gönderim: <input type="datetime-local"> değeri → ISO UTC
export const toApiDate = (localValue: string) => new Date(localValue).toISOString();
```

### 9.11 Swagger ile denemek

Swagger sayfasında (`/swagger-ui.html`) sağ üstteki **Authorize** düğmesine bas, `accessToken` değerini yapıştır
(başına `Bearer` **yazma**, kendisi ekler). Böylece korumalı endpoint'leri arayüzü yazmadan deneyebilirsin.
Token 15 dakikada dolar; tekrar login olup yenisini yapıştır.

---

## 10. Sık sorulan sorular ve tuzaklar

| Sorun | Sebep ve çözüm |
|---|---|
| **Her istek 401 dönüyor** | `Authorization: Bearer <token>` başlığı eklenmemiş ya da `accessToken` süresi (15 dk) dolmuş. 9.3'teki interceptor'ı kur. |
| **Tarayıcıda "CORS" hatası** | Frontend'i izin verilen portlardan birinde çalıştır (3000 ya da 5173) ya da bana haber ver, eklerim. Hata aslında backend'in kapalı olmasından da kaynaklanabilir. |
| **Teknoloji filtresi hiçbir şeyi filtrelemiyor** | Axios dizi parametresini `technologies[]=...` biçiminde gönderiyor. `paramsSerializer: { indexes: null }` ekle (9.3). |
| **E-posta doğrulama sayfası ilk açılışta "geçersiz" diyor** | `StrictMode` isteği iki kez atıyor, ikincisi 400 alıyor. 9.8'deki `useRef` korumasını kullan. |
| **Bir süre sonra sebepsiz çıkış yapılıyor** | Yenileme yanıtındaki **yeni `refreshToken` kaydedilmemiş**, ya da aynı anda birden fazla yenileme başlamış. 9.3'ü olduğu gibi kullan. |
| **Profili güncelleyince bazı alanlar boşaldı** | `PUT` **tam değiştirme**dir. Göndermediğin isteğe bağlı alan silinir. Formu mevcut verilerle doldurup tümünü gönder. |
| **İlan düzenleyince teknolojiler gitti** | Aynı sebep: `technologies` göndermezsen ilanın teknolojileri boşalır. |
| **`GET /api/postings/{id}` 404 veriyor ama ilan vardı** | İlan **iptal edilmiş** ve sen sahibi değilsin. Bu bilinçli bir davranış. |
| **Bir sayfa gelmiyor / boş geliyor** | Sayfa numarası **0'dan** başlar. `page=1` ikinci sayfadır. |
| **Saatler 3 saat kayık** | Backend UTC döndürür. Gösterirken `toLocaleString("tr-TR")` ile yerel saate çevir. |
| **`applicant.email` hep `null`** | Başvuru henüz **kabul edilmemiş**. Bu gizlilik kuralı (7.5). |
| **Kendi ilanımda "Başvur" düğmesi çıkıyor** | `owner.id === me.userId` kontrolünü yap ve düğmeyi gizle. Backend zaten 400 verir ama kullanıcıya hiç gösterme. |
| **Dolu role başvuru yapabiliyorum** | Doğru: backend dolu role başvuruyu engellemez, sadece **kabulde** engeller. Dolu rolü seçtirmemek arayüz tasarım kararı. |
| **Hata mesajı gelmiyor (401)** | `401` yanıtının gövdesi boştur. Durum koduna göre kendi mesajını göster. |
| **Aynı anda iki sekme açık, biri çıkış yaptı** | Refresh token tek kullanımlık olduğu için iki sekme **aynı anda** yenilerse biri başarısız olup çıkış yapabilir. Önemli bir sorun değil; ileride `storage` olayıyla sekmeler arası senkron eklenebilir. |
| **Bir hata mesajı İngilizce geldi** | Doğrulama hatalarının metni (`fieldErrors`) Türkçe'dir; bilinmeyen adres (404) ve yanlış metot (405) gibi framework hatalarının gövdesi İngilizce olabilir. Durum koduna göre kendi Türkçe mesajını göster. |

**Genel öneriler:**
- Her istekte yükleniyor (`loading`) ve hata durumlarını ele al. Ağ hatası (`status === 0`) için "Sunucuya ulaşılamadı" göster.
- Aynı butona iki kez basılmasını engelle (istek sürerken düğmeyi pasif yap).
- Silme/iptal/geri çekme gibi geri alınması zor işlemlerde **onay penceresi** göster.
- Başarılı işlemden sonra ilgili listeyi yenile (kabul sonrası gelen başvurular ve rozet, başvuru sonrası başvurularım).

---

## 11. Henüz hazır olmayanlar

Aşağıdakiler için **endpoint yok**; arayüzde şimdilik gösterme ya da "yakında" olarak işaretle.

| Özellik | Durum |
|---|---|
| **Mesajlaşma** (menüdeki "Mesaj") | Planlı, henüz yok |
| **Bildirimler / anlık güncelleme** | Yok. Başvuru rozetini yenilemek için periyodik istek at (5.5) |
| **"İlanlarım" listesi** (kendi açtığım ilanlar) | Planlı (`GET /api/users/me/postings`). Şimdilik kullanıcının kendi ilanlarına anasayfa filtresiyle ulaşma yolu yok; ilan oluşturunca yönlendirilen detay sayfası bir giriş noktasıdır |
| **İlan yanıtında `saved` bayrağı** (kalp ikonunun durumu) | **Yakında.** Şimdilik `GET /api/saved-postings` ile id listesi çekip karşılaştır (5.7) |
| **İlan detayında "bu ilana başvurdum mu"** | Alan yok; `GET /api/applications/mine` ile bul (5.7) |
| **Başka kullanıcının profil sayfası** | Yok (sadece ilan sahibi kartı ve gelen başvurulardaki başvuran bilgisi) |
| **Profilde projeler, CV bağlantısı** | Planlı (önce CV bağlantısı ve "Projeler" bölümü), henüz yok |
| **Şifre değiştirme (giriş yapmışken), hesap silme, e-posta değiştirme** ("Settings") | Yok |
| **İlandaki rolleri düzenleme** | Yok (ilan açıldıktan sonra roller değişmez) |
| **Yönetici paneli** (kullanıcı/ilan yönetimi) | Planlı |
| **Üniversite ve bölüm listesi** | Serbest metin; filtreler "içerir" mantığıyla çalışır. İleride seçilebilir liste düşünülebilir |
| **İlan şikayet etme** | Planlı |

Yeni endpoint'ler eklendikçe Swagger kendiliğinden güncellenir ve bu belge de yenilenir. Bir uca ihtiyacın olursa **söyle**: sıralamayı sana göre ayarlarız.

---

## 12. Sözlük

| Terim | Anlamı |
|---|---|
| **Endpoint** | Backend'in bir işi yaptığı adres (örn. `POST /api/postings`). |
| **İstek / yanıt (request / response)** | Frontend'in gönderdiği / backend'in geri döndürdüğü veri. |
| **Gövde (body)** | İsteğin ya da yanıtın içindeki JSON verisi. |
| **Durum kodu (status code)** | Yanıtın sonucunu söyleyen sayı: 200 başarılı, 400 hatalı istek, 401 giriş gerekli, 403 yetkisiz, 404 yok, 409 çakışma. |
| **Token** | Kullanıcının kim olduğunu kanıtlayan, backend'in ürettiği uzun metin. |
| **Access token** | 15 dakikalık "giriş kartı"; her istekte gönderilir. |
| **Refresh token** | 7 günlük, sadece yeni access token almak için kullanılan token; tek kullanımlıktır. |
| **Bearer** | Token'ı `Authorization` başlığında taşıma biçimi: `Authorization: Bearer <token>`. |
| **JWT** | Access token'ın biçimi (içine kullanıcı kimliği yazılmış imzalı metin). Arayüzün içini okuması gerekmez. |
| **CORS** | Tarayıcının, bir sitenin başka bir adresteki API'ye istek atmasına izin verip vermeyeceğini kontrol eden kuralı. |
| **UUID** | Her kaydın benzersiz kimliği (`0f98e878-2941-...`). |
| **Sayfalama (pagination)** | Uzun listeyi parça parça (sayfa sayfa) çekme. |
| **Debounce** | Kullanıcı yazmayı bırakana kadar beklemek (her tuşta istek atmamak için). |
| **Idempotent (tekrar çağrılabilir)** | Aynı isteği birden çok kez atmanın sonucu değiştirmemesi (kaydet, kaydı kaldır, çıkış gibi). |
| **İlan (posting)** | Ekip arayan kişinin yayınladığı kayıt. |
| **Başvuru (application)** | Bir kullanıcının bir ilana yaptığı istek. |
| **Rol (role)** | İlandaki aranan pozisyon (Backend, UI/UX...). |
| **Kontenjan (vacancy)** | Bir rol için aranan kişi sayısı; kabul edilenlerle dolar. |
| **Kabul / red** | İlan sahibinin başvuru hakkındaki kararı. |
| **Geri çekme (withdraw)** | Başvuranın kendi başvurusunu iptal etmesi. |
| **Kaydedilen (saved)** | Kullanıcının sonra bakmak üzere işaretlediği ilan. |
| **İptal (cancel)** | İlan sahibinin ilanı yayından kaldırması (geri alınamaz). |

---

### Yardım isterken

Bir sorun yaşarsan şunları paylaş, çok daha hızlı çözeriz:
1. Hangi sayfada ve hangi işlemi yaparken oldu?
2. İsteğin adresi, metodu ve gövdesi (tarayıcıda **Network** sekmesinden bakabilirsin).
3. Yanıtın **durum kodu ve gövdesi**.
4. Mümkünse Swagger'da aynı isteği denediğinde ne olduğu.

*Belgenin yazıldığı tarih: 19 Ekim 2026. Backend'in o günkü hâlini anlatır.*