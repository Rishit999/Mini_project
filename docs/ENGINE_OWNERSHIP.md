# EVChargeFlow — Four-Engine Ownership & Architecture Guide

EVChargeFlow is designed as **one cohesive, integrated web application** logically divided into four modular engines so four team members can independently own, document, demonstrate, and explain their respective layers during academic evaluation.

---

## Engine Breakdown

| Engine | Package / Location | Primary Owner Focus | Key Artifacts |
|---|---|---|---|
| **Engine 1** — Data Management & Integrity | `backend/src/main/java/com/evchargeflow/engine1/` | Relational schema, persistence, CRUD, validation, referential integrity | Entities (`ChargingStation`, `Charger`, `Vehicle`, `VehicleRequest`, `Reservation`, `ChargingSession`, `StationConfiguration`, `UserAccount`, `AuditLog`), Repositories, `IntegrityValidator`, `DataIntegrityService`, `AuditLogService` |
| **Engine 2** — Logic & Rule Engine | `backend/src/main/java/com/evchargeflow/engine2/` | Priority scoring, custom max heap, charger allocation, reservation overlap, wait estimation, orchestration | `PriorityCalculator`, `CustomPriorityMaxHeap`, `QueueManager`, `ChargerAllocator`, `ReservationManager`, `WaitTimeEstimator`, `StationOrchestrator` |
| **Engine 3** — Interface & Access Engine | `backend/src/main/java/com/evchargeflow/engine3/` & `frontend/` | REST APIs, DTO mapping, exception handling, responsive React dashboard UI | `DashboardController`, `StationController`, `VehicleController`, `QueueController`, `ReservationController`, `SessionController`, `WaitTimeController`, `DtoMapper`, `DashboardAnalyticsService`, `GlobalExceptionHandler`, React pages & components |
| **Engine 4** — Testing & Validation Engine | `backend/src/test/java/com/evchargeflow/` & `docs/testing/` | Automated unit, integration, and end-to-end verification, formal test report | `IntegrityValidatorTest`, `PriorityCalculatorTest`, `CustomPriorityMaxHeapTest`, `QueueManagerTest`, `ChargerAllocatorTest`, `ReservationConflictTest`, `ReservationManagerTest`, `WaitTimeEstimatorTest`, `StationControllerIntegrationTest`, `QueueControllerIntegrationTest`, `EndToEndFlowTest` |

---

## Engine Interfaces & Data Flow

1. **Engine 3 → Engine 2 / Engine 1**: React UI calls REST endpoints (`/api/*`) exposed by Engine 3 controllers, which validate DTO payloads (`@Valid`) and delegate to `StationOrchestrator` (Engine 2) or `DataIntegrityService` (Engine 1).
2. **Engine 2 → Engine 1**: `StationOrchestrator`, `QueueManager`, and `ReservationManager` read/write persistent entities through Engine 1 services and repositories and enforce domain transitions via `IntegrityValidator`.
3. **Engine 4 → Engines 1–3**: Tests exercise `IntegrityValidator` (Engine 1), all algorithmic classes directly (Engine 2), and full HTTP/DB flows using MockMvc + H2 (`test` profile) across Engine 3.

---

## Team Division Suggestion (4 Members)

- **Member 1 (Engine 1):** Database ER design, JPA entities, MySQL configuration, CRUD services, duplicate/state-transition validation, seed data.
- **Member 2 (Engine 2):** Priority scoring table logic, array-backed `CustomPriorityMaxHeap`, tie-breaking comparator, power-aware `ChargerAllocator`, overlap-checked reservations, explainable `WaitTimeEstimator`.
- **Member 3 (Engine 3):** Spring Boot REST controllers, structured error responses, React + Vite dashboard, charts, responsive forms, API client integration.
- **Member 4 (Engine 4):** JUnit 5 test suite across all 4 engines, edge cases (power limit, adjacent slots, heap rebuild, invalid transitions), test report documentation, build/lint verification.
