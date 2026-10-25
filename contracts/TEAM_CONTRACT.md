# AeroSaga – Integration contract (Drone Sim)

## Ports
| Service            | Port |
|--------------------|------|
| Drone Simulator    | 8081 |
| Telemetry Gateway  | 8080 |
| Frontend           | 3000 |
| Temporal           | 7233 |

## Temporal → Sim (commands)
Base: `http://localhost:8081`

| Method | Path | Body |
|--------|------|------|
| POST | `/api/sim/drones/{id}/takeoff` | — |
| POST | `/api/sim/drones/{id}/mission` | `{"dropLat":19.12,"dropLng":72.85}` |
| POST | `/api/sim/drones/{id}/rtb` | — |
| GET  | `/api/sim/drones/{id}` | — |
| GET  | `/api/sim/drones` | — |
| POST | `/api/sim/missions/start-all` | — |
| POST | `/api/sim/drones/{id}/offline` | — |
| POST | `/api/sim/drones/{id}/reconnect` | — |

IDs: `DRONE-01` … `DRONE-50`

## Sim → Gateway (telemetry each 1s)
`POST http://localhost:8080/api/telemetry/batch`

```json
[{
  "droneId": "DRONE-01",
  "lat": 19.076,
  "lng": 72.8777,
  "alt": 120.0,
  "battery": 87,
  "phase": "NAVIGATE_TO_DROP",
  "status": "NAVIGATE_TO_DROP",
  "online": true,
  "headingDeg": 142.5,
  "payloadKg": 20.0,
  "droneType": "HEAVY_CARGO",
  "timestamp": 1710000000000
}]