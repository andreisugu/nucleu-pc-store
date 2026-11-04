# Ghid de prezentare — Raportarea I

**Durată țintă:** 7–8 minute de demonstrație + 2–3 minute întrebări.
**Mesajul central:** un catalog funcțional, cu roluri separate și administrare reală, construit pe un API testat — nu doar o pagină statică.

---

## 1. Ce trebuie să se vadă (mapare pe cerințe)

| Cerință | Unde o arăți | Timp |
|---|---|---|
| Autentificare Admin și Client | Înregistrare client, apoi login Admin | ~2 min |
| Adăugare / editare / ștergere produs (Admin) | Panoul **Administrare** | ~3 min |
| Produs cu toate câmpurile | Formularul de produs | inclus |
| Răsfoire produse și categorii | Catalogul + selectorul de categorii | ~1 min |
| Căutare produse | Câmpul de căutare + filtrare | ~1 min |
| API documentat și testat | Swagger UI + rezultatul testelor | ~1 min |

---

## 2. Pregătire (cu 10 minute înainte)

1. Pornește stack-ul:

   ```bash
   cd nucleu-pc-store
   ./porneste.sh
   ```

   Scriptul pornește și baza dacă e oprită, compilează doar dacă e nevoie și deschide browserul.

2. Deschide din timp aceste tab-uri:
   - aplicația: `http://localhost:5173`
   - Swagger: `http://localhost:8080/swagger-ui/index.html`
   - opțional, contractul brut: `http://localhost:8080/v3/api-docs`

3. Verifică rapid:
   - catalogul afișează 5 produse și 5 categorii;
   - butonul **Intră în cont** e vizibil;
   - Zoom browser la 110% pentru proiector.

4. Ține la îndemână datele pentru produsul demonstrativ:

   | Câmp | Valoare |
   |---|---|
   | Nume | `Corsair RM850x` |
   | Descriere | `Sursă modulară pentru sisteme gaming` |
   | Specificații | `850 W, 80+ Gold, modulară` |
   | Preț | `649.99` |
   | Stoc | `6` |
   | Furnizor | `Corsair` |
   | Livrare | `Curier` |
   | Categorie | `Surse` (o creezi în demonstrație) |

5. Credențiale Admin: `admin@nucleu.local` / `AdminDemo12345!`.

> Dacă vrei catalog curat pentru prezentare, șterge produsele demo din **Administrare** înainte. Dacă vrei un catalog bogat, păstrează-le — cele 5 produse acoperă toate categoriile.

---

## 3. Scenariul de prezentare

### 0:00–0:45 — Introducere

Spune pe scurt:

> „Este un magazin demonstrativ de componente PC. Backend-ul este un API REST în Spring Boot cu PostgreSQL, iar interfața este o aplicație React. Există două roluri: clientul vede și caută produse, iar administratorul gestionează catalogul.”

Nu deschide codul încă. Arată aplicația și lasă vizualul să vorbească.

### 0:45–1:45 — Catalog public, categorii și căutare

1. Derulează catalogul: „Produsele vin din API, nu sunt hardcodate.”
2. Alege o categorie din selector: „Filtrarea se face pe server, după identificatorul categoriei.”
3. Revino la **Toate categoriile** și caută `ryzen`: „Căutarea este case-insensitive și acoperă numele, descrierea și specificațiile.”
4. Șterge căutarea.

**De spus:** catalogul este public; nu ai nevoie de cont pentru răsfoire.

### 1:45–3:00 — Înregistrare Client

1. Click **Intră în cont** → **Creează cont**.
2. Folosește un email de test, ex. `client.demo@nucleu.local`, parolă `ClientDemo123!` (minim 12 caractere).
3. **Creează cont** → apare mesajul „Contul a fost creat”.
4. Autentifică-te cu aceleași date.
5. Arată header-ul: apare email-ul și butonul **Ieșire**.
6. Subliniază: **nu apare butonul Administrare**.

**De spus:** „Înregistrarea creează mereu un cont de client. Rolul nu poate fi ales din formular — un câmp `role: ADMIN` trimis direct la API este ignorat.”

### 3:00–3:20 — Login Admin

1. **Ieșire** → **Intră în cont**.
2. `admin@nucleu.local` / `AdminDemo12345!`.
3. Apare butonul **Administrare**.

### 3:20–5:30 — Administrare: categorie nouă + produs nou

1. Click **Administrare**.
2. În zona de categorii scrie `Surse` → **Adaugă categoria**.
   - „Categoria apare imediat în selector și în catalog.”
3. Click **Produs nou** și completează cele 8 câmpuri din tabelul de pregătire.
4. **Salvează produsul**.
5. Produsul apare în catalog, la categoria `Surse`.

**De spus:** „Formularul acoperă exact câmpurile cerute: nume, descriere, specificații, preț, stoc, furnizor, livrare și categorie. Prețul trebuie să fie pozitiv, stocul nenegativ — validarea este pe server, nu doar în interfață.”

### 5:30–6:30 — Editare și ștergere

1. Pe cardul produsului nou: **Editează**.
2. Schimbă prețul în `599.99` și stocul în `4` → **Salvează modificările**.
3. Arată valorile actualizate pe card.
4. **Șterge** → **Confirmă ștergerea** → produsul dispare.
5. Reîncarcă pagina (`Ctrl+R`) după creare/înainte de ștergere, dacă vrei să demonstrezi persistența.

**De spus:** „Ștergerea cere confirmare, ca să nu pierzi un produs din greșeală.”

### 6:30–7:30 — Dovada părții de API

1. Deschide Swagger UI.
2. Arată gruparea endpoint-urilor: **Autentificare**, **Produse**, **Categorii**.
3. Arată butonul **Authorize** și schema `bearerAuth`.
4. Opțional, demonstrează rapid protecția:

   ```bash
   curl -s -o /dev/null -w '%{http_code}\n' -X POST http://localhost:8080/api/categories \
     -H 'Content-Type: application/json' -d '{"name":"Test"}'
   ```

   Rezultat: `401`. Cu token de client: `403`.

5. Menționează testele: **16 teste backend** (API, autentificare, OpenAPI) și **6 teste frontend** (catalog, filtre, stări de eroare, flux Admin).

### 7:30–8:00 — Încheiere

> „Raportarea I este acoperită complet: autentificare cu două roluri, CRUD de produse cu toate câmpurile, categorii și căutare. Urmează favoritele, coșul și checkout-ul simulat.”

---

## 4. Explicații tehnice pentru întrebări

**Arhitectură:** controller → service → repository. Controllerul doar expune API-ul, service-ul ține regulile, repository-ul accesează baza. DTO-urile separă contractul API de entitățile JPA.

**Autentificare:** login → verificare BCrypt → token JWT semnat HMAC, valabil o oră, cu rol în claim. Serverul este stateless; fiecare cerere protejată trimite `Authorization: Bearer <token>`.

**Roluri:** `CLIENT` și `ADMIN`. Înregistrarea publică forțează `CLIENT`. Adminul este creat la pornire din `ADMIN_EMAIL` / `ADMIN_PASSWORD`. Endpoint-urile de modificare cer `ROLE_ADMIN`.

**Parole:** hash BCrypt, niciodată text clar. Limită de 72 bytes UTF-8, respinsă explicit.

**Catalog:** produsul are cheie străină către categorie. API-ul primește numele categoriei pentru lizibilitate, dar baza stochează relația normalizată.

**Căutare:** interogare JPQL cu `lower(...) like`, pe nume, descriere și specificații; filtrare după categorie. Termenul vid întoarce tot catalogul.

**Erori:** `400` validare, `401` fără token, `403` rol insuficient, `404` resursă inexistentă, `409` duplicat sau categorie cu produse.

**Testare:** teste de API cu MockMvc și H2 pentru backend; Vitest + Testing Library pentru interfață. Rulare:

```bash
cd backend && ./mvnw test
cd frontend && npm test
```

---

## 5. Întrebări probabile și răspunsuri scurte

| Întrebare | Răspuns |
|---|---|
| Cum împiedici un client să devină admin? | Înregistrarea ignoră rolul trimis; Adminul există doar din configurare sau promovare controlată. Testul verifică explicit. |
| Ce se întâmplă dacă ștergi o categorie cu produse? | `409 Conflict`; categoria nu poate fi ștearsă cât timp are produse. |
| Unde se validează datele? | Pe server cu Bean Validation; interfața doar ajută utilizatorul. |
| Ce se întâmplă cu token-ul expirat? | Cererea primește `401`; utilizatorul se autentifică din nou. |
| De ce PostgreSQL și H2? | PostgreSQL la rulare; H2 doar în teste, ca suita să ruleze fără servicii externe. |
| Ce nu este încă implementat? | Favorite, coș, checkout și istoric — etapa a doua. |
| Cât de mult ai scris tu? | Entități, DTO-uri, servicii, securitate, teste și interfața; framework-urile oferă infrastructura. |

---

## 6. Plan B

- **Baza nu răspunde:** rulează shortcut-ul **Toggle PostgreSQL (PC Garage)** sau `podman start pcgarage-postgres`.
- **Backend-ul nu pornește:** verifică `/tmp/pcgarage-logs/backend.log`; rulează din nou `./porneste.sh`.
- **Port ocupat:** scriptul te avertizează; oprește procesul vechi sau repornește sesiunea.
- **Frontend-ul nu încarcă:** `Ctrl+Shift+R`; dacă persistă, `cd frontend && npm run dev`.
- **Vrei să sari peste o parte:** ordinea minimă este catalog → login Admin → creare produs → căutare. Restul sunt completări.

---

## 7. Fișă rapidă

```text
Pornire:        ./porneste.sh
Aplicație:      http://localhost:5173
Swagger:        http://localhost:8080/swagger-ui/index.html
Admin:          admin@nucleu.local / AdminDemo12345!
Client test:    client.demo@nucleu.local / ClientDemo123!
Loguri:         /tmp/pcgarage-logs/

Flux: catalog → căutare → înregistrare client → login admin →
      categorie „Surse” → produs nou → editare → ștergere → Swagger
```
