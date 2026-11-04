# Cum explici codul backend-ului

**Ideea de bază:** nu trebuie să explici tot. Alegi **o cerere** și o urmărești prin straturi. Asta acoperă dependency injection, validare, tranzacții, JPA, DTO-uri și securitate — adică tot ce poate întreba un profesor.

Backend-ul are **24 de fișiere și ~850 de linii**, deci încape într-o explicație de 5–7 minute.

---

## 1. Fraza de deschidere

> „Backend-ul e organizat pe trei straturi: controller, service și repository. Controllerul expune API-ul, service-ul ține regulile, repository-ul vorbește cu baza de date. Entitățile JPA nu ies niciodată direct în API — folosesc DTO-uri.”

---

## 2. Fluxul unei cereri (povestea principală)

Alege `POST /api/products` și spune:

```
HTTP POST /api/products
   │
   ├─ 1. Spring Security: are token? are rol ADMIN?         → 401 / 403
   ├─ 2. ProductController: @Valid pe ProductRequest        → 400 dacă datele sunt invalide
   ├─ 3. ProductCatalogService: rezolvă categoria, creează  → regulile de business
   ├─ 4. ProductRepository.save: Hibernate → PostgreSQL     → INSERT în products
   └─ 5. ProductResponse: DTO returnat cu 201 + Location
```

Ce spui la fiecare pas:

1. **Security** — „Filtrul verifică semnătura tokenului și rolul din claim. Fără token → 401; token de client → 403.”
2. **Controller** — „E subțire: doar rute și `@Valid`. Nu are logică de business.”
3. **Service** — „Aici e regula: categoria se caută după nume, produsul se construiește și se salvează. Metoda e tranzacțională.”
4. **Repository** — „Spring Data generează implementarea; JPA generează SQL-ul.”
5. **DTO** — „Returnez `ProductResponse`, nu entitatea, ca să nu cuplez API-ul de schema bazei.”

---

## 3. Fișierele de deschis, în ordine

| # | Fișier | Ce spui într-o frază |
|---|---|---|
| 1 | `catalog/ProductController.java` | „Rutele HTTP și validarea; controller subțire.” |
| 2 | `catalog/ProductRequest.java` | „Contractul de intrare, cu adnotări Bean Validation: preț ≥ 0.01, stoc ≥ 0, lungimi maxime.” |
| 3 | `catalog/ProductCatalogService.java` | „Regulile: rezolvă categoria, creează/actualizează, aruncă 404 dacă produsul lipsește; `@Transactional`.” |
| 4 | `catalog/ProductRepository.java` | „Spring Data pentru CRUD și o interogare JPQL pentru căutare.” |
| 5 | `catalog/Product.java` | „Entitate JPA: `BigDecimal` pentru preț, relație `ManyToOne` lazy către categorie, constructor protejat pentru JPA.” |
| 6 | `catalog/CategoryService.java` | „Nume unic fără diferențiere de majuscule; ștergerea e refuzată cu 409 dacă are produse.” |
| 7 | `security/SecurityConfiguration.java` | „Stateless, CSRF dezactivat, citirea publică, scrierea doar ADMIN; aici se leagă encoder/decoderul JWT.” |
| 8 | `auth/AuthService.java` | „Înregistrarea forțează rolul CLIENT; login-ul compară parola cu hash-ul BCrypt.” |
| 9 | `auth/JwtTokenService.java` | „Token HS256, valabil o oră, cu email ca subject și rol în claim.” |
| 10 | `auth/AdminAccountBootstrap.java` | „Adminul vine din variabile de mediu, nu e hardcodat în cod.” |
| 11 | `resources/application.properties` | „DB și secretul JWT vin din environment; `open-in-view=false`.” |
| 12 | `src/test/java/...` | „Teste de API cu MockMvc și H2; acoperă și securitatea, și documentația OpenAPI.” |

> Nu deschide toate 12. Maxim 5–6, în ordinea fluxului: controller → request → service → repository → entitate → security.

---

## 4. Cinci lucruri care merită subliniate

**1. DTO vs entitate.** `ProductRequest` / `ProductResponse` separă API-ul de baza de date. Poți schimba schema fără să rupi contractul.

**2. Controller subțire, service consistent.** Toate regulile stau în service; controllerul nu știe cum se salvează un produs.

**3. Securitate stateless.** Nu există sesiune; fiecare cerere se autentifică prin token. Rolul e în claim, iar `JwtAuthenticationConverter` îl transformă în autoritate `ROLE_ADMIN`.

**4. Integritate în două locuri.** Categoria are constrângere unică în baza de date și verificare în service. Baza e ultima linie de apărare, nu singura.

**5. Un bug real, găsit pe PostgreSQL.** Căutarea folosea `:query is null`; PostgreSQL tipa parametrul ca `bytea` și `lower(bytea)` nu există. Testele pe H2 treceau. Am reparat cauza: parametrul nu mai e null, ci string vid. Povestea arată că testezi pe tehnologia reală, nu doar pe cea de test.

---

## 5. Întrebări probabile și răspunsuri

| Întrebare | Răspuns |
|---|---|
| Unde e logica de business? | În `*Service`. Controllerul doar traduce HTTP în apeluri de service. |
| De ce `@Transactional` pe service? | O operație = o tranzacție; dacă ceva eșuează, nu rămân date pe jumătate. |
| Cum funcționează căutarea? | JPQL cu `lower(...) like`, pe nume, descriere și specificații, plus filtru opțional pe `category.id`. |
| Ce se întâmplă la nume de categorie duplicat? | Verificare în service → `409`; constrângerea unică din DB rămâne plasa de siguranță. |
| Cum se generează tokenul? | `JwtEncoder` cu HS256, claims: subject=email, `roles`, `iat`, `exp` (o oră). |
| Cum se validează tokenul? | `NimbusJwtDecoder` cu același secret; converterul citește claim-ul `roles` și pune prefixul `ROLE_`. |
| De ce BCrypt? | Hash cu salt și cost; parolele nu sunt reversibile. Limita de 72 bytes e impusă explicit. |
| Ce face `open-in-view=false`? | Nu ține sesiunea JPA deschisă în timpul randării; accesul la date rămâne în service. |
| De ce H2 în teste? | Suita rulează rapid și izolat, fără PostgreSQL. Nu acoperă diferențele de dialect — de aceea există și verificare pe PostgreSQL. |
| Ce ai scris tu? | Entități, DTO-uri, servicii, configurația de securitate, bootstrap-ul Admin și testele. Spring oferă infrastructura. |
| Cum adaugi un câmp nou? | Adaugi în entitate + request + response, actualizezi testele; schema se actualizează prin JPA. |

---

## 6. Dacă te pune să modifici ceva pe loc

Alege o schimbare mică și sigură, apoi rulează testul:

**Exemplu: numele produsului să aibă minim 3 caractere.**

1. În `ProductRequest`: `@Size(min = 3, max = 160) String name`.
2. În `ProductCatalogApiTest`: un test care trimite `"ab"` și așteaptă `400`.
3. Rulezi:

   ```bash
   cd backend && ./mvnw -q -Dtest=ProductCatalogApiTest test
   ```

Spui: „Validarea e declarativă; testul dovedește comportamentul.”

---

## 7. Ce să nu faci

- Nu deschide tot proiectul și nu derula cod la întâmplare.
- Nu citi importurile și adnotările cu voce tare.
- Nu spune „nu știu” la framework; spune „asta face Spring, eu configurez aici”.
- Nu pretinde că ai implementat Spring Security de la zero.

---

## 8. Fișă de reținut

```text
24 fișiere, ~850 linii.
Straturi: controller → service → repository.
O cerere: security → validare → service → JPA → DTO.
Securitate: JWT stateless, BCrypt, roluri din claim.
Calitate: 16 teste backend, verificare și pe PostgreSQL real.
```
