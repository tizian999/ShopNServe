import React, { useEffect, useState, useMemo } from "react";
import ReactFlow, {
    Background,
    Controls,
    MiniMap,
} from "reactflow";
import "reactflow/dist/style.css";
import "./App.css";
import VueShopWrapper from './VueShopWrapper.jsx'
import PostItNode from './PostItNode.jsx'
import dagre from 'dagre'

function LiveBlackboardView() {
    const [graph, setGraph] = useState({ nodes: [], links: [] });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [showMessages, setShowMessages] = useState(false);
    const [showCapabilities, setShowCapabilities] = useState(true);
    const [layoutDirection, setLayoutDirection] = useState('LR');

    useEffect(() => {
        let cancelled = false;

        async function load() {
            try {
                const res = await fetch("/api/graph/blackboard");
                if (!res.ok) throw new Error("HTTP " + res.status);
                const data = await res.json();
                if (!cancelled) {
                    setGraph(data);
                    setLoading(false);
                    setError(null);
                }
            } catch (err) {
                if (!cancelled) {
                    console.error("Error loading graph", err);
                    setError(err.message);
                    setLoading(false);
                }
            }
        }

        load();
        const interval = setInterval(load, 2000);
        return () => {
            cancelled = true;
            clearInterval(interval);
        };
    }, []);

    const { nodes, edges } = useMemo(() => {
        const rawNodes = graph.nodes || [];
        const rawLinks = graph.links || [];

        // Filter optional Messages aus
        const filteredNodes = rawNodes.filter(n => {
            if (n.type === 'Message' && !showMessages) return false;
            if (n.type === 'Capability' && !showCapabilities) return false;
            return true;
        });

        const filteredLinks = rawLinks.filter(l => {
            const fromOk = filteredNodes.find(n => String(n.id) === String(l.from));
            const toOk = filteredNodes.find(n => String(n.id) === String(l.to));
            return fromOk && toOk;
        });

        // Gruppiere Capabilities: gleiche name zusammenfassen
        const capabilityByName = {};
        filteredNodes.forEach(n => {
            if (n.type === 'Capability') {
                capabilityByName[n.name] = capabilityByName[n.name] || { ...n, mergedIds: [] };
                capabilityByName[n.name].mergedIds.push(n.id);
            }
        });

        const microClients = filteredNodes.filter(n => n.type === 'MicroClient');
        const dedupCaps = Object.values(capabilityByName);

        // Maps für requires/provides pro MicroClient
        const requiresMap = {}; const providesMap = {};
        filteredLinks.forEach(l => {
            if (l.type === 'REQUIRES') {
                requiresMap[l.from] = requiresMap[l.from] || [];
                const capNode = filteredNodes.find(n => String(n.id) === String(l.to));
                if (capNode) requiresMap[l.from].push(capNode.name);
            }
            if (l.type === 'PROVIDES') {
                providesMap[l.from] = providesMap[l.from] || [];
                const capNode = filteredNodes.find(n => String(n.id) === String(l.to));
                if (capNode) providesMap[l.from].push(capNode.name);
            }
        });

        // Dagre Graph für Layout
        const g = new dagre.graphlib.Graph();
        g.setGraph({ rankdir: layoutDirection });
        g.setDefaultEdgeLabel(() => ({}));

        microClients.forEach(n => {
            g.setNode(n.id, { width: 180, height: 110 });
        });
        dedupCaps.forEach(n => {
            g.setNode(n.id, { width: 180, height: 100 });
        });
        filteredLinks.forEach(l => {
            g.setEdge(l.from, l.to);
        });
        dagre.layout(g);

        function dagrePos(id) {
            const nodeWithPos = g.node(id);
            return nodeWithPos ? { x: nodeWithPos.x - 90, y: nodeWithPos.y - 50 } : { x: 0, y: 0 };
        }

        const rfNodes = [];
        microClients.forEach(n => {
            const pos = dagrePos(n.id);
            rfNodes.push({ id: String(n.id), type: 'postIt', data: { label: n.name, role: 'MicroClient', requires: requiresMap[n.id] || [], provides: providesMap[n.id] || [] }, position: pos, draggable: false });
        });
        dedupCaps.forEach(n => {
            const pos = dagrePos(n.id);
            rfNodes.push({ id: String(n.id), type: 'postIt', data: { label: n.name, role: 'Capability', capabilityType: n.capabilityType }, position: pos, draggable: false });
        });

        // Edge Pfeilklassen
        const rfEdges = filteredLinks.map((l, idx) => ({
            id: `e-${idx}`,
            source: String(l.from),
            target: String(l.to),
            label: l.type === 'PROVIDES' || l.type === 'REQUIRES' ? l.type : '',
            animated: false,
            style: {},
            className: l.type === 'PROVIDES' ? 'provides' : (l.type === 'REQUIRES' ? 'requires' : ''),
            markerEnd: { type: 'arrowclosed', color: l.type === 'PROVIDES' ? '#22c55e' : (l.type === 'REQUIRES' ? '#3b82f6' : '#94a3b8'), width: 18, height: 18 }
        }));

        return { nodes: rfNodes, edges: rfEdges };
    }, [graph, showMessages, showCapabilities, layoutDirection]);

    if (loading) {
        return <div className="panel">Lade Blackboard-Graph…</div>;
    }

    if (error) {
        return (
            <div className="panel error">
                Fehler beim Laden des Graphen: {error}
            </div>
        );
    }

    return (
        <div className="graph-panel">
            <div className="graph-legend">
                <span className="badge client">MicroClient (Post-it)</span>
                <span className="badge cap-provides">Capability PROVIDED</span>
                <span className="badge cap-requires">Capability REQUIRED</span>
                <span className="badge arrow-provides">grün = PROVIDES</span>
                <span className="badge arrow-requires">blau = REQUIRES</span>
            </div>
            <div className="graph-controls">
                <div className="grp">
                    <label><input type="checkbox" checked={showCapabilities} onChange={e=>setShowCapabilities(e.target.checked)} /> Capabilities</label>
                    <label><input type="checkbox" checked={showMessages} onChange={e=>setShowMessages(e.target.checked)} /> Messages</label>
                </div>
                <div className="grp">
                    <label style={{fontWeight:600}}>Layout</label>
                    <select value={layoutDirection} onChange={e=>setLayoutDirection(e.target.value)}>
                        <option value="LR">Links→Rechts</option>
                        <option value="TB">Oben→Unten</option>
                    </select>
                </div>
            </div>
            <div style={{ height: "600px", borderRadius: 16, overflow: "hidden" }}>
                <ReactFlow nodes={nodes} edges={edges} fitView nodeTypes={{ postIt: PostItNode }} zoomOnScroll={false} panOnScroll={true} zoomOnPinch={false} minZoom={0.25} maxZoom={1.5}>
                    <Background />
                    <MiniMap />
                    <Controls />
                </ReactFlow>
            </div>
        </div>
    );
}

function App() {
    const [view, setView] = useState("shop"); // "shop" | "blackboard"

    return (
        <div className="app-root">
            <header className="app-header">
                <h1>ShopNServe Shell</h1>
                <p>Blackboard-basierte Microfrontend-Plattform</p>

                <div className="tabs">
                    <button
                        className={view === "shop" ? "tab active" : "tab"}
                        onClick={() => setView("shop")}
                    >
                        🛒 Shop (Vue Microclient)
                    </button>
                    <button
                        className={view === "blackboard" ? "tab active" : "tab"}
                        onClick={() => setView("blackboard")}
                    >
                        📌 Live Blackboard-Ansicht
                    </button>
                </div>
            </header>

            <main className="app-main">
                {view === "shop" && (
                    <div className="panel">
                        <h2>Shop-Ansicht</h2>
                        <p>
                            Eingebetteter <strong>Vue-Microclient</strong> (Shop.vue) direkt gemountet in React.
                        </p>
                        <VueShopWrapper />
                    </div>
                )}

                {view === "blackboard" && <LiveBlackboardView />}
            </main>
        </div>
    );
}

export default App;
