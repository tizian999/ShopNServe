import React from 'react'
import { Handle, Position } from 'reactflow'

export default function PostItNode({ data }) {
  const { label, role, capabilityType, highlight, requires, provides } = data
  const isMicro = role === 'MicroClient'
  const isCap = role === 'Capability'
  return (
    <div
      data-role={role}
      data-capability-type={capabilityType || ''}
      className={['postit', highlight && 'new', isMicro && 'microclient', isCap && 'capability', isCap && capabilityType && capabilityType.toLowerCase()].filter(Boolean).join(' ')}>
      <div className="title">{label}</div>
      <div className="subtitle">{isMicro ? 'MicroClient' : (capabilityType ? capabilityType : 'Capability')}</div>
      <div className="badge-line">
        {isCap && capabilityType === 'PROVIDED' && <span className="mini-badge">PROVIDES</span>}
        {isCap && capabilityType === 'REQUIRED' && <span className="mini-badge">REQUIRES</span>}
        {isMicro && <span className="mini-badge">CLIENT</span>}
      </div>
      {requires && (
        <div className={['list-line', requires.length === 0 && 'empty'].filter(Boolean).join(' ')}>
          <span>Req:</span> {requires.length ? requires.slice(0,3).join(', ')+(requires.length>3?'…':'') : '—'}
        </div>
      )}
      {provides && (
        <div className={['list-line', provides.length === 0 && 'empty'].filter(Boolean).join(' ')}>
          <span>Prov:</span> {provides.length ? provides.slice(0,3).join(', ')+(provides.length>3?'…':'') : '—'}
        </div>
      )}
      {/* Handles for connections */}
      <Handle type="source" position={Position.Right} />
      <Handle type="target" position={Position.Left} />
    </div>
  )
}
