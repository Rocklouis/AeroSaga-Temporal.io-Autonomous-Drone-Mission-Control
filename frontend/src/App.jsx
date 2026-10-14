
import { useEffect, useRef, useState } from 'react'
import {
  Cartesian3,
  Color,
  createWorldTerrainAsync,
  Ion,
  Viewer,
  createOsmBuildingsAsync
} from 'cesium'
import 'cesium/Build/Cesium/Widgets/widgets.css'
import './App.css'

const drones = [
  { id: 'DR-001', name: 'Drone Alpha', status: 'In Flight', lat: 22.5726, lon: 88.3639, battery: 86 },
  { id: 'DR-002', name: 'Drone Bravo', status: 'Ready', lat: 28.6139, lon: 77.2090, battery: 100 },
  { id: 'DR-003', name: 'Drone Charlie', status: 'Returning', lat: 19.0760, lon: 72.8777, battery: 42 }
]

function App() {
  const globeRef = useRef(null)
  const viewerRef = useRef(null)
  const [selectedDrone, setSelectedDrone] = useState(null)
  const [globeError, setGlobeError] = useState('')

  useEffect(() => {
    let cancelled = false
    let viewer

    async function initializeGlobe() {
      try {
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
          terrainProvider: await createWorldTerrainAsync()
        })

        if (cancelled) {
          viewer.destroy()
          return
        }

        viewerRef.current = viewer

        for (const drone of drones) {
          viewer.entities.add({
            name: drone.name,
            position: Cartesian3.fromDegrees(drone.lon, drone.lat, 120),
            point: {
              pixelSize: 13,
              color: drone.status === 'Returning' ? Color.ORANGE : Color.CYAN,
              outlineColor: Color.WHITE,
              outlineWidth: 2
            },
            label: {
              text: drone.id,
              font: '13px sans-serif',
              fillColor: Color.WHITE,
              outlineColor: Color.BLACK,
              outlineWidth: 3,
              style: 2,
              pixelOffset: new Cartesian3(0, -20, 0),
              distanceDisplayCondition: undefined
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
          // The globe and drone markers can work without 3D buildings.
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

  function focusDrone(drone) {
    setSelectedDrone(drone)
    viewerRef.current?.camera.flyTo({
      destination: Cartesian3.fromDegrees(drone.lon, drone.lat, 450000)
    })
  }

  return (
    <main className="dashboard">
      <header className="topbar">
        <div>
          <p className="eyebrow">AUTONOMOUS FLIGHT OPERATIONS</p>
          <h1>AeroSaga <span>Mission Control</span></h1>
        </div>
        <div className="connection">
          <span className="live-dot" />
          DEMO TELEMETRY
        </div>
      </header>

      <section className="summary">
        <article className="summary-card">
          <span>Total fleet</span>
          <strong>03</strong>
        </article>
        <article className="summary-card">
          <span>In flight</span>
          <strong>01</strong>
        </article>
        <article className="summary-card">
          <span>Ready</span>
          <strong>01</strong>
        </article>
        <article className="summary-card">
          <span>Returning</span>
          <strong>01</strong>
        </article>
      </section>

      <section className="workspace">
        <div className="map-panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">LIVE OPERATIONS / INDIA</p>
              <h2>Fleet overview</h2>
            </div>
            <span className="demo-tag">STATIC DEMO</span>
          </div>

          {globeError && <p className="error-message">{globeError}</p>}
          <div className="globe" ref={globeRef} />
          <p className="map-note">
            Sample locations only · Not connected to real drones
          </p>
        </div>

        <aside className="fleet-panel">
          <div className="panel-heading">
            <div>
              <p className="eyebrow">MISSION RESOURCES</p>
              <h2>Drone fleet</h2>
            </div>
            <span className="fleet-count">03 UNITS</span>
          </div>

          <div className="drone-list">
            {drones.map((drone) => (
              <button
                className={`drone-card ${selectedDrone?.id === drone.id ? 'selected' : ''}`}
                key={drone.id}
                onClick={() => focusDrone(drone)}
              >
                <div className="drone-card-top">
                  <div className="drone-icon">✦</div>
                  <div className="drone-title">
                    <strong>{drone.name}</strong>
                    <span>{drone.id}</span>
                  </div>
                  <span className={`status ${drone.status.toLowerCase().replace(' ', '-')}`}>
                    {drone.status}
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
                  {drone.lat.toFixed(4)}° N, {drone.lon.toFixed(4)}° E
                </div>
              </button>
            ))}
          </div>

          <div className="workflow-note">
            <span className="workflow-icon">↗</span>
            <div>
              <strong>Temporal workflows</strong>
              <p>Workflow integration is the next backend milestone.</p>
            </div>
          </div>
        </aside>
      </section>

      <footer>
        AEROSAGA / WEEK 1 PROTOTYPE <span>3D FLEET VISUALIZATION</span>
      </footer>
    </main>
  )
}

export default App
