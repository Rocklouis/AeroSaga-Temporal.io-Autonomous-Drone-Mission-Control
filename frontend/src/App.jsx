
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

const initialDrones = [
  { id: 'DR-001', name: 'Drone Alpha', status: 'In Flight', lat: 22.5726, lon: 88.3639, battery: 86 },
  { id: 'DR-002', name: 'Drone Bravo', status: 'Ready', lat: 28.6139, lon: 77.2090, battery: 100 },
  { id: 'DR-003', name: 'Drone Charlie', status: 'Returning', lat: 19.0760, lon: 72.8777, battery: 42 }
]

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

function App() {
  const globeRef = useRef(null)
  const viewerRef = useRef(null)

  const [selectedDrone, setSelectedDrone] = useState(null)
  const [drones, setDrones] = useState(initialDrones)
  const [telemetryStatus, setTelemetryStatus] = useState('Connecting')
  const [globeError, setGlobeError] = useState('')
  const [activeLanguage, setActiveLanguage] = useState('java')
  const [codeByLanguage, setCodeByLanguage] = useState(starterCode)

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

        for (const drone of initialDrones) {
          viewer.entities.add({
            id: `drone-${drone.id}`,
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
              pixelOffset: new Cartesian3(0, -20, 0)
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
          // The globe and drone markers work without 3D buildings.
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

  useEffect(() => {
    const socket = new WebSocket('ws://localhost:8081/ws/telemetry')
    let intentionalClose = false

    socket.onopen = () => setTelemetryStatus('Connected')

    socket.onmessage = event => {
      try {
        const telemetry = JSON.parse(event.data)
        if (!Array.isArray(telemetry)) return

        setDrones(previous => telemetry.map(update => {
          const existing = previous.find(drone => drone.id === update.id)
          return {
            ...(existing ?? { id: update.id, name: `Drone ${update.id}` }),
            lat: Number(update.lat),
            lon: Number(update.lon),
            altitude: Number(update.altitude ?? 120)
          }
        }))

        const viewer = viewerRef.current
        if (viewer && !viewer.isDestroyed()) {
          for (const update of telemetry) {
            const entity = viewer.entities.getById(`drone-${update.id}`)
            if (entity) {
              entity.position = Cartesian3.fromDegrees(
                Number(update.lon),
                Number(update.lat),
                Number(update.altitude ?? 120)
              )
            }
          }
        }
      } catch (error) {
        console.error('Could not parse AeroSaga telemetry:', error)
      }
    }

    socket.onerror = () => setTelemetryStatus('Connection error')

    socket.onclose = () => {
      if (!intentionalClose) setTelemetryStatus('Disconnected')
    }

    return () => {
      intentionalClose = true
      socket.close()
    }
  }, [])

  function focusDrone(drone) {
    setSelectedDrone(drone)
    viewerRef.current?.camera.flyTo({
      destination: Cartesian3.fromDegrees(drone.lon, drone.lat, 450000)
    })
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

  return (
    <main className="dashboard">
      <header className="topbar">
        <div>
          <p className="eyebrow">AUTONOMOUS FLIGHT OPERATIONS</p>
          <h1>AeroSaga <span>Mission Control</span></h1>
        </div>
        <div className="connection">
          <span className={`live-dot ${telemetryStatus === 'Connected' ? 'connected' : ''}`} />
          TELEMETRY {telemetryStatus.toUpperCase()}
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
            <span className="demo-tag">LIVE SIMULATED GPS</span>
          </div>

          {globeError && <p className="error-message">{globeError}</p>}
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
            <span className="fleet-count">03 UNITS</span>
          </div>

          <div className="drone-list">
            {drones.map(drone => (
              <button
                type="button"
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
              <p>Simulated takeoff workflow implemented in the backend.</p>
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
          <div className="language-tabs" role="tablist" aria-label="Programming language">
            {languages.map(language => (
              <button
                key={language.id}
                type="button"
                role="tab"
                aria-selected={activeLanguage === language.id}
                className={`language-tab ${activeLanguage === language.id ? 'active' : ''}`}
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
          <span className="editor-status">Editable · Local preview</span>
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
