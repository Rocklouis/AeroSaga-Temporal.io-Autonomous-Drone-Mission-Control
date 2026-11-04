
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

function getStatusClass(status) {
  const value = String(status || 'PENDING').toUpperCase()

  if (value.includes('COMPLETED')) return 'status-completed'
  if (value.includes('FAILED')) return 'status-failed'

  if (value.includes('RUNNING') || value.includes('ACTIVE')) {
    return 'status-running'
  }

  if (value.includes('CANCELED') || value.includes('TERMINATED')) {
    return 'status-failed'
  }

  return 'status-pending'
}

function MetricCard({ title, value, unit, description, icon }) {
  return (
    <article className="metric-card">
      <div className="metric-card-heading">
        <span className="metric-icon" aria-hidden="true">
          {icon}
        </span>
        <span className="metric-label">{title}</span>
      </div>

      <div className="metric-value">
        {value}
        {unit && <span className="metric-unit"> {unit}</span>}
      </div>

      <p className="metric-description">{description}</p>
    </article>
  )
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
  const [workflowStatus, setWorkflowStatus] = useState('')
  const [missionSteps, setMissionSteps] = useState({})
  const [missionError, setMissionError] = useState('')
  const [droneSearch, setDroneSearch] = useState('')
  const [benchmark, setBenchmark] = useState(null)
  const [benchmarkLoading, setBenchmarkLoading] = useState(false)
  const [benchmarkError, setBenchmarkError] = useState('')

  const [compensationWorkflowId, setCompensationWorkflowId] = useState(
    'aerosaga-mission-faad212b-68d3-41fe-a229-8024a3b9bdb9'
  )
  const [compensationStatuses, setCompensationStatuses] = useState({})
  const [compensationLoading, setCompensationLoading] = useState(false)
  const [compensationMessage, setCompensationMessage] = useState('')

  async function fetchBenchmark() {
    setBenchmarkLoading(true)
    setBenchmarkError('')

    try {
      const response = await fetch(
        'http://localhost:8081/api/performance/benchmark'
      )

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }

      const data = await response.json()
      setBenchmark(data)
    } catch (error) {
      setBenchmarkError(
        error instanceof Error ? error.message : 'Unable to fetch benchmark'
      )
    } finally {
      setBenchmarkLoading(false)
    }
  }

  useEffect(() => {
    fetchBenchmark()
  }, [])
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
          requestRenderMode: true,
          maximumRenderTimeChange: Infinity
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
            Math.min(85, drone.lat + Math.sin(phase) * 0.006)
          )

          const lon = Math.max(
            -180,
            Math.min(180, drone.lon + Math.cos(phase) * 0.006)
          )

          if (viewer && !viewer.isDestroyed()) {
            const entity = viewer.entities.getById(`drone-${drone.id}`)

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

      // Request one Cesium frame after updating the drone positions.
      if (viewer && !viewer.isDestroyed()) {
        viewer.scene.requestRender()
      }
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
setMissionSteps({})
setWorkflowStatus('')
setMissionError('')
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
 async function fetchCompensationStatus() {
  const id = compensationWorkflowId.trim()

  if (!id) {
    setCompensationMessage('Please enter a workflow ID.')
    return
  }

  setCompensationLoading(true)
  setCompensationMessage('Fetching compensation status...')
  setCompensationStatuses({})

  try {
    const response = await fetch(
      `http://localhost:8081/api/missions/${encodeURIComponent(id)}/status`
    )

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    const result = await response.json()

    setCompensationStatuses(result.droneStatuses || {})
    setCompensationMessage('Workflow status retrieved successfully.')
  } catch (error) {
    setCompensationMessage(
      `Could not retrieve workflow status: ${error.message}`
    )
  } finally {
    setCompensationLoading(false)
  }
}
async function fetchCompensationStatus() {
  const id = compensationWorkflowId.trim()

  if (!id) {
    setCompensationMessage('Please enter a workflow ID.')
    return
  }

  setCompensationLoading(true)
  setCompensationMessage('Fetching compensation status...')
  setCompensationStatuses({})

  try {
    const response = await fetch(
      `http://localhost:8081/api/missions/${encodeURIComponent(id)}/status`
    )

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    const result = await response.json()

    setCompensationStatuses(result.droneStatuses || {})
    setCompensationMessage('Workflow status retrieved successfully.')
  } catch (error) {
    setCompensationMessage(
      `Could not retrieve workflow status: ${error.message}`
    )
  } finally {
    setCompensationLoading(false)
  }
}
  // Poll the Temporal mission status endpoint.
  
  // Poll Temporal workflow status and mission steps.
  useEffect(() => {
    if (!workflowId) return

    let cancelled = false
    let timer

    async function fetchMissionData() {
      try {
        const baseUrl =
          `http://localhost:8081/api/missions/${encodeURIComponent(workflowId)}`

        const [statusResponse, stepsResponse] = await Promise.all([
          fetch(`${baseUrl}/status`),
          fetch(`${baseUrl}/steps`)
        ])

        if (!statusResponse.ok) {
          throw new Error(
            `Status request failed: HTTP ${statusResponse.status}`
          )
        }

        if (!stepsResponse.ok) {
          throw new Error(
            `Steps request failed: HTTP ${stepsResponse.status}`
          )
        }

        const [statusResult, stepsResult] = await Promise.all([
          statusResponse.json(),
          stepsResponse.json()
        ])

        if (cancelled) return

        setWorkflowStatus(statusResult.workflowStatus || 'UNKNOWN')
        setMissionStatuses(statusResult.droneStatuses || {})
        setMissionSteps(stepsResult.missionSteps || {})
        setMissionError('')
        setMissionMessage('Mission data synchronized with Temporal.')

        const terminalStatuses = [
          'COMPLETED',
          'FAILED',
          'CANCELED',
          'TERMINATED',
          'TIMED_OUT'
        ]

        if (terminalStatuses.includes(statusResult.workflowStatus)) {
          clearInterval(timer)
        }

      } catch (error) {
        if (!cancelled) {
          setMissionError(error.message)
          setMissionMessage('Could not synchronize mission data.')
        }
      }
    }

    fetchMissionData()

    timer = setInterval(fetchMissionData, 2000)

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
const droneStatusEntries = Object.entries(missionStatuses)

const completedDrones = droneStatusEntries.filter(
  ([, status]) => status === 'MISSION_COMPLETED'
).length

const failedDrones = droneStatusEntries.filter(
  ([, status]) => String(status).includes('FAILED')
).length

const pendingDrones = droneStatusEntries.filter(
  ([, status]) => String(status).includes('PENDING')
).length

const activeDrones = Math.max(
  0,
  droneStatusEntries.length -
    completedDrones -
    failedDrones -
    pendingDrones
)

const totalSteps = Object.keys(missionSteps).length * 4

const completedSteps = Object.values(missionSteps).reduce(
  (total, steps) =>
    total +
    Object.values(steps).filter(status => status === 'Completed').length,
  0
)

const missionProgress =
  totalSteps > 0
    ? Math.round((completedSteps / totalSteps) * 100)
    : 0

const filteredDrones = Object.entries(missionSteps).filter(
  ([droneId]) =>
    droneId.toLowerCase().includes(droneSearch.toLowerCase())
)
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

<section className="performance-panel">
  <div className="panel-heading performance-heading">
    <div>
      <p className="eyebrow">SYSTEM DIAGNOSTICS / BENCHMARK</p>
      <h2>Execution Performance</h2>
      <p className="performance-intro">
        GraalPy execution compared with an in-process Java computation.
      </p>
    </div>

    <span className={`demo-tag ${benchmark ? 'benchmark-live' : ''}`}>
      <span className="status-dot" />
      {benchmark ? 'LIVE DATA' : 'AWAITING DATA'}
    </span>
  </div>

  {benchmarkLoading && (
    <div className="benchmark-message">
      <span className="loading-spinner" />
      <p>Loading benchmark results...</p>
    </div>
  )}

  {benchmarkError && (
    <div className="benchmark-message benchmark-error" role="alert">
      <strong>BENCHMARK UNAVAILABLE</strong>
      <p>{benchmarkError}</p>
    </div>
  )}

  {!benchmarkLoading && !benchmarkError && benchmark && (
    <>
      <div className="benchmark-overview">
        <div>
          <span className="benchmark-label">BENCHMARK STATUS</span>
          <strong>Execution test completed</strong>
        </div>

        <span className="benchmark-result-tag">
          <span className="status-dot" />
          DATA RECEIVED
        </span>
      </div>

      <div className="metrics-grid">
        <MetricCard
          title="Python Execution"
          value={(benchmark.pythonExecutionNanos / 1000000).toFixed(4)}
          unit="ms"
          icon="PY"
          description="Measured GraalPy execution time"
        />

        <MetricCard
          title="Java Baseline"
          value={(benchmark.javaBaselineNanos / 1000000).toFixed(4)}
          unit="ms"
          icon="JV"
          description="In-process Java computation"
        />

        <MetricCard
          title="Python Operation"
          value={benchmark.pythonOperation}
          unit=""
          icon="OP"
          description="Executed Python expression"
        />

        <MetricCard
          title="Java Result"
          value={String(benchmark.javaResult)}
          unit=""
          icon="OK"
          description="Result of Java computation"
        />
      </div>

      <div className="benchmark-disclaimer" role="note">
        <span className="disclaimer-icon">i</span>
        <p>
          Measurements are from a small computation. They are not REST API
          latency measurements or a general language performance comparison.
        </p>
      </div>
    </>
  )}

  {!benchmarkLoading && !benchmarkError && !benchmark && (
    <div className="benchmark-message">
      <strong>NO BENCHMARK DATA</strong>
      <p>No benchmark results are currently available.</p>
    </div>
  )}
</section>

      <section className="mission-control">
  <div className="panel-heading mission-heading">
    <div>
      <p className="eyebrow">TEMPORAL WORKFLOW / LIVE OPERATIONS</p>
      <h2>Mission Control</h2>
      <p className="mission-subtitle">
        Monitor and manage autonomous drone missions
      </p>
    </div>

    <button
      type="button"
      className="mission-start-button"
      onClick={startMission}
      disabled={missionLoading}
    >
      {missionLoading
        ? 'Starting Mission...'
        : `Start ${drones.length}-Drone Mission`}
    </button>
  </div>

  {missionMessage && (
    <p role="status" aria-live="polite" className="mission-message">
      {missionMessage}
    </p>
  )}

  {missionError && (
    <p role="alert" className="error-message">
      {missionError}
    </p>
  )}

  {missionLoading && (
    <p className="mission-loading">
      Starting mission. Please wait...
    </p>
  )}

  {workflowId ? (
    <>
      <div className="mission-overview">
        <div className="mission-info-card">
          <span className="mission-label">WORKFLOW ID</span>
          <code>{workflowId}</code>
        </div>

        <div className="mission-info-card">
          <span className="mission-label">EXECUTION STATUS</span>
          <strong className={`status-badge ${getStatusClass(workflowStatus)}`}>
            {workflowStatus || 'Loading...'}
          </strong>
        </div>

        <div className="mission-info-card">
          <span className="mission-label">ACTIVE DRONES</span>
          <strong>{Object.keys(missionSteps).length}</strong>
        </div>
      </div>

      {workflowId && (
  <div className="mission-analytics">
    <div className="mission-progress-heading">
      <div>
        <p className="eyebrow">OVERALL MISSION PROGRESS</p>
        <h3>{missionProgress}% Completed</h3>
      </div>
      <span>{completedSteps} / {totalSteps} STEPS</span>
    </div>

    <div className="mission-progress-track">
      <div
        className="mission-progress-fill"
        style={{ width: `${missionProgress}%` }}
      />
    </div>

    <div className="mission-counter-grid">
      <div className="mission-counter">
        <strong>{completedDrones}</strong>
        <span>Completed</span>
      </div>
      <div className="mission-counter">
        <strong>{activeDrones}</strong>
        <span>Active</span>
      </div>
      <div className="mission-counter">
        <strong>{failedDrones}</strong>
        <span>Failed</span>
      </div>
      <div className="mission-counter">
        <strong>{pendingDrones}</strong>
        <span>Pending</span>
      </div>
    </div>

    <div className="mission-section-heading">
      <div>
        <p className="eyebrow">FLEET EXECUTION</p>
        <h3>Drone Workflows</h3>
      </div>
      <span className="fleet-count">
        {Object.keys(missionSteps).length} DRONES
      </span>
    </div>

    <input
      type="search"
      className="drone-search"
      placeholder="Search drone ID..."
      value={droneSearch}
      onChange={event => setDroneSearch(event.target.value)}
    />

    <div className="compact-drone-list">
      {filteredDrones.map(([droneId, steps]) => (
        <details className="compact-drone-card" key={droneId}>
          <summary>
            <strong>{droneId}</strong>
            <span>
              {Object.values(steps).filter(
                status => status === 'Completed'
              ).length} / 4 steps
            </span>
            <span className="drone-expand">＋</span>
          </summary>

          <div className="compact-drone-steps">
            {[
              'Takeoff',
              'Navigate',
              'Drop Package',
              'Return to Base'
            ].map(step => (
              <div className="compact-step-row" key={step}>
                <span>{step}</span>
                <strong className={`status-badge ${getStatusClass(steps[step])}`}>
                  {steps[step] || 'Pending'}
                </strong>
              </div>
            ))}
          </div>
        </details>
      ))}

      {filteredDrones.length === 0 && (
        <p className="mission-empty-message">No matching drones found.</p>
      )}
    </div>
  </div>
)}

      
    </>
  ) : (
    <div className="mission-empty-state">
      <span className="mission-empty-icon">⌁</span>
      <h3>No Active Mission</h3>
      <p>
        Start a drone mission to monitor workflow execution and
        individual drone progress here.
      </p>
    </div>
  )}
</section>
            
      <section className="workspace fleet-workspace">
  <div className="map-panel">
    <div className="panel-heading">
      <div>
        <p className="eyebrow">AEROSAGA / LIVE OPERATIONS</p>
        <h2>Fleet Command Map</h2>
        <p className="map-subtitle">
          Real-time simulated drone positioning and fleet monitoring
        </p>
      </div>

      <span className="live-map-status">
        <span className="live-dot" />
        LIVE SIMULATION
      </span>
    </div>

    <div className="map-toolbar">
      <div className="map-toolbar-item">
        <span className="map-indicator indicator-green" />
        <span>Active Fleet</span>
        <strong>{drones.length}</strong>
      </div>

      <div className="map-toolbar-item">
        <span className="map-indicator indicator-cyan" />
        <span>Selected Drone</span>
        <strong>{selectedDrone?.id || 'None'}</strong>
      </div>
    </div>

    {globeError && (
      <p className="error-message">{globeError}</p>
    )}

    <div className="globe-container">
      <div className="globe" ref={globeRef} />

      <div className="map-overlay map-overlay-top">
        <span>INDIA REGION</span>
        <span>GPS TRACKING</span>
      </div>

      {selectedDrone && (
  <div className="selected-drone-overlay">
    <div className="telemetry-card-header">
      <div>
        <p className="eyebrow">LIVE TELEMETRY</p>
        <strong>{selectedDrone.name}</strong>
        <span>{selectedDrone.id}</span>
      </div>

      <span
        className={`status ${
          (selectedDrone.status || 'Unknown')
            .toLowerCase()
            .replace(' ', '-')
        }`}
      >
        {selectedDrone.status || 'Unknown'}
      </span>
    </div>

    <div className="telemetry-card-battery">
      <div>
        <span>Battery level</span>
        <strong>{selectedDrone.battery}%</strong>
      </div>

      <div className="battery-track">
        <div
          className="battery-fill"
          style={{ width: `${selectedDrone.battery}%` }}
        />
      </div>
    </div>

    <div className="telemetry-card-grid">
      <div>
        <span>LATITUDE</span>
        <strong>{selectedDrone.lat.toFixed(4)}°</strong>
      </div>

      <div>
        <span>LONGITUDE</span>
        <strong>{selectedDrone.lon.toFixed(4)}°</strong>
      </div>

      <div>
        <span>ALTITUDE</span>
        <strong>
          {selectedDrone.altitude ?? 120} m
        </strong>
      </div>

      <div>
        <span>CONNECTION</span>
        <strong>
          {selectedDrone.simulated ? 'SIMULATED' : 'TELEMETRY'}
        </strong>
      </div>
    </div>
  </div>
)}
    </div>

    <div className="map-footer">
      <span>SIMULATED GPS UPDATES</span>
      <span>NOT CONNECTED TO REAL DRONES</span>
    </div>
  </div>

  <aside className="fleet-panel">
    <div className="panel-heading">
      <div>
        <p className="eyebrow">MISSION RESOURCES</p>
        <h2>Drone Fleet</h2>
      </div>

      <span className="fleet-count">
        {String(drones.length).padStart(2, '0')} UNITS
      </span>
    </div>

    <div className="fleet-list-heading">
      <span>AVAILABLE UNITS</span>
      <span>{drones.length} TOTAL</span>
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
            <span>Battery Level</span>
            <strong>{drone.battery}%</strong>
          </div>

          <div className="battery-track">
            <div
              className="battery-fill"
              style={{ width: `${drone.battery}%` }}
            />
          </div>

          <div className="coordinates">
            LAT {drone.lat.toFixed(4)}° &nbsp; LONG {drone.lon.toFixed(4)}°
          </div>
        </button>
      ))}
    </div>

    <div className="workflow-note">
      <span className="workflow-icon">↗</span>
      <div>
        <strong>Temporal Workflow Engine</strong>
        <p>
          Mission execution and recovery are managed by the Temporal backend.
        </p>
      </div>
    </div>
  </aside>
</section>

      <section className="editor-panel">
  <div className="panel-heading editor-heading">
    <div>
      <p className="eyebrow">AEROSAGA / DEVELOPER CONSOLE</p>
      <h2>Mission Code Workspace</h2>
      <p className="editor-subtitle">
        Multi-language environment for mission development
      </p>
    </div>

    <span className="editor-live-tag">
      <span className="live-dot connected" />
      EDITOR READY
    </span>
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
      <span>↺</span> Reset Code
    </button>
  </div>

  <div className="editor-filebar">
    <div className="editor-file-info">
      <span className="file-indicator">●</span>
      <strong>mission{currentLanguage.extension}</strong>
    </div>

    <span className="editor-status">
      EDITABLE / LOCAL PREVIEW
    </span>
  </div>

  <div className="monaco-container">
    <Editor
      height="400px"
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
        padding: { top: 18 },
        tabSize: 4,
        wordWrap: 'on',
        lineNumbers: 'on',
        renderLineHighlight: 'all',
        roundedSelection: true,
        bracketPairColorization: { enabled: true },
        cursorBlinking: 'smooth',
        smoothScrolling: true,
        suggestOnTriggerCharacters: true
      }}
    />
  </div>

  <div className="editor-footer">
    <div>
      <span className="editor-footer-dot" />
      {currentLanguage.label}
    </div>

    <span>UTF-8</span>
    <span>Execution not connected</span>
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
