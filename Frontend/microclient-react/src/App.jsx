import React, { useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import {
    AppBar,
    Toolbar,
    Typography,
    Box,
    Card,
    CardContent,
    Divider,
    List,
    ListItem,
    ListItemText,
    Stack,
    Chip,
    Switch,
    FormControlLabel,
    Button,
    Alert,
} from "@mui/material";
import RefreshIcon from "@mui/icons-material/Refresh";
import Xarrow, { Xwrapper, useXarrow } from "react-xarrows";

const COLORS = {
    primary: "#7c3aed",
    backgroundDefault: "#07070a",
    cardPaper: "#0f1724",
    textPrimary: "#e6e6fa",
    textSecondary: "rgba(230,230,250,0.72)",
    border: "rgba(255,255,255,0.08)",
    chipText: "#ffffff",
    subtleBg: "rgba(255,255,255,0.03)",
    rel: {
        PROVIDES: "#9f7aea",
        REQUIRES: "#7c3aed",
        COMMUNICATES_WITH: "rgba(230,230,250,0.6)",
        PART_OF: "rgba(230,230,250,0.45)",
        HAS_EVENT: "rgba(230,230,250,0.35)",
        ABOUT: "rgba(230,230,250,0.35)",
        HANDLED_BY: "rgba(230,230,250,0.45)",
        SENDS: "rgba(230,230,250,0.5)",
    },
    node: {
        UIComponent: "rgba(124,58,237,0.15)",
        BackendComponent: "rgba(159,122,234,0.14)",
        Capability: "rgba(230,230,250,0.07)",
        MicroClient: "rgba(230,230,250,0.06)",
        MessageEvent: "rgba(230,230,250,0.05)",
        Trace: "rgba(230,230,250,0.08)",
        Default: "rgba(230,230,250,0.06)",
    },
};

function relChip(relType) {
    const bg = (COLORS.rel && COLORS.rel[relType]) ? COLORS.rel[relType] : "rgba(230,230,250,0.25)";
    return {
        bgcolor: bg,
        color: COLORS.chipText,
        borderRadius: 999,
        fontWeight: 800,
        border: `1px solid ${COLORS.border}`,
    };
}

function nodeAccent(type) {
    return (COLORS.node && COLORS.node[type]) ? COLORS.node[type] : COLORS.node.Default;
}

function formatTimestamp(timestamp) {
    if (!timestamp) return "—";
    const date = new Date(timestamp);
    if (Number.isNaN(date.getTime())) return String(timestamp);
    return date.toLocaleString("de-DE", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
        hour12: false,
    });
}

function buildTreeLayout(nodes, edges, showMessageNodes) {
    const NODE_W = 320;
    const COL_GAP = 140;
    const ROW_GAP = 190;
    const PAD_X = 24;
    const PAD_Y = 24;

    const baseNodes = (nodes || []).filter((n) => (showMessageNodes ? true : n.type !== "MessageEvent"));
    const byId = new Map(baseNodes.map((n) => [n.id, n]));

    const safeEdges = (edges || []).filter((e) => byId.has(e.from) && byId.has(e.to));

    const out = new Map();
    const indeg = new Map();
    for (const n of baseNodes) {
        out.set(n.id, []);
        indeg.set(n.id, 0);
    }
    for (const e of safeEdges) {
        out.get(e.from).push(e.to);
        indeg.set(e.to, (indeg.get(e.to) || 0) + 1);
    }

    const traceNode = baseNodes.find((n) => n.type === "Trace");
    let rootId =
        (traceNode && traceNode.id) ||
        (baseNodes.find((n) => (indeg.get(n.id) || 0) === 0) || {}).id ||
        (baseNodes[0] || {}).id ||
        null;

    if (!rootId) return { nodes: [], width: "100%", height: "100%" };

    const layerOf = new Map();
    const q = [rootId];
    layerOf.set(rootId, 0);

    while (q.length) {
        const cur = q.shift();
        const curL = layerOf.get(cur) || 0;
        for (const nxt of out.get(cur) || []) {
            if (!layerOf.has(nxt)) {
                layerOf.set(nxt, curL + 1);
                q.push(nxt);
            }
        }
    }

    let maxLayer = 0;
    for (const v of layerOf.values()) maxLayer = Math.max(maxLayer, v);
    for (const n of baseNodes) {
        if (!layerOf.has(n.id)) {
            maxLayer += 1;
            layerOf.set(n.id, maxLayer);
        }
    }

    const layers = new Map();
    for (const n of baseNodes) {
        const L = layerOf.get(n.id) || 0;
        if (!layers.has(L)) layers.set(L, []);
        layers.get(L).push(n.id);
    }

    const typeRank = (t) => {
        if (t === "Trace") return 0;
        if (t === "UIComponent") return 1;
        if (t === "MicroClient") return 2;
        if (t === "BackendComponent") return 3;
        if (t === "Capability") return 4;
        if (t === "MessageEvent") return 5;
        return 6;
    };

    for (const [L, ids] of layers.entries()) {
        ids.sort((a, b) => {
            const A = byId.get(a);
            const B = byId.get(b);
            const r = typeRank(A.type) - typeRank(B.type);
            if (r !== 0) return r;
            return (A.label || "").localeCompare(B.label || "");
        });
    }

    const positioned = [];
    const layerKeys = Array.from(layers.keys()).sort((a, b) => a - b);
    const maxRows = Math.max(...layerKeys.map((L) => layers.get(L).length), 1);

    for (const L of layerKeys) {
        const ids = layers.get(L);
        const layerCount = ids.length;
        const topOffset = PAD_Y + ((maxRows - layerCount) * ROW_GAP) / 2;

        ids.forEach((id, row) => {
            const n = byId.get(id);
            positioned.push({
                ...n,
                style: {
                    position: "absolute",
                    left: `${PAD_X + L * (NODE_W + COL_GAP)}px`,
                    top: `${topOffset + row * ROW_GAP}px`,
                    width: NODE_W,
                },
            });
        });
    }

    const width = `${PAD_X * 2 + layerKeys.length * (NODE_W + COL_GAP)}px`;
    const height = `${PAD_Y * 2 + maxRows * ROW_GAP}px`;

    return { nodes: positioned, width, height };
}

function GraphPanel({
                        traceId,
                        setTraceId,
                        traceLabel,
                        graph,
                        showMessageNodes,
                        updateXarrow,
                        loading,
                    }) {
    const layout = useMemo(
        () => buildTreeLayout(graph.nodes, graph.edges, showMessageNodes),
        [graph.nodes, graph.edges, showMessageNodes]
    );

    const viewportRef = useRef(null);
    const [scale, setScale] = useState(1);

    useLayoutEffect(() => {
        const el = viewportRef.current;
        if (!el) return;

        const getPx = (v) => {
            if (!v) return 0;
            const n = typeof v === "number" ? v : parseFloat(String(v).replace("px", ""));
            return Number.isFinite(n) ? n : 0;
        };

        const updateScale = () => {
            const cw = el.clientWidth || 1;
            const lw = getPx(layout.width) || 1;
            const maxW = Math.max(1, cw - 24);
            const s = Math.min(1, maxW / lw);
            setScale(s);
        };

        updateScale();
        const ro = new ResizeObserver(updateScale);
        ro.observe(el);
        return () => ro.disconnect();
    }, [layout.width]);

    useLayoutEffect(() => {
        const t = setTimeout(updateXarrow, 150);
        return () => clearTimeout(t);
    }, [layout.nodes, graph.edges, updateXarrow, scale]);

    return (
        <Card sx={{ bgcolor: "rgba(0,0,0,0.12)", border: `1px solid ${COLORS.border}`, borderRadius: 3, overflow: "hidden" }}>
            <CardContent sx={{ pb: 2 }}>
                <Stack direction="row" alignItems="center" justifyContent="space-between" gap={2}>
                    <Stack spacing={0.25}>
                        <Typography variant="h6" sx={{ fontWeight: 900, color: COLORS.textPrimary }}>
                            {traceLabel || "Trace Graph"}
                        </Typography>
                        <Typography variant="caption" sx={{ color: COLORS.textSecondary }}>
                            {traceId || "—"}
                        </Typography>
                    </Stack>

                    <Stack direction="row" alignItems="center" gap={1}>
                        <input
                            type="text"
                            value={traceId}
                            onChange={(e) => setTraceId(e.target.value)}
                            placeholder="trace id"
                            style={{
                                padding: "4px 10px",
                                borderRadius: 6,
                                border: `1px solid ${COLORS.border}`,
                                background: COLORS.subtleBg,
                                color: COLORS.textPrimary,
                                fontWeight: 700,
                                fontSize: 14,
                                outline: "none",
                                minWidth: 260,
                            }}
                        />
                        <Chip
                            size="small"
                            label={loading ? "Loading…" : `Nodes ${graph.nodes.length} · Edges ${graph.edges.length}`}
                            sx={{ bgcolor: COLORS.subtleBg, color: COLORS.textPrimary, border: `1px solid ${COLORS.border}`, fontWeight: 800 }}
                        />
                    </Stack>
                </Stack>

                <Divider sx={{ my: 1.5, borderColor: COLORS.border }} />

                <Box
                    ref={viewportRef}
                    sx={{
                        width: "100%",
                        height: 540,
                        overflowY: "auto",
                        overflowX: "hidden",
                        borderRadius: 2,
                    }}
                >
                    <Box
                        sx={{
                            position: "relative",
                            width: layout.width,
                            height: layout.height,
                            minHeight: 540,
                            transform: `scale(${scale})`,
                            transformOrigin: "top left",
                        }}
                    >
                        <Xwrapper>
                            {layout.nodes.map((node) => (
                                <Card
                                    id={node.id}
                                    key={node.id}
                                    sx={{
                                        bgcolor: COLORS.cardPaper,
                                        color: COLORS.textPrimary,
                                        borderRadius: 3,
                                        border: `1px solid ${COLORS.border}`,
                                        boxShadow: "0 10px 30px rgba(0,0,0,0.55)",
                                        ...node.style,
                                    }}
                                >
                                    <CardContent sx={{ p: 2 }}>
                                        <Stack direction="row" alignItems="center" justifyContent="space-between" spacing={1}>
                                            <Chip
                                                size="small"
                                                label={node.type}
                                                sx={{
                                                    bgcolor: nodeAccent(node.type),
                                                    color: COLORS.textPrimary,
                                                    border: `1px solid ${COLORS.border}`,
                                                    fontWeight: 800,
                                                }}
                                            />
                                            <Typography variant="caption" sx={{ color: COLORS.textSecondary }}>
                                                {node.id}
                                            </Typography>
                                        </Stack>

                                        <Typography variant="h6" sx={{ mt: 1, lineHeight: 1.2 }}>
                                            {node.label}
                                        </Typography>
                                    </CardContent>
                                </Card>
                            ))}

                            {graph.edges
                                .filter((e) => layout.nodes.some((n) => n.id === e.from) && layout.nodes.some((n) => n.id === e.to))
                                .map((edge, i) => (
                                    <Xarrow
                                        key={`${edge.from}-${edge.to}-${edge.type}-${i}`}
                                        start={edge.from}
                                        end={edge.to}
                                        color={(COLORS.rel && COLORS.rel[edge.type]) ? COLORS.rel[edge.type] : COLORS.textSecondary}
                                        strokeWidth={2}
                                        headSize={5}
                                        path="smooth"
                                        dashness={edge.type === "COMMUNICATES_WITH" || edge.type === "PART_OF"}
                                        labels={
                                            <div
                                                style={{
                                                    padding: "2px 8px",
                                                    borderRadius: 999,
                                                    background: "rgba(0,0,0,0.55)",
                                                    border: `1px solid ${COLORS.border}`,
                                                    color: COLORS.textPrimary,
                                                    fontSize: 12,
                                                    fontWeight: 800,
                                                }}
                                            >
                                                {edge.type}
                                            </div>
                                        }
                                    />
                                ))}
                        </Xwrapper>
                    </Box>
                </Box>
            </CardContent>
        </Card>
    );
}

export default function App() {
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);

    const [autoRefresh, setAutoRefresh] = useState(true);
    const [showMessageNodes, setShowMessageNodes] = useState(false);

    const [traces, setTraces] = useState([]);
    const [selectedTraceId, setSelectedTraceId] = useState("");
    const [liveNewestTrace, setLiveNewestTrace] = useState(true);

    const [graph, setGraph] = useState({ nodes: [], edges: [] });
    const [traceLabelById, setTraceLabelById] = useState({});

    const updateXarrow = useXarrow();

    const deriveLabelFromGraph = (g) => {
        const nodes = Array.isArray(g?.nodes) ? g.nodes : [];
        const ui = nodes.find((n) => n?.type === "UIComponent" && n?.label);
        const raw = ui?.label ? String(ui.label) : "Unknown Trace";
        return raw.replace(".vue", "");
    };

    const loadGraph = async (traceId) => {
        if (!traceId || String(traceId).trim() === "") {
            setGraph({ nodes: [], edges: [] });
            return;
        }
        const url = "/api/blackboard/graph?traceId=" + encodeURIComponent(String(traceId).trim());
        const res = await fetch(url);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        setGraph({
            nodes: Array.isArray(data?.nodes) ? data.nodes : [],
            edges: Array.isArray(data?.edges) ? data.edges : [],
        });
    };

    const loadTracesAndLabels = async () => {
        const res = await fetch("/api/blackboard/traces");
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        const arr = Array.isArray(data) ? data : [];
        setTraces(arr);

        if (arr.length > 0) {
            setSelectedTraceId((prev) => {
                if (liveNewestTrace) return String(arr[0].id);
                return prev && String(prev).trim() !== "" ? prev : String(arr[0].id);
            });
        }

        const existing = traceLabelById || {};
        const need = arr.map((t) => String(t.id)).filter((tid) => tid && !existing[tid]);
        if (need.length === 0) return;

        const CONCURRENCY = 6;
        const queue = [...need];
        const nextLabels = { ...existing };

        const worker = async () => {
            while (queue.length) {
                const tid = queue.shift();
                try {
                    const r = await fetch("/api/blackboard/graph?traceId=" + encodeURIComponent(tid));
                    if (!r.ok) throw new Error(`graph ${tid} -> HTTP ${r.status}`);
                    const g = await r.json();
                    nextLabels[tid] = deriveLabelFromGraph(g);
                } catch {
                    nextLabels[tid] = "Unknown Trace";
                }
            }
        };

        await Promise.all(Array.from({ length: Math.min(CONCURRENCY, queue.length) }, worker));
        setTraceLabelById(nextLabels);
    };

    const reloadAll = async () => {
        try {
            setLoading(true);
            setError(null);
            await loadTracesAndLabels();
        } catch (e) {
            setError(String(e?.message ?? e));
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        reloadAll();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        (async () => {
            try {
                setLoading(true);
                setError(null);
                await loadGraph(selectedTraceId);
            } catch (e) {
                setError(String(e?.message ?? e));
            } finally {
                setLoading(false);
            }
        })();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [selectedTraceId]);

    useEffect(() => {
        if (!autoRefresh) return;
        const interval = setInterval(async () => {
            try {
                await loadTracesAndLabels();
                if (!liveNewestTrace) await loadGraph(selectedTraceId);
            } catch {
                // ignore
            }
        }, 2000);
        return () => clearInterval(interval);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [autoRefresh, selectedTraceId, liveNewestTrace]);

    const selectedLabel = selectedTraceId ? (traceLabelById[String(selectedTraceId)] || "Trace Graph") : "Trace Graph";

    return (
        <Box
            sx={{
                fontFamily: "system-ui",
                bgcolor: COLORS.backgroundDefault,
                color: COLORS.textPrimary,
                minHeight: "100vh",
                display: "flex",
                flexDirection: "column",
                overflowX: "hidden",
            }}
        >
            <AppBar position="static" sx={{ backgroundColor: COLORS.primary, boxShadow: "0 1px 10px rgba(0,0,0,0.6)" }}>
                <Toolbar sx={{ gap: 2 }}>
                    <Typography variant="h6" sx={{ color: COLORS.chipText, fontWeight: 900 }}>
                        Live Blackboard
                    </Typography>

                    <Box sx={{ flexGrow: 1 }} />

                    <Stack direction="row" spacing={1} alignItems="center">
                        <FormControlLabel
                            control={<Switch checked={autoRefresh} onChange={(e) => setAutoRefresh(e.target.checked)} size="small" />}
                            label={<Typography variant="body2" sx={{ color: COLORS.chipText }}>Auto Refresh</Typography>}
                        />
                        <FormControlLabel
                            control={<Switch checked={showMessageNodes} onChange={(e) => setShowMessageNodes(e.target.checked)} size="small" />}
                            label={<Typography variant="body2" sx={{ color: COLORS.chipText }}>Show Message Nodes</Typography>}
                        />
                        <Button
                            onClick={reloadAll}
                            startIcon={<RefreshIcon />}
                            variant="contained"
                            disabled={loading}
                            sx={{
                                bgcolor: "rgba(255,255,255,0.18)",
                                color: COLORS.chipText,
                                fontWeight: 900,
                                borderRadius: 2,
                                "&:hover": { bgcolor: "rgba(255,255,255,0.24)" },
                            }}
                        >
                            Reload
                        </Button>
                    </Stack>
                </Toolbar>
            </AppBar>

            <Box sx={{ p: 2, flexGrow: 1, overflow: "hidden" }}>
                {error && (
                    <Alert
                        severity="error"
                        sx={{
                            mb: 2,
                            borderRadius: 2,
                            backgroundColor: "rgba(239,68,68,0.12)",
                            color: COLORS.textPrimary,
                            border: `1px solid rgba(239,68,68,0.35)`,
                        }}
                    >
                        {error}
                    </Alert>
                )}

                <Box sx={{ display: "grid", gridTemplateColumns: "360px 1fr", gap: 2, height: "100%" }}>
                    <Card sx={{ bgcolor: COLORS.cardPaper, border: `1px solid ${COLORS.border}`, borderRadius: 3, overflow: "hidden" }}>
                        <CardContent>
                            <Stack direction="row" alignItems="center" justifyContent="space-between">
                                <Typography variant="h6" sx={{ fontWeight: 900, color: COLORS.textPrimary }}>
                                    Traces
                                </Typography>
                                <FormControlLabel
                                    control={<Switch checked={liveNewestTrace} onChange={(e) => setLiveNewestTrace(e.target.checked)} size="small" />}
                                    label={<Typography variant="caption" sx={{ color: COLORS.textSecondary }}>Live newest</Typography>}
                                />
                            </Stack>

                            <Divider sx={{ my: 1.5, borderColor: COLORS.border }} />

                            <List dense sx={{ maxHeight: 560, overflow: "auto" }}>
                                {traces.map((t) => {
                                    const tid = String(t.id);
                                    const label = traceLabelById[tid] || tid;
                                    return (
                                        <ListItem
                                            key={tid}
                                            onClick={() => {
                                                setLiveNewestTrace(false);
                                                setSelectedTraceId(tid);
                                            }}
                                            sx={{
                                                borderRadius: 2,
                                                mb: 0.5,
                                                border: `1px solid ${COLORS.border}`,
                                                cursor: "pointer",
                                                bgcolor: String(selectedTraceId) === tid ? "rgba(124,58,237,0.18)" : COLORS.subtleBg,
                                            }}
                                        >
                                            <ListItemText
                                                primary={<Typography sx={{ fontWeight: 900, color: COLORS.textPrimary }}>{label}</Typography>}
                                                secondary={<Typography variant="caption" sx={{ color: COLORS.textSecondary }}>{formatTimestamp(t.startedAt)}</Typography>}
                                            />
                                        </ListItem>
                                    );
                                })}

                                {traces.length === 0 && <Typography sx={{ color: COLORS.textSecondary, mt: 1 }}>No traces yet.</Typography>}
                            </List>
                        </CardContent>
                    </Card>

                    <GraphPanel
                        traceId={selectedTraceId}
                        setTraceId={(val) => {
                            setLiveNewestTrace(false);
                            setSelectedTraceId(val);
                        }}
                        traceLabel={selectedLabel}
                        graph={graph}
                        showMessageNodes={showMessageNodes}
                        updateXarrow={updateXarrow}
                        loading={loading}
                    />
                </Box>
            </Box>
        </Box>
    );
}