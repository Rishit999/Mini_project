# EVChargeFlow — Test Report (`Engine 4`)

**Execution Command:** `mvn test -Dspring.profiles.active=test` (H2 in-memory MySQL-mode database)  
**Latest Verified Result:** **112 Tests Run, 0 Failures, 0 Errors, 0 Skipped (BUILD SUCCESS)**

---

## Test Suite Summary

| Suite ID | Test Class | Engine Scope | Cases | Status |
|---|---|---|---|---|
| `TC-E1-01` | `IntegrityValidatorTest` | Engine 1 — Validation & Transitions | 14 | PASS |
| `TC-E2-01` | `PriorityCalculatorTest` | Engine 2 — 0–100 Priority Scoring | 20 | PASS |
| `TC-E2-02` | `CustomPriorityMaxHeapTest` | Engine 2 — Custom Array Max Heap | 16 | PASS |
| `TC-E2-03` | `ChargerAllocatorTest` | Engine 2 — Connector, Power & Reservation Rules | 12 | PASS |
| `TC-E2-04` | `ReservationConflictTest` | Engine 2 — Overlap & Adjacent Slot Rules | 12 | PASS |
| `TC-E2-05` | `QueueManagerTest` | Engine 2 — Priority Queue & Tie-Breaking | 6 | PASS |
| `TC-E2-06` | `ReservationManagerTest` | Engine 2 — Service Persistence & Expiry | 5 | PASS |
| `TC-E2-07` | `WaitTimeEstimatorTest` | Engine 2 — Timeline Wait Projections | 5 | PASS |
| `TC-E3-01` | `StationControllerIntegrationTest` | Engine 3 — Station/Vehicle/Dashboard APIs | 10 | PASS |
| `TC-E3-02` | `QueueControllerIntegrationTest` | Engine 3 — Queue, Reject & Wait Estimate APIs | 4 | PASS |
| `TC-E4-01` | `EndToEndFlowTest` | Engine 4 — Vehicle → Allocation → Session → Release | 8 | PASS |

---

## Documented Representative Test Cases

| Test ID | Description | Input | Expected Output | Actual Result | Status |
|---|---|---|---|---|---|
| `PC-001` | Critical battery urgency (0–10%) | `currentBatteryPercent = 5` | `batteryScore = 50` | `50` | PASS |
| `PC-019` | Maximum combined priority score | Battery 5%, departure in 20m, wait 90m | `totalScore = 100` (`50+30+20`) | `100` | PASS |
| `PC-020` | Minimum combined priority score | Battery 100%, no departure, wait 5m | `totalScore = 0` (`0+0+0`) | `0` | PASS |
| `CH-006` | Tie-breaking by earlier arrival | Two items score 70, `t1 < t2` | Earlier arrival extracted first | Earlier item (`R2`) | PASS |
| `CH-007` | Tie-breaking by lexicographic ID | Same score 70, same timestamp (`REQ-001`, `REQ-002`) | `REQ-001` extracted first | `REQ-001` | PASS |
| `CH-016` | Heap scalability & sort invariant | 1000 random priority items | All 1000 extracted non-increasing | 1000 valid extractions | PASS |
| `CA-005` | Station power capacity constraint | Max 100 kW, allocated 90 kW, charger 22 kW | `null` (exceeds 100 kW) | `null` | PASS |
| `CA-009` | Active reservation conflict check | Available charger with active overlapping booking | Charger skipped (`null`) | `null` | PASS |
| `RM-005` | Adjacent reservation boundary | Existing `11:00–12:00`, requested `12:00–13:00` | No conflict (`false`) | `false` | PASS |
| `RMS-002` | Overlapping slot booking via service | Existing `T+30..T+90`, new `T+60..T+120` | `ConflictException` (`409`) | Thrown | PASS |
| `RMS-004` | Grace-period expiry transition | Confirmed booking past `start + 15m` | Transition to `EXPIRED` | `EXPIRED` | PASS |
| `WT-003` | Cumulative queue wait projection | Occupied 60 kW charger free in 15m, REQ-1 (20m), REQ-2 (30m) | REQ-1 `15.0m`, REQ-2 `35.0m` | `15.0m`, `35.0m` | PASS |
| `IV-003` | Negative battery percentage | `currentBatteryPercent = -1.0` | `DomainValidationException` | Thrown | PASS |
| `IV-014` | Invalid state transition | Request `COMPLETED -> CHARGING` | `DomainValidationException` | Thrown | PASS |
| `QI-002` | Incompatible connector station | `CHADEMO` request at `TYPE2`-only station | `outcome = REJECTED` | `REJECTED` | PASS |
| `E2E-005` | Duplicate active request guard | Second request for vehicle in active session | HTTP `409 Conflict` | `409` | PASS |
| `E2E-007` | Session completion auto-release | Complete active session while `REQ` waiting | `COMPLETED` & queue drained | Queue size `0` | PASS |
