# Document de intenție — Proiect Comerț Electronic, Tema 3

**Bază:** `ghid_proiect_tema_3_rest_api_spa_mobil.md`  
**Autor:** proiect individual  
**Scop:** un magazin online demonstrativ de componente și calculatoare, inspirat de magazinele de profil precum PC Garage. Proiectul trebuie să fie interesant de demonstrat și ușor de explicat, fără complexitate sau cod adăugat doar pentru volum.

## 1. Ce construim

O aplicație pentru un catalog fictiv de componente PC și sisteme desktop configurate, cu două roluri:

- **Client:** vede și caută produse; la etapa a doua poate salva favorite, gestiona coșul și plasa o comandă demonstrativă.
- **Admin:** se autentifică și gestionează produse și categorii.

Componente: backend REST API în **Java cu Spring Boot**, bază de date **PostgreSQL** și interfață **React SPA** care consumă API-ul. API-ul va fi documentat prin Swagger/OpenAPI. Alegerea păstrează proiectul într-un stack cunoscut și permite separarea clară între API, persistență și interfață.

## 1.1 Intenția de proiectare

- Fiecare funcționalitate trebuie să susțină un flux vizibil în demonstrație sau să fie necesară pentru corectitudinea acelui flux.
- Codul va fi organizat pe responsabilități clare: API/controller, logică de domeniu/service și acces la date/repository. Evităm atât un singur bloc de cod pentru toate, cât și straturi/clase fără rol real.
- Regulile importante (preț, stoc, total, acces pe rol) se implementează într-un loc coerent și se pot explica prin exemple.
- Interfața poate avea un aspect modern de magazin hardware, dar nu se urmărește copierea PC Garage și nu se consumă timp pe efecte vizuale fără valoare pentru flux.
- Prioritatea este un traseu complet, funcțional și bine prezentabil, nu numărul de endpoint-uri sau de linii de cod.

## 2. Limitele versiunii

- Se implementează cerințele din ghid pe etape, nu funcții comerciale reale.
- Plata cu cardul este **simulată**: nu se colectează și nu se stochează date reale de card.
- Livrarea și cupoanele folosesc reguli simple, explicite, potrivite demonstrației.
- Fără integrare cu furnizori, curieri, procesatori de plăți sau servicii externe.
- Fără funcții necerute precum chat, recenzii, recomandări inteligente sau aplicație mobilă.

## 3. Plan pe raportări

### Raportarea I — fundamentul aplicației

1. **Definirea datelor:** utilizator, categorie și produs. Produsul conține nume, descriere, specificații, preț, stoc, furnizor, modalitate de livrare și categorie. Exemple de categorii: procesoare, plăci video, memorie, stocare și sisteme desktop.
2. **Autentificare și roluri:** înregistrare/autentificare client; acces admin separat și protejat. API-ul emite token și verifică rolul pentru operațiile administrative.
3. **Administrare catalog:** adminul poate crea, lista, edita și șterge produse și categorii.
4. **Catalog client:** listă produse, navigare pe categorii și căutare după nume/cuvinte-cheie.
5. **Demonstrație:** se prezintă un flux complet: autentificare admin → adăugare produs → produsul apare în catalog → căutare.

**Criteriu de gata:** rolurile sunt diferențiate, operațiile CRUD funcționează, iar clientul poate găsi produsele din interfață.

### Raportarea II — cumpărare demonstrativă

1. **Filtrare:** preț, stoc, model, furnizor și livrare; combinare prin AND/OR pe un set restrâns de filtre, demonstrat clar. În UI se oferă filtre uzuale; combinațiile logice sunt păstrate explicabile, nu se construiește un limbaj generic de interogare.
2. **Favorite:** clientul autentificat adaugă și elimină produse din lista proprie.
3. **Coș:** adaugă produse, schimbă cantități și elimină produse; se verifică să nu depășească stocul.
4. **Sumar și checkout:** adresă de livrare, metodă de livrare, cost calculat printr-o regulă simplă și cupon cu o condiție demonstrabilă (de exemplu, reducere procentuală peste un prag minim al comenzii).
5. **Comandă și istoric:** confirmarea creează o comandă demonstrativă; clientul își vede doar propriile comenzi.
6. **Demonstrație:** căutare/filtrare → favorite → coș → sumar → checkout simulat → istoric.

**Criteriu de gata:** fluxul de cumpărare poate fi parcurs complet fără servicii externe și fără date reale de plată.

## 4. Ordinea recomandată de lucru

1. Confirmați tema și tehnologiile cu cadrul didactic; alegeți catalogul fictiv.
2. Desenați schema minimală a datelor și lista endpoint-urilor.
3. Implementați autentificarea și CRUD-ul catalogului; testați API-ul înainte de interfață.
4. Conectați SPA-ul și validați fluxul Raportării I.
5. Adăugați filtrarea, favoritele și coșul, apoi comenzile/checkout-ul simulat.
6. Testați scenariile principale, pregătiți date demonstrative și documentația/API-ul pentru prezentare.

După fiecare etapă, păstrați o versiune funcțională și cereți feedback la laborator, conform recomandării din notițele cursului.

## 5. Decizii tehnice

Stack ales: **Spring Boot + React SPA + PostgreSQL**, cu API descris OpenAPI/Swagger. Aplicația rulează local și folosește date demonstrative. Nu introducem microservicii, servicii cloud sau infrastructură de deployment decât dacă devin cerințe explicite.

Autentificarea va folosi token-uri și roluri, conform cerinței. Checkout-ul și plata rămân simulate: nu se integrează un procesator și nu se stochează date de card.

## 6. Verificări minime

- Cererile fără autentificare nu pot modifica date; clientul nu poate folosi rutele admin.
- Clientul poate lista, căuta și filtra produse.
- Favoritele și comenzile sunt izolate per utilizator.
- Coșul respinge cantități peste stoc și totalurile sunt calculate corect.
- Checkout-ul nu cere și nu persistă numărul cardului.
- Endpoint-urile pot fi explorate/testate din documentația OpenAPI.

## 7. Întrebări de confirmat

- Este acceptată tema magazinului de calculatoare/componente și stack-ul Spring Boot + React + PostgreSQL?
- Ce dată și ce format sunt cerute pentru cele două raportări? Ghidul indică săptămânile 7/8 și 11/12, dar trebuie verificat calendarul grupei.
- Ce nivel de detaliu se așteaptă la filtrele AND/OR și la regulile pentru cupoane/livrare?

## Stadiu implementare

- **Raportarea I — implementare completă, verificare locală de prezentare rămasă:** produse persistate prin JPA, CRUD, căutare case-insensitive după nume/descriere/specificații și filtrare după categorie. Categoriile au CRUD, nume unice fără diferențiere de majuscule și nu pot fi șterse cât timp au produse asociate. SPA-ul React consumă endpoint-urile publice de listare, căutare și filtrare, cu stări de încărcare/eroare/fără rezultate.
- Relația produs-categorie este normalizată în baza de date; API-ul de produs primește deocamdată numele categoriei pentru a păstra payload-ul ușor de citit.
- Autentificarea JWT și rolurile sunt implementate: înregistrarea acordă doar `CLIENT`, iar modificările catalogului sunt rezervate rolului `ADMIN`. API-ul este documentat cu Swagger/OpenAPI, inclusiv schema bearer JWT. SPA-ul React/Vite include formulare de înregistrare/login, zonă Admin cu CRUD produse și creare categorii, plus catalog public cu căutare/filtrare. Favoritele, coșul și comenzile nu sunt încă implementate.

### Raportarea I — integrare SPA și documentație API

- Frontend React + TypeScript în `frontend/`, cu proxy Vite `/api` către backend-ul local. Pornirea locală este automatizată prin `porneste.sh`, care verifică baza, pornește API-ul și interfața și le oprește la `Ctrl+C`.
- Catalogul citește produse și categorii din API, filtrează după text/categorie și afișează loading, error și empty states.
- Clientul se poate înregistra și autentifica; contul Admin se inițializează din `ADMIN_EMAIL`/`ADMIN_PASSWORD`. Formularul Admin acoperă toate câmpurile cerute pentru produs și apelează endpoint-urile protejate cu JWT.
- Testele UI rulează cu Vitest + Testing Library; `npm test` și `npm run build` validează frontend-ul.
- Verificat manual pe PostgreSQL 18 real: catalog, autentificare Admin, creare categorie/produs și căutare răspund corect. Testele automate folosesc H2, care nu reproduce toate particularitățile de tipare ale PostgreSQL.
- Backend OpenAPI: `/v3/api-docs`; Swagger UI: `/swagger-ui/index.html`.
- Favoritele/coșul/checkout-ul sunt păstrate pentru etapa a doua și nu sunt simulate de controale fără funcționalitate.
