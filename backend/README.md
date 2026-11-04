# PC Garage Demo API

Backend REST API pentru magazinul demonstrativ de componente PC.

## Stadiu

Backend-ul are catalog cu creare/listare/căutare, categorii CRUD și relație persistentă produs-categorie. Produsele și categoriile sunt persistate prin JPA; câmpurile și regulile de bază sunt validate.

API-ul folosește JWT și roluri `CLIENT`/`ADMIN`. Citirea catalogului este publică; modificările cer token de Admin. Pentru testare, baza H2 și testele nu necesită conturi configurate.

Contractul API este disponibil la `http://localhost:8080/v3/api-docs`, iar interfața Swagger UI la `http://localhost:8080/swagger-ui/index.html`.

## Baza de date (Podman)

PostgreSQL rulează local într-un container Podman, cu datele păstrate în volumul `pcgarage-pgdata`:

```bash
podman start pcgarage-postgres
podman exec pcgarage-postgres pg_isready -U pcgarage -d pcgarage
```

Dacă recipientul nu mai există, îl recreezi (atenție: `podman rm` + `podman volume rm` șterg definitiv datele):

```bash
podman volume create pcgarage-pgdata
podman run -d --name pcgarage-postgres \
  -e POSTGRES_USER=pcgarage -e POSTGRES_PASSWORD=pcgarage -e POSTGRES_DB=pcgarage \
  -v pcgarage-pgdata:/var/lib/postgresql:Z -p 5432:5432 docker.io/library/postgres:latest
```

Pentru PostgreSQL 18, volumul se montează la `/var/lib/postgresql`, nu la `/var/lib/postgresql/data`.

## Rulare teste

Proiectul cere JDK 21 sau mai nou. Testele folosesc o bază H2 temporară și nu au nevoie de PostgreSQL:

```bash
./mvnw test
```

## Rulare locală

Aplicația folosește PostgreSQL. Valorile implicite din `src/main/resources/application.properties` sunt `jdbc:postgresql://localhost:5432/pcgarage` cu utilizator și parolă `pcgarage`, deci se potrivesc containerului de mai sus. Le poți suprascrie prin `DB_URL`, `DB_USERNAME` și `DB_PASSWORD`.

Configurează și `JWT_SECRET` (minimum 32 bytes) și `ADMIN_EMAIL` + `ADMIN_PASSWORD` (minimum 12 caractere, maximum 72 bytes UTF-8) pentru a crea contul Admin la prima pornire. Parolele sunt stocate cu BCrypt. Secretul implicit JWT este doar pentru dezvoltare locală și trebuie suprascris înainte de expunerea aplicației.

```bash
export JWT_SECRET='local-demo-secret-change-before-sharing-123456'
export ADMIN_EMAIL='admin@nucleu.local'
export ADMIN_PASSWORD='AdminDemo12345!'
./mvnw spring-boot:run
```

Contul Admin este creat automat dacă nu există. Clienții își creează contul din interfață; endpoint-ul public de înregistrare nu permite alegerea rolului Admin.

Endpoint-urile disponibile momentan:

- `POST /api/auth/register` — înregistrează un client; rolul primit în payload este ignorat.
- `POST /api/auth/login` — validează email/parolă și emite un JWT valabil o oră.
- `GET /api/products?q=ryzen&categoryId=1` — caută după nume, descriere sau specificații și poate filtra după categorie.
- `GET /api/products/{id}` — afișează un produs.
- `POST /api/products` — creează un produs și returnează URL-ul său în header-ul `Location`.
- `PUT /api/products/{id}` / `DELETE /api/products/{id}` — editează, respectiv șterge un produs.
- `GET /api/categories` — listează categoriile.
- Operațiile de modificare la `/api/products` și `/api/categories` cer `Authorization: Bearer <token>` pentru un cont cu rol `ADMIN`.

Exemplu pentru `POST /api/products`:

```json
{
  "name": "GeForce RTX 5080",
  "description": "Placă video pentru gaming",
  "specifications": "16 GB GDDR7",
  "price": 5999.99,
  "stock": 3,
  "supplier": "Demo Parts",
  "deliveryMethod": "Curier",
  "category": "Plăci video"
}
```
