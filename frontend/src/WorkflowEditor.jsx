
import { useCallback, useState } from 'react'
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  Handle,
  Position,
  addEdge,
  useNodesState,
  useEdgesState
} from '@xyflow/react'

import '@xyflow/react/dist/style.css'
import './WorkflowEditor.css'

const initialNodes = [
  {
    id: 'start',
    type: 'missionNode',
    position: { x: 100, y: 180 },
    data: { label: 'Mission Start', category: 'TRIGGER', icon: '▶' }
  },
  {
    id: 'takeoff',
    type: 'missionNode',
    position: { x: 370, y: 180 },
    data: {
      label: 'Takeoff',
      category: 'FLIGHT',
      icon: '↗',
      altitude: 120
    }
  },
  {
    id: 'navigate',
    type: 'missionNode',
    position: { x: 640, y: 180 },
    data: {
      label: 'Navigate',
      category: 'FLIGHT',
      icon: '⌖',
      speed: 12,
      waypoint: 'WP-001'
    }
  },
  {
    id: 'drop',
    type: 'missionNode',
    position: { x: 910, y: 180 },
    data: {
      label: 'Drop Package',
      category: 'ACTION',
      icon: '□',
      packageId: 'PKG-001'
    }
  },
  {
    id: 'return',
    type: 'missionNode',
    position: { x: 1180, y: 180 },
    data: { label: 'Return to Base', category: 'FLIGHT', icon: '⌂' }
  }
]

const initialEdges = [
  { id: 'e1', source: 'start', target: 'takeoff', animated: true },
  { id: 'e2', source: 'takeoff', target: 'navigate', animated: true },
  { id: 'e3', source: 'navigate', target: 'drop', animated: true },
  { id: 'e4', source: 'drop', target: 'return', animated: true }
]

function MissionNode({ data, selected }) {
  return (
    <div className={`mission-node ${selected ? 'selected' : ''}`}>
      <Handle type="target" position={Position.Left} />

      <div className="node-category">{data.category}</div>

      <div className="node-content">
        <span className="node-icon">{data.icon}</span>
        <strong>{data.label}</strong>
      </div>

      <div className="node-footer">AEROSAGA WORKFLOW</div>

      <Handle type="source" position={Position.Right} />
    </div>
  )
}

const nodeTypes = {
  missionNode: MissionNode
}

const libraryItems = [
  { label: 'Takeoff', category: 'FLIGHT', icon: '↗' },
  { label: 'Navigate', category: 'FLIGHT', icon: '⌖' },
  { label: 'Orbit Area', category: 'FLIGHT', icon: '◎' },
  { label: 'Drop Package', category: 'ACTION', icon: '□' },
  { label: 'Return to Base', category: 'FLIGHT', icon: '⌂' }
]

export default function WorkflowEditor() {
  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes)
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges)
  const [selectedNodeId, setSelectedNodeId] = useState(null)

  const selectedNode = nodes.find(node => node.id === selectedNodeId)

  const onConnect = useCallback(
    connection => setEdges(current => addEdge(connection, current)),
    [setEdges]
  )

  function addMissionNode(item) {
    const id = `${item.label.toLowerCase().replaceAll(' ', '-')}-${Date.now()}`

    const newNode = {
      id,
      type: 'missionNode',
      position: {
        x: 180 + Math.random() * 500,
        y: 380 + Math.random() * 180
      },
      data: {
        label: item.label,
        category: item.category,
        icon: item.icon,
        ...(item.label === 'Takeoff' ? { altitude: 120 } : {}),
        ...(item.label === 'Navigate'
          ? { speed: 12, waypoint: 'WP-001' }
          : {}),
        ...(item.label === 'Drop Package'
          ? { packageId: 'PKG-001' }
          : {})
      }
    }

    setNodes(current => [...current, newNode])
    setSelectedNodeId(id)
  }

  function updateNodeProperty(property, value) {
    if (!selectedNodeId) return

    setNodes(current =>
      current.map(node =>
        node.id === selectedNodeId
          ? {
              ...node,
              data: {
                ...node.data,
                [property]: value
              }
            }
          : node
      )
    )
  }

  function renderNumberField(label, property, unit, min = 0, step = 1) {
    return (
      <label className="property-field" key={property}>
        <span className="property-label">{label}</span>
        <div className="property-input-row">
          <input
            type="number"
            min={min}
            step={step}
            value={selectedNode.data[property] ?? ''}
            onChange={event =>
              updateNodeProperty(property, event.target.value)
            }
          />
          <span>{unit}</span>
        </div>
      </label>
    )
  }

  function renderTextField(label, property) {
    return (
      <label className="property-field" key={property}>
        <span className="property-label">{label}</span>
        <input
          type="text"
          value={selectedNode.data[property] ?? ''}
          onChange={event =>
            updateNodeProperty(property, event.target.value)
          }
        />
      </label>
    )
  }

  return (
    <section className="workflow-editor">
      <header className="workflow-header">
        <div>
          <p className="workflow-eyebrow">AEROSAGA / MISSION DESIGNER</p>
          <h2>Visual Workflow Editor</h2>
        </div>

        <span className="workflow-status">
          <span className="workflow-status-dot" />
          DESIGN MODE
        </span>
      </header>

      <div className="workflow-layout">
        <aside className="node-library">
          <h3>NODE LIBRARY</h3>
          <p>ADD MISSION BLOCKS</p>

          {libraryItems.map(item => (
            <button
              type="button"
              className="library-item"
              key={item.label}
              onClick={() => addMissionNode(item)}
            >
              <span>{item.icon}</span>
              {item.label}
              <span className="library-plus">+</span>
            </button>
          ))}
        </aside>

        <div className="workflow-canvas">
          <ReactFlow
            nodes={nodes}
            edges={edges}
            nodeTypes={nodeTypes}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            onNodeClick={(_, node) => setSelectedNodeId(node.id)}
            onPaneClick={() => setSelectedNodeId(null)}
            fitView
            fitViewOptions={{ padding: 0.2 }}
          >
            <Background color="#263548" gap={22} size={1} />
            <Controls />
            <MiniMap
              nodeColor="#16b8c8"
              maskColor="rgba(5, 12, 24, 0.7)"
            />
          </ReactFlow>
        </div>

        <aside className="node-properties">
          <h3>PROPERTIES</h3>

          {selectedNode ? (
            <>
              <p className="property-label">SELECTED NODE</p>
              <strong>{selectedNode.data.label}</strong>

              <p className="property-label">NODE TYPE</p>
              <span>{selectedNode.data.category}</span>

              {selectedNode.data.altitude !== undefined &&
                renderNumberField('ALTITUDE', 'altitude', 'metres', 1)}

              {selectedNode.data.speed !== undefined &&
                renderNumberField('SPEED', 'speed', 'm/s', 1, 0.5)}

              {selectedNode.data.waypoint !== undefined &&
                renderTextField('TARGET WAYPOINT', 'waypoint')}

              {selectedNode.data.packageId !== undefined &&
                renderTextField('PACKAGE ID', 'packageId')}

              {selectedNode.data.label === 'Return to Base' && (
                <p className="properties-empty">
                  This node currently uses the configured base location.
                </p>
              )}
            </>
          ) : (
            <p className="properties-empty">
              Select a node on the canvas to view and edit its properties.
            </p>
          )}
        </aside>
      </div>
    </section>
  )
}
