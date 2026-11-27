# EVChargeFlow — EV Charging Queue & Resource Allocation Decision Support System

EVChargeFlow is a full-stack web-based Decision Support System (DSS) built as a BCA undergraduate minor project using **Java 17 + Spring Boot 3.3**, **React 18 + Vite**, and **MySQL 8**.

It simulates an EV charging station network and solves real-time priority queueing, power-aware charger allocation, conflict-free reservations, and explainable wait-time projections across four integrated engines.

---

## Four-Engine Architecture

1. **Engine 1 — Data Management & Integrity (`backend/.../engine1`)**: JPA entities, relational persistence, duplicate/state-transition validation, seed data.
2. **Engine 2 — Logic & Rule Engine (`backend/.../engine2`)**:
   - **Priority Scoring (0–100):** Battery urgency (max 50) + Departure urgency (max 30) + Waiting time (max 20)
   - **Custom Max Heap (`CustomPriorityMaxHeap`):** Array-backed binary max heap (does **not** use `java.util.PriorityQueue`) with deterministic tie-breaking (`score desc → arrivalTime asc → requestId asc`)
   - **Charger Allocator:** Operational status, connector match, station kW capacity check, reservation window check
   - **Reservation Manager:** Overlap rule `ExistingStart < RequestedEnd AND RequestedStart < ExistingEnd` (adjacent slots allowed)
   - **Wait-Time Estimator:** Timeline-based advisory wait projections (clearly marked non-guaranteed)
3. **Engine 3 — Interface & Access (`backend/.../engine3` & `frontend/`)**: REST APIs, structured error responses, responsive React + Vite operational dashboard with Recharts visualizations.
4. **Engine 4 — Testing & Validation (`backend/src/test/...` & `docs/testing/test-report.md`)**: 112 JUnit 5 unit, integration, and end-to-end tests.

---

## Prerequisites

- **Java:** JDK 17+ (project compiles targeting Java 17)
- **Maven:** 3.9+
- **Node.js:** 20+ & npm
- **MySQL:** 8.x running on `localhost:3306`

---

## Database Setup (MySQL)

1. Copy `.env.example` to `.env` and configure your MySQL credentials if needed (defaults: database `evchargeflow`, user `root`, password empty or configured via `DB_PASSWORD`).
2. Ensure MySQL is running and create the database if needed (`createDatabaseIfNotExist=true` is enabled in `application.properties`):
   ```sql
   CREATE DATABASE IF NOT EXISTS evchargeflow;
   ```
3. On first startup, `DataSeeder` automatically populates realistic demo stations, chargers, vehicles, active simulated sessions, queued requests, and reservations.

---

## Running Locally

### 1. Run Automated Backend Tests (H2 in-memory)
```powershell
cd d:\Mini_project\backend
mvn test -Dspring.profiles.active=test
```

### 2. Start Backend Server (Port 8080)
```powershell
cd d:\Mini_project\backend
mvn spring-boot:run
```
Or use `.\scripts\run-backend.ps1`.

### 3. Start Frontend Dev Server (Port 5173)
```powershell
cd d:\Mini_project\frontend
npm install
npm run dev
```
Or use `.\scripts\run-frontend.ps1`.

Open `http://localhost:5173` in your browser. Vite automatically proxies `/api/*` requests to `http://localhost:8080`.

### 4. Frontend Lint & Production Build (Biome + Vite)
```powershell
cd d:\Mini_project\frontend
npm run lint
npm run build
```

---

## Documentation Index

- [Four-Engine Ownership](docs/ENGINE_OWNERSHIP.md)
- [System Architecture](docs/architecture/README.md)
- [Database Schema & ER Diagram](docs/database/schema.md)
- [REST API Reference](docs/api/README.md)
- [Testing & Validation Report](docs/testing/test-report.md)
