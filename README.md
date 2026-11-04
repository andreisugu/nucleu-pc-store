# Nucleu — magazin demonstrativ de componente PC

Aplicație de comerț electronic cu backend **Spring Boot** (REST, JWT, PostgreSQL) și interfață **React + Vite**. Proiect individual pentru tema de Comerț Electronic — Raportarea I.

## Funcționalități

- Catalog public cu produse, categorii, căutare case-insensitive și filtrare după categorie.
- Înregistrare și autentificare clienți; token JWT valid o oră.
- Roluri separate: `CLIENT` și `ADMIN`.
- Administrare produse: creare, editare și ștergere, cu toate câmpurile cerute (nume, descriere, specificații, preț, stoc, furnizor, livrare, categorie).
- Administrare categorii, cu protecție împotriva duplicatelor și a ștergerii categoriilor care au produse.
- Documentație OpenAPI/Swagger și teste automate pentru API și interfață.

## Tehnologii

| Componentă | Tehnologie |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Security, Spring Data JPA |
| Bază de date | PostgreSQL 18 (Podman) |
| Frontend | React 19, TypeScript, Vite 8 |
| Teste | JUnit + MockMvc + H2; Vitest + Testing Library |
| Documentație API | springdoc OpenAPI 3 / Swagger UI |

## Pornire rapidă

Cerințe: JDK 21+, Node ≥ 20.19 și Podman.

1. Clonează proiectul:

   ```bash
   git clone https://github.com/andreisugu/nucleu-pc-store.git
   cd nucleu-pc-store
   ```

2. Creează baza de date o singură dată:

   ```bash
   podman volume create pcgarage-pgdata
   podman run -d --name pcgarage-postgres \
     -e POSTGRES_USER=pcgarage -e POSTGRES_PASSWORD=pcgarage -e POSTGRES_DB=pcgarage \
     -v pcgarage-pgdata:/var/lib/postgresql:Z -p 5432:5432 docker.io/library/postgres:latest
   ```

   La pornirile următoare este suficient `podman start pcgarage-postgres`.

3. Pornește aplicația:

   ```bash
   ./porneste.sh
   ```

Scriptul verifică baza, compilează backend-ul doar dacă sursele s-au schimbat, pornește API-ul pe `http://localhost:8080` și interfața pe `http://localhost:5173`, apoi deschide browserul. `Ctrl+C` oprește backend-ul și frontend-ul; PostgreSQL rămâne pornit.

> Scriptul cere Node ≥ 20.19 pentru Vite 8. Dacă terminalul rulează într-un container cu Node 18, folosește un terminal pe host.

## Conturi demonstrative

| Rol | Email | Parolă |
|---|---|---|
| Admin | `admin@nucleu.local` | `AdminDemo12345!` |
| Client | se creează din interfață | minim 12 caractere |

Adminul este creat la prima pornire din variabilele `ADMIN_EMAIL` și `ADMIN_PASSWORD`; poți suprascrie și `JWT_SECRET`. **Acestea sunt credențiale demonstrative** — înainte de orice utilizare reală, schimbă secretul JWT și parola Adminului.

## API

Documentația interactivă: `http://localhost:8080/swagger-ui/index.html`
Contractul OpenAPI: `http://localhost:8080/v3/api-docs`

Operațiile de citire sunt publice; cele de scriere cer `Authorization: Bearer <token>` pentru un cont `ADMIN`.

## Verificări

```bash
cd backend && ./mvnw test
cd frontend && npm test && npm run build
```

## Structură

- `backend/` — API Spring Boot, teste și configurare.
- `frontend/` — SPA React/Vite și teste UI.
- `plan_proiect_tema_3.md` — documentul de intenție și stadiul implementării.
- `ghid_prezentare_raportarea_1.md` — scenariul de demonstrație.
- `ghid_explicare_cod_backend.md` — explicația arhitecturii backend-ului.
- `porneste.sh` — pornirea locală a întregului stack.
