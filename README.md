# BidMart A01

> **IMPORTANT (DO NOT BREAK CI)**
> - **JANGAN ngerusak/ubah logic backend** yang bisa mempengaruhi **PMD** dan **testing**.
> - Patch yang dibahas di chat ini fokus ke **UI/UX + responsive (frontend)**.
> - Nama aplikasi: **BidMart A01**.
> - Semua credentials di README ini untuk **dev/local aja**.

---

## Project Structure
- `backend/` — Spring Boot (Java 21) + PostgreSQL + Security/RBAC + PMD + JUnit
- `frontend/` — React + TypeScript + Vite

Default ports:
- Frontend (dev): http://localhost:5173
- Backend: http://localhost:8080
- Postgres (host): localhost:5433

---

## Prerequisites
- Docker + Docker Compose
- Java **21**
- Node.js **20/22** + npm

---

# QUICK START (Recommended)

Run from **repo root**:

1) Start DB (Docker)
```bash
docker compose up -d db
```

2) Run backend (local)
```bash
cd backend
./gradlew bootRun
```

3) Run frontend (local)
```bash
cd ../frontend
npm ci
npm run dev
```

Open:
- Frontend: http://localhost:5173
- Backend: http://localhost:8080

## Optional: (If you want to reset old Database) (all of this run locally)
```bash
docker compose down -v
docker compose up -d db
```

---

# RUNNING WITH DOCKER

> Run all commands from **repo root** (folder yang ada `docker-compose.yml`).

## A) Start DB only
**Purpose:** Bring up PostgreSQL locally for backend local dev or DB inspection.

Start:
```bash
docker compose up -d db
docker compose ps
```

View DB logs:
```bash
docker compose logs -f db
```

Stop:
```bash
docker compose down
```

Reset DB (delete volume/data):
```bash
docker compose down -v
```

## B) Start DB + Backend (Docker)
**Purpose:** Backend runs in container and connects to DB container.

Start:
```bash
docker compose up -d --build db backend
docker compose ps
```

Backend logs:
```bash
docker compose logs -f backend
```

Stop:
```bash
docker compose down
```

## C) Start Fullstack (Docker)
**Purpose:** Run DB + Backend + Frontend via Docker.

Start:
```bash
docker compose up -d --build
docker compose ps
```

Stop:
```bash
docker compose down
```

---

# LOCAL DEV (without Docker backend/frontend)

## Step 1 — Start DB (Docker)
```bash
docker compose up -d db
```

## Step 2 — Run backend (local)
**Purpose:** Start backend at http://localhost:8080 using DB at localhost:5433.
```bash
cd backend
./gradlew bootRun
```

Windows:
```bat
cd backend
gradlew.bat bootRun
```

## Step 3 — Run frontend (Vite)
**Purpose:** Start dev UI at http://localhost:5173. Vite proxies `/api/*` → backend.
```bash
cd ../frontend
npm ci
npm run dev
```

---

# BACKEND QUALITY & TESTS (PMD + JUnit)

> Run commands from `backend/`.

## A) Run tests
**Purpose:** Run JUnit tests (tests use H2/in-memory; DB docker not required).
```bash
cd backend
./gradlew clean test
```

## B) Run PMD
**Purpose:** Static analysis (quality gate). Violations will fail the build.
```bash
cd backend
./gradlew pmdMain pmdTest
```

## C) CI-like (tests + PMD)
```bash
cd backend
./gradlew clean test pmdMain pmdTest
```

PMD report output:
- `backend/build/reports/pmd/`

---

# FRONTEND TYPESCRIPT CHECKS (Lint + Build)

> Run commands from `frontend/`.

Install deps:
```bash
cd frontend
npm ci
```

## A) Lint
**Purpose:** Run ESLint over the codebase.
```bash
cd frontend
npm run lint
```

## B) Type-check + Build
**Purpose:** `tsc -b` (type-check) + `vite build`.
```bash
cd frontend
npm run build
```

## C) Dev server
```bash
cd frontend
npm run dev
```

---

# CREDENTIALS & IMPORTANT CONFIG (Tim perlu tahu)

## A) Database (PostgreSQL)
From `docker-compose.yml`:
- Host: `localhost`
- Port: `5433`
- Database: `bidmart`
- Username: `bidmart`
- Password: `bidmart`

Optional connect via psql:
```bash
psql -h localhost -p 5433 -U bidmart -d bidmart
# password: bidmart
```

## B) Backend datasource defaults
Backend reads (defaults):
- `SPRING_DATASOURCE_URL` (default: `jdbc:postgresql://localhost:5433/bidmart`)
- `SPRING_DATASOURCE_USERNAME` (default: `bidmart`)
- `SPRING_DATASOURCE_PASSWORD` (default: `bidmart`)

When backend runs in docker, it points to the DB container:
- `jdbc:postgresql://db:5432/bidmart`

## C) SQL init behavior
Backend schema/data initialization is controlled by Spring SQL init flags.
In Docker compose, initialization is commonly forced (example):
- `SPRING_SQL_INIT_MODE=always`

## D) App accounts (flow-level credentials)
This is what teammates **must** know to login/admin successfully:
1) Username otomatis **di-lowercase** (login pakai lowercase).
2) Register mengembalikan `verificationToken` dan harus `POST /api/auth/verify-email`.
3) MFA/2FA: `/api/auth/login` bisa balikin `mfaRequired=true` + `devCode` → lanjut `POST /api/auth/2fa/verify`.
4) Cara pakai token untuk request: `Authorization: Bearer <UUID>` (atau legacy `X-Auth-Token: <UUID>`).
5) Cara promote ADMIN via SQL (lihat bawah).

---

# AUTH / API SMOKE TEST (DEV CHEATSHEET)

> Notes:
> - Username selalu dinormalisasi ke **lowercase** di backend (login pakai lowercase).
> - Register **tidak boleh** request role ADMIN.
> - Access token format: UUID. Pakai header `Authorization: Bearer <token>` (atau legacy `X-Auth-Token: <token>`).

## 1) Register (dapet verificationToken)
```bash
curl -sS -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo","requestedRole":"BUYER"}'
```

## 2) Verify email
```bash
curl -sS -X POST http://localhost:8080/api/auth/verify-email \
  -H "Content-Type: application/json" \
  -d '{"token":"<PASTE_VERIFICATION_TOKEN>"}'
```

## 3) Login
```bash
curl -sS -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo"}'
```

If MFA not active → response contains token pair:
- `accessToken`, `refreshToken`, `tokenType`, `expiresIn`

If MFA active → response contains:
- `mfaRequired=true`, `challengeId`, `method`, `devCode`

## 4) MFA verify (kalau login mfaRequired=true)
> Untuk dev/testing, bisa pakai `devCode` dari response login.
```bash
curl -sS -X POST http://localhost:8080/api/auth/2fa/verify \
  -H "Content-Type: application/json" \
  -d '{"challengeId":"<PASTE_CHALLENGE_ID>","code":"<PASTE_DEV_CODE_OR_OTP>"}'
```

## 5) Call protected endpoint pakai token
```bash
ACCESS="<PASTE_ACCESS_TOKEN>"
curl -sS http://localhost:8080/api/auth/me -H "Authorization: Bearer $ACCESS"
```

List sessions:
```bash
curl -sS http://localhost:8080/api/auth/sessions -H "Authorization: Bearer $ACCESS"
```

Revoke session:
```bash
curl -sS -X POST http://localhost:8080/api/auth/sessions/<TOKEN_UUID>/revoke \
  -H "Authorization: Bearer $ACCESS"
```

Refresh token:
```bash
curl -sS -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<PASTE_REFRESH_TOKEN>"}'
```

## 6) Enable/disable MFA (butuh login)
Enable email MFA:
```bash
curl -sS -X POST http://localhost:8080/api/auth/2fa/enable-email \
  -H "Authorization: Bearer $ACCESS"
```

Disable MFA:
```bash
curl -sS -X POST http://localhost:8080/api/auth/2fa/disable \
  -H "Authorization: Bearer $ACCESS"
```

TOTP setup (returns secret + otpauthUrl):
```bash
curl -sS -X POST http://localhost:8080/api/auth/2fa/totp/setup \
  -H "Authorization: Bearer $ACCESS"
```

---

# ADMIN SETUP (PROMOTE FIRST ADMIN)

> Register biasa tidak membuat ADMIN. Cara paling cepat: tambah ADMIN lewat SQL.

## Option A — psql from host
```bash
psql -h localhost -p 5433 -U bidmart -d bidmart \
  -c "UPDATE app_users SET role='ADMIN' WHERE username='admin';"
```

## Option B — psql inside container
```bash
docker compose exec db psql -U bidmart -d bidmart \
  -c "UPDATE app_users SET role='ADMIN' WHERE username='admin';"
```

User table: `app_users` (see `backend/src/main/resources/schema.sql`).

---

# USEFUL COMMANDS

View running containers:
```bash
docker compose ps
```

Follow all logs:
```bash
docker compose logs -f
```

Rebuild only backend container:
```bash
docker compose up -d --build backend
```

---

# TROUBLESHOOTING

## Port conflicts
Defaults:
- 5173 (frontend)
- 8080 (backend)
- 5433 (postgres host mapping)

If a port is already used, change port mapping in `docker-compose.yml` or stop the conflicting process.

## DB got corrupted / schema mismatch
Reset DB volume:
```bash
docker compose down -v
docker compose up -d db
```

## Frontend can't hit API
For local dev:
- backend must run at http://localhost:8080
- frontend must run with `npm run dev` so Vite proxy `/api` is active