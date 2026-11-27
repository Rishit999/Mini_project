# EVChargeFlow — REST API Reference

Base URL: `http://localhost:8080/api`

---

## Health & Monitoring (`DashboardController`)

- `GET /api/health` → Service health check (`{ "status": "UP", "service": "EVChargeFlow", "timestamp": "..." }`)
- `GET /api/dashboard` → Overview KPI metrics, station summaries, queue preview, active sessions, priority distribution
- `GET /api/analytics` → Request/session aggregates, simulated energy total, charger utilization table
- `GET /api/audit` → Recent system audit logs

---

## Stations, Chargers & Settings (`StationController`)

- `GET /api/stations` → List stations with allocated power, charger counts, and queued counts
- `GET /api/stations/{stationId}` → Full station monitor view (summary, chargers, station queue, configuration)
- `POST /api/stations` → Create a station (`201 Created`)
- `PUT /api/stations/{stationId}` → Update station details & max power limit
- `PATCH /api/stations/{stationId}/status` → Update `OPERATIONAL` / `MAINTENANCE` / `OFFLINE`
- `GET /api/chargers?stationId=` → List chargers (optional station filter)
- `POST /api/chargers` → Add charger bay (`201 Created`)
- `PATCH /api/chargers/{chargerId}/status` → Update charger operational status
- `GET /api/settings` / `GET /api/settings/{stationId}` / `PUT /api/settings/{stationId}` → Station configuration settings

---

## Vehicles (`VehicleController`)

- `GET /api/vehicles` → List all registered vehicles
- `GET /api/vehicles/{vehicleId}` → Get vehicle by ID
- `POST /api/vehicles` → Register vehicle (`201 Created`)
- `PUT /api/vehicles/{vehicleId}` → Update vehicle details
- `DELETE /api/vehicles/{vehicleId}` → Delete vehicle (blocked if referenced by active/historical records)

---

## Priority Queue & Allocation (`QueueController`)

- `GET /api/queue?stationId=` → Retrieve live ordered priority queue (`PriorityBreakdown`, estimated wait)
- `POST /api/requests` → Submit charging request (`201 Created`) returning `AllocationResult` (`ASSIGNED`, `WAITING`, or `REJECTED`):

```json
{
  "vehicleId": "VEH-101",
  "stationId": "STN-01",
  "currentBatteryPercent": 15.0,
  "requiredConnector": "CCS",
  "expectedDurationMinutes": 40,
  "desiredDepartureTime": "2026-11-28T04:00:00Z",
  "targetBatteryPercent": 85.0
}
```

- `POST /api/requests/{requestId}/cancel` → Cancel eligible request and remove from heap
- `DELETE /api/requests/{requestId}` → Alias to cancel request
- `POST /api/allocate?stationId=` → Run priority allocation cycle across waiting requests

---

## Reservations (`ReservationController`)

- `GET /api/reservations?stationId=` → List reservations
- `GET /api/reservations/{reservationId}` → Get reservation details
- `POST /api/reservations` → Book non-overlapping slot (`201 Created`); returns `409 Conflict` on overlap
- `POST /api/reservations/{reservationId}/cancel` → Cancel reservation and release bay if reserved
- `POST /api/reservations/process` → Activate due slots and expire overdue grace-period bookings

---

## Charging Sessions (`SessionController`) & Wait Estimation (`WaitTimeController`)

- `GET /api/sessions?stationId=` → List simulated charging sessions
- `POST /api/sessions/start/{requestId}` → Start session for an `ASSIGNED` request
- `POST /api/sessions/start-reservation/{reservationId}` → Check-in reservation to active session
- `POST /api/sessions/{sessionId}/complete` / `POST /api/sessions/{sessionId}/stop` → Finish session, release bay, trigger waiting queue allocation
- `GET /api/wait-time/estimate?stationId=&connectorType=&currentBatteryPercent=&expectedDurationMinutes=` → Advisory prospective wait projection (`isGuaranteed: false`)
