
import { useEffect, useRef, useState } from 'react'
import Editor from '@monaco-editor/react'
import {
  Cartesian3,
  Color,
  createWorldTerrainAsync,
  Viewer,
  createOsmBuildingsAsync
} from 'cesium'
import 'cesium/Build/Cesium/Widgets/widgets.css'
import './App.css'

const FLEET_SIZE = 60

const originalDrones = [
  {
    id: 'DR-001',
    name: 'Drone Alpha',
    status: 'In Flight',
    lat: 22.5726,
    lon: 88.3639,
    battery: 86
  },
  {
    id: 'DR-002',
    name: 'Drone Bravo',
    status: 'Ready',
    lat: 28.6139,
    lon: 77.2090,
    battery: 100
  },
  {
    id: 'DR-003',
    name: 'Drone Charlie',
    status: 'Returning',
    lat: 19.0760,
    lon: 72.8777,
    battery: 42
  }
]

const droneNames = [
  'Alpha', 'Bravo', 'Charlie', 'Delta', 'Echo',
  'Foxtrot', 'Golf', 'Hotel', 'India', 'Juliet'
]

const initialDrones = Array.from({ length: FLEET_SIZE }, (_, index) => {
  const number = index + 1

  if (number <= originalDrones.length) {
    return {
      ...originalDrones[index],
      altitude: 120,
      simulated: true
    }
  }

  return {
    id: `DR-${String(number).padStart(3, '0')}`,
    name: `Drone ${droneNames[(number - 1) % droneNames.length]} ${number}`,
    status: ['In Flight', 'Ready', 'Returning'][index % 3],
    lat: 8 + Math.random() * 27,
    lon: 68 + Math.random() * 29,
    battery: 30 + Math.floor(Math.random() * 71),
    altitude: 120,
    simulated: true
  }
})

const starterCode = {
  java: `public class Main {
    public static void main(String[] args) {
        System.out.println("AeroSaga Mission Control");
    }
}`,
  python: `def main():
    print("AeroSaga Mission Control")

if __name__ == "__main__":
    main()`,
  javascript: `function main() {
    console.log("AeroSaga Mission Control");
}

main();`
}

const languages = [
  { id: 'java', label: 'Java', extension: 'Main.java' },
  { id: 'python', label: 'Python', extension: 'main.py' },
  { id: 'javascript', label: 'JavaScript', extension: 'main.js' }
]

function getDroneColor(status) {
  if (status === 'Returning') return Color.ORANGE
  if (status === 'Ready') return Color.LIME
  return Color.CYAN
}

function App() {
  const globeRef = useRef(null)
  const viewerRef = useRef(null)

  // Store the latest telemetry timestamp separately for each drone.
  const lastTelemetryByDroneRef = useRef(new Map())

  const [selectedDrone, setSelectedDrone] = useState(null)
  const [drones, setDrones] = useState(initialDrones)
  const [telemetryStatus, setTelemetryStatus] = useState('Connecting')
  const [globeError, setGlobeError] = useState('')
  const [activeLanguage, setActiveLanguage] = useState('java')
  const [codeByLanguage, setCodeByLanguage] = useState(starterCode)
  const [workflowId, setWorkflowId] = useState('')
  const [missionStatuses, setMissionStatuses] = useState({})
  const [missionMessage, setMissionMessage] = useState('')
  const [missionLoading, setMissionLoading] = useState(false)

  // Initialize Cesium and create all 60 drone markers.
  useEffect(() => {
    let cancelled = false
    let viewer

    async function initializeGlobe() {
      try {
        const terrainProvider = await createWorldTerrainAsync()

        if (cancelled) return

        viewer = new Viewer(globeRef.current, {
          animation: false,
          timeline: false,
          baseLayerPicker: false,
          geocoder: false,
          homeButton: true,
          sceneModePicker: false,
          navigationHelpButton: false,
          infoBox: false,
          selectionIndicator: false,
          terrainProvider,
          requestRenderMode: false
        })

        if (cancelled) {
          viewer.destroy()
          return
        }

        viewerRef.current = viewer

        for (const drone of initialDrones) {
          viewer.entities.add({
            id: `drone-${drone.id}`,
            name: drone.name,
            position: Cartesian3.fromDegrees(
              drone.lon,
              drone.lat,
              drone.altitude
            ),
            point: {
              pixelSize: 10,
              color: getDroneColor(drone.status),
              outlineColor: Color.WHITE,
              outlineWidth: 2,
              disableDepthTestDistance: Number.POSITIVE_INFINITY
            },
            label: {
              text: drone.id,
              font: '11px sans-serif',
              fillColor: Color.WHITE,
              outlineColor: Color.BLACK,
              outlineWidth: 3,
              style: 2,
              pixelOffset: new Cartesian3(0, -17, 0),
              disableDepthTestDistance: Number.POSITIVE_INFINITY
            }
          })
        }

        viewer.camera.flyTo({
          destination: Cartesian3.fromDegrees(82, 23, 3500000)
        })

        try {
          const buildings = await createOsmBuildingsAsync()

          if (!cancelled && !viewer.isDestroyed()) {
            viewer.scene.primitives.add(buildings)
          }
        } catch {
          // The globe still works if 3D buildings are unavailable.
        }
      } catch (error) {
        console.error('Cesium initialization failed:', error)

        if (!cancelled) {
          setGlobeError(
            'The 3D globe could not initialize. Check your internet connection and browser console.'
          )
        }
      }
    }

    initializeGlobe()

    return () => {
      cancelled = true

      if (viewer && !viewer.isDestroyed()) {
        viewer.destroy()
      }

      viewerRef.current = null
    }
  }, [])

  // Move every drone independently unless it has recent backend telemetry.
  useEffect(() => {
    const timer = setInterval(() => {
      const now = Date.now()
      const viewer = viewerRef.current

      setDrones(previous =>
        previous.map((drone, index) => {
          const lastTelemetryAt =
            lastTelemetryByDroneRef.current.get(drone.id) ?? 0

          // Only pause simulation for the drone receiving fresh telemetry.
          if (now - lastTelemetryAt < 5000) {
            return drone
          }

          // Give every drone a different, repeatable movement pattern.
          const phase = now / 5000 + index * 1.7

          const lat = Math.max(
            -85,
            Math.min(
              85,
              drone.lat + Math.sin(phase) * 0.006
            )
          )

          const lon = Math.max(
            -180,
            Math.min(
              180,
              drone.lon + Math.cos(phase) * 0.006
            )
          )

          if (viewer && !viewer.isDestroyed()) {
            const entity = viewer.entities.getById(
              `drone-${drone.id}`
            )

            if (entity) {
              entity.position = Cartesian3.fromDegrees(
                lon,
                lat,
                drone.altitude ?? 120
              )
            }
          }

          return { ...drone, lat, lon }
        })
      )
    }, 1000)

    return () => clearInterval(timer)
  }, [])

  // Connect to backend telemetry and reconnect when disconnected.
  useEffect(() => {
    let socket
    let reconnectTimer
    let intentionalClose = false
    let reconnectDelay = 1000

    function connect() {
      if (intentionalClose) return

      setTelemetryStatus('Connecting')

      socket = new WebSocket('ws://localhost:8081/ws/telemetry')

      socket.onopen = () => {
        setTelemetryStatus('Connected')
        reconnectDelay = 1000
      }

      socket.onmessage = event => {
        try {
          const telemetry = JSON.parse(event.data)

          if (!Array.isArray(telemetry)) return

          // Declare validUpdates only once.
          const validUpdates = telemetry.filter(update => {
            const lat = Number(update.lat)
            const lon = Number(update.lon)
            const altitude = Number(update.altitude ?? 120)

            return (
              update.id != null &&
              Number.isFinite(lat) &&
              Number.isFinite(lon) &&
              Number.isFinite(altitude) &&
              lat >= -90 &&
              lat <= 90 &&
              lon >= -180 &&
              lon <= 180
            )
          })

          // Track telemetry freshness for each individual drone.
          const telemetryTime = Date.now()

          for (const update of validUpdates) {
            lastTelemetryByDroneRef.current.set(
              String(update.id),
              telemetryTime
            )
          }

          // Merge telemetry into the existing fleet instead of replacing it.
          setDrones(previous => {
            const droneMap = new Map(
              previous.map(drone => [drone.id, drone])
            )

            for (const update of validUpdates) {
              const id = String(update.id)
              const existing = droneMap.get(id)

              droneMap.set(id, {
                ...(existing ?? {
                  id,
                  name: `Drone ${id}`,
                  status: 'Unknown',
                  battery: 0
                }),
                lat: Number(update.lat),
                lon: Number(update.lon),
                altitude: Number(update.altitude ?? 120),
                simulated: false
              })
            }

            return Array.from(droneMap.values())
          })

          // Update the corresponding Cesium markers.
          const viewer = viewerRef.current

          if (viewer && !viewer.isDestroyed()) {
            for (const update of validUpdates) {
              const id = String(update.id)
              const lat = Number(update.lat)
              const lon = Number(update.lon)
              const altitude = Number(update.altitude ?? 120)

              let entity = viewer.entities.getById(`drone-${id}`)

              if (!entity) {
                entity = viewer.entities.add({
                  id: `drone-${id}`,
                  name: `Drone ${id}`,
                  position: Cartesian3.fromDegrees(
                    lon,
                    lat,
                    altitude
                  ),
                  point: {
                    pixelSize: 10,
                    color: Color.CYAN,
                    outlineColor: Color.WHITE,
                    outlineWidth: 2,
                    disableDepthTestDistance: Number.POSITIVE_INFINITY
                  },
                  label: {
                    text: id,
                    font: '11px sans-serif',
                    fillColor: Color.WHITE,
                    outlineColor: Color.BLACK,
                    outlineWidth: 3,
                    style: 2,
                    pixelOffset: new Cartesian3(0, -17, 0),
                    disableDepthTestDistance: Number.POSITIVE_INFINITY
                  }
                })
              }

              entity.position = Cartesian3.fromDegrees(
                lon,
                lat,
                altitude
              )
            }
          }
        } catch (error) {
          console.error('Could not parse AeroSaga telemetry:', error)
        }
      }

      socket.onerror = () => {
        setTelemetryStatus('Connection error')
        socket.close()
      }

      socket.onclose = () => {
        if (intentionalClose) return

        setTelemetryStatus('Disconnected')

        reconnectTimer = setTimeout(connect, reconnectDelay)
        reconnectDelay = Math.min(reconnectDelay * 2, 10000)
      }
    }

    connect()

    return () => {
      intentionalClose = true
      clearTimeout(reconnectTimer)

      if (socket) socket.close()
    }
  }, [])

  // Start a mission through the existing backend API.
  async function startMission() {
    setMissionLoading(true)
    setMissionMessage('Starting mission...')
    setMissionStatuses({})
    setWorkflowId('')

    try {
      const response = await fetch(
        'http://localhost:8081/api/missions',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(drones.map(drone => drone.id))
        }
      )

      if (!response.ok) {
        throw new Error(`Mission start failed: HTTP ${response.status}`)
      }

      const result = await response.json()

      setWorkflowId(result.workflowId)
      setMissionMessage(
        result.message || 'Mission started successfully.'
      )
    } catch (error) {
      setMissionMessage(
        error.message || 'Could not start mission.'
      )
    } finally {
      setMissionLoading(false)
    }
  }

  // Poll the Temporal mission status endpoint.
  useEffect(() => {
    if (!workflowId) return

    let cancelled = false

    async function fetchMissionStatus() {
      try {
        const response = await fetch(
          `http://localhost:8081/api/missions/${encodeURIComponent(workflowId)}/status`
        )

        if (!response.ok) {
          throw new Error(
            `Status request failed: HTTP ${response.status}`
          )
        }

        const result = await response.json()

        if (!cancelled) {
          setMissionStatuses(result.droneStatuses || {})
          setMissionMessage('Mission status updated.')
        }
      } catch (error) {
        if (!cancelled) {
          setMissionMessage(
            `Mission started; status unavailable: ${error.message}`
          )
        }
      }
    }

    fetchMissionStatus()

    const timer = setInterval(fetchMissionStatus, 2000)

    return () => {
      cancelled = true
      clearInterval(timer)
    }
  }, [workflowId])

  function focusDrone(drone) {
    setSelectedDrone(drone)

    const viewer = viewerRef.current

    if (viewer && !viewer.isDestroyed()) {
      viewer.camera.flyTo({
        destination: Cartesian3.fromDegrees(
          drone.lon,
          drone.lat,
          450000
        )
      })
    }
  }

  function updateCode(value) {
    setCodeByLanguage(previous => ({
      ...previous,
      [activeLanguage]: value ?? ''
    }))
  }

  function resetCode() {
    setCodeByLanguage(previous => ({
      ...previous,
      [activeLanguage]: starterCode[activeLanguage]
    }))
  }

  const currentLanguage = languages.find(
    language => language.id === activeLanguage
  )

  const inFlightCount = drones.filter(
    drone => drone.status === 'In Flight'
  ).length

  const readyCount = drones.filter(
    drone => drone.status === 'Ready'
  ).length

  const returningCount = drones.filter(
    drone => drone.status === 'Returning'
  ).length

  return (
    <main className="dashboard">
      <header className="topbar">
        <div>
          <p className="eyebrow">AUTONOMOUS FLIGHT OPERATIONS</p>
          <h1>
            AeroSaga <span>Mission Control</span>
          </h1>
        </div>

        <div className="connection">
          <span
            className={`live-dot ${
              telemetryStatus === 'Connected' ? 'connected' : ''
            }`}
          />
          TELEMETRY {telemetryStatus.toUpperCase()}
        </div>
      </header>

      <section className="summary">
        <article className="summary-card">
          <span>Total fleet</span>
          <strong>{String(drones.length).padStart(2, '0')}</strong>
        </article>

        <article className="summary-card">
          <span>In flight</span>
          <strong>{String(inFlightCount).padStart(2, '0')}</strong>
        </article>

        <article className="summary-card">
          <span>Ready</span>
          <strong>{String(readyCount).padStart(2, '0')}</strong>
        </article>

        <article className="summary-card">
          <span>Returning</span>
          <strong>{String(returningCount).padStart(2, '0')}</strong>
        </article>
      </section>

      <section className="mission-control">
        <div className="panel-heading">
          <div>
            <p className="eyebrow">TEMPORAL WORKFLOW</p>
            <h2>Mission Control</h2>
          </div>

          <button
            type="button"
            onClick={startMission}
            disabled={missionLoading}
          >
            {missionLoading
              ? 'Starting...'
              : `Start ${drones.length}-Drone Mission`}
          </button>
        </div>

        {missionMessage && (
          <p role="status" aria-live="polite">
            {missionMessage}
          </p>
        )}

        {workflowId && (
          <p>
            <strong>Workflow ID:</strong>{' '}
            <code>{workflowId}</code>
          </p>
        )}

        {Object.keys(missionStatuses).length > 0 && (
          <div className="mission-status-list">
            <h3>Drone mission statuses</h3>

            {Object.entries(missionStatuses).map(([droneId, status]) => (
              <div key={droneId} className="mission-status-row">
                <span>{droneId}</span>
                <strong>{status}</strong>
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="workspace">
        <div className="map-panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">LIVE OPERATIONS / INDIA</p>
              <h2>Fleet overview</h2>
            </div>

            <span className="demo-tag">
              {drones.length} DRONES
            </span>
          </div>

          {globeError && (
            <p className="error-message">{globeError}</p>
          )}

          <div className="globe" ref={globeRef} />

          <p className="map-note">
            Simulated GPS updates · Not connected to real drones
          </p>
        </div>

        <aside className="fleet-panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">MISSION RESOURCES</p>
              <h2>Drone fleet</h2>
            </div>

            <span className="fleet-count">
              {String(drones.length).padStart(2, '0')} UNITS
            </span>
          </div>

          <div className="drone-list">
            {drones.map(drone => (
              <button
                type="button"
                className={`drone-card ${
                  selectedDrone?.id === drone.id ? 'selected' : ''
                }`}
                key={drone.id}
                onClick={() => focusDrone(drone)}
              >
                <div className="drone-card-top">
                  <div className="drone-icon">✦</div>

                  <div className="drone-title">
                    <strong>{drone.name}</strong>
                    <span>{drone.id}</span>
                  </div>

                  <span
                    className={`status ${
                      (drone.status || 'Unknown')
                        .toLowerCase()
                        .replace(' ', '-')
                    }`}
                  >
                    {drone.status || 'Unknown'}
                  </span>
                </div>

                <div className="battery-row">
                  <span>Battery</span>
                  <strong>{drone.battery}%</strong>
                </div>

                <div className="battery-track">
                  <div
                    className="battery-fill"
                    style={{ width: `${drone.battery}%` }}
                  />
                </div>

                <div className="coordinates">
                  {drone.lat.toFixed(4)}°, {drone.lon.toFixed(4)}°
                </div>
              </button>
            ))}
          </div>

          <div className="workflow-note">
            <span className="workflow-icon">↗</span>
            <div>
              <strong>Temporal workflows</strong>
              <p>
                Simulated takeoff workflow implemented in the backend.
              </p>
            </div>
          </div>
        </aside>
      </section>

      <section className="editor-panel">
        <div className="panel-heading editor-heading">
          <div>
            <p className="eyebrow">DEVELOPMENT ENVIRONMENT</p>
            <h2>Mission Code Workspace</h2>
          </div>

          <span className="demo-tag">EDITOR PREVIEW</span>
        </div>

        <div className="editor-toolbar">
          <div
            className="language-tabs"
            role="tablist"
            aria-label="Programming language"
          >
            {languages.map(language => (
              <button
                key={language.id}
                type="button"
                role="tab"
                aria-selected={activeLanguage === language.id}
                className={`language-tab ${
                  activeLanguage === language.id ? 'active' : ''
                }`}
                onClick={() => setActiveLanguage(language.id)}
              >
                {language.label}
              </button>
            ))}
          </div>

          <button
            type="button"
            className="reset-button"
            onClick={resetCode}
          >
            Reset code
          </button>
        </div>

        <div className="editor-filebar">
          <span className="file-indicator">●</span>
          <span>{currentLanguage.extension}</span>
          <span className="editor-status">
            Editable · Local preview
          </span>
        </div>

        <div className="monaco-container">
          <Editor
            height="360px"
            language={activeLanguage}
            theme="vs-dark"
            path={currentLanguage.extension}
            value={codeByLanguage[activeLanguage]}
            onChange={updateCode}
            options={{
              fontSize: 14,
              fontFamily: "'DM Mono', monospace",
              minimap: { enabled: false },
              automaticLayout: true,
              scrollBeyondLastLine: false,
              padding: { top: 16 },
              tabSize: 4,
              wordWrap: 'on',
              lineNumbers: 'on',
              renderLineHighlight: 'all',
              roundedSelection: true,
              bracketPairColorization: { enabled: true }
            }}
          />
        </div>

        <div className="editor-footer">
          <span>{currentLanguage.label}</span>
          <span>Code execution is not connected yet</span>
        </div>
      </section>

      <footer>
        AEROSAGA / WEEK 2 TELEMETRY PROTOTYPE
        <span>3D FLEET VISUALIZATION + CODE EDITOR</span>
      </footer>
    </main>
  )
}

export default App
