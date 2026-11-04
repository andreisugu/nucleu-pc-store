# Nucleu — PC Components

Interfață React + TypeScript pentru magazinul demonstrativ. Catalogul public citește produse și categorii din API-ul Spring Boot, iar autentificarea și operațiile Admin folosesc endpoint-urile protejate cu JWT; nu există date de rezervă hardcodate.

## Rulare locală

Necesită Node ≥ 20.19 (Vite 8). Pornește backend-ul și PostgreSQL conform [instrucțiunilor backend](../backend/README.md), apoi într-un terminal separat:

```bash
npm install
npm run dev
```

Vite servește aplicația local și redirecționează `/api` către `http://localhost:8080`. Vizitatorii pot răsfoi/căuta produsele și se pot înregistra ca `CLIENT`; Adminul configurat în backend se autentifică din butonul **Intră în cont** și primește zona de administrare.

În panoul Admin se pot crea categorii și produse, iar produsele existente pot fi editate sau șterse. Formularul produsului acoperă nume, descriere, specificații, preț, stoc, furnizor, livrare și categorie. Swagger UI este disponibil la `http://localhost:8080/swagger-ui/index.html`.

## Verificări

```bash
npm test
npm run build
npm run lint
```

Interfața acoperă Raportarea I: autentificare Client/Admin, CRUD Admin pentru produse, navigarea catalogului/categoriilor și căutare. Favoritele, coșul și checkout-ul vor fi conectate după implementarea endpoint-urilor backend corespunzătoare.
