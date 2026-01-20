import React, { useEffect, useMemo, useState, useLayoutEffect } from "react";
import {
    Box,
    Card,
    CardContent,
    Typography,
    List,
    ListItem,
    ListItemText,
    ListSubheader,
    Chip,
    Alert,
    AppBar,
    Tabs,
    Toolbar,
    Tab,
    Button,
    Stack,
    Switch,
    FormControlLabel,
    Divider,
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
    const bg = COLORS.rel[relType] ?? "rgba(230,230,250,0.25)";
    return {
        bgcolor: bg,
        color: COLORS.chipText,
        borderRadius: 999,
        fontWeight: 700,
        border: `1px solid ${COLORS.border}`,
    };
}

function nodeAccent(type) {
    return COLORS.node[type] ?? COLORS.node.Default;
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

function stripeColor(eventType) {
    if (eventType === "PROVIDES") return COLORS.rel.PROVIDES;
    if (eventType === "REQUIRES") return COLORS.rel.REQUIRES;
    if (eventType === "COMMUNICATES_WITH") return COLORS.rel.COMMUNICATES_WITH;
    if (eventType === "PART_OF") return COLORS.rel.PART_OF;
    return "rgba(239,68,68,0.9)";
}

function pickTraceId(messages, wantedCapability) {
    const arr = Array.isArray(messages) ? messages : [];
    const withWanted = arr.filter((m) => (m?.capability || "").toLowerCase() === wantedCapability.toLowerCase());
    const pickFrom = withWanted.length ? withWanted : arr;
    const first = pickFrom[0];
    const tid = first?.traceId;
    return tid && String(tid).trim() !== "" ? String(tid) : "";
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
        traceNode?.id ||
        baseNodes.find((n) => (indeg.get(n.id) || 0) === 0)?.id ||
        baseNodes[0]?.id ||
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
                        title,
                        traceId,
                        setTraceId,
                        graph,
                        derived,
                        showMessageNodes,
                        updateXarrow,
                        loading,
                    }) {
    const layout = useMemo(
        () => buildTreeLayout(graph.nodes, graph.edges, showMessageNodes),
        [graph.nodes, graph.edges, showMessageNodes]
    );

    useLayoutEffect(() => {
        const t = setTimeout(updateXarrow, 150);
        return () => clearTimeout(t);
    }, [layout.nodes, graph.edges, updateXarrow]);

    return (
        <Card sx={{ bgcolor: "rgba(0,0,0,0.12)", border: `1px solid ${COLORS.border}`, borderRadius: 3, overflow: "hidden" }}>
            <CardContent sx={{ pb: 2 }}>
                <Stack direction="row" alignItems="center" justifyContent="space-between" gap={2}>
                    <Typography variant="h6" sx={{ fontWeight: 900, color: COLORS.textPrimary }}>
                        {title}
                    </Typography>

                    <Stack direction="row" alignItems="center" gap={1}>
                        <Typography variant="caption" sx={{ color: COLORS.textSecondary }}>
                            TraceId:
                        </Typography>
                        <input
                            type="text"
                            value={traceId}
                            onChange={(e) => setTraceId(e.target.value)}
                            placeholder="auto / optional"
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

                <Box sx={{ width: "100%", height: 540, overflow: "auto", borderRadius: 2 }}>
                    <Box sx={{ position: "relative", width: layout.width, height: layout.height, minHeight: 540 }}>
                        <Xwrapper>
                            {layout.nodes.map((node) => {
                                const provides = derived.provides[node.id] ?? [];
                                const requires = derived.requires[node.id] ?? [];
                                const comms = derived.comms[node.id] ?? [];
                                const partOf = derived.partOf[node.id] ?? [];

                                return (
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

                                            {partOf.length > 0 && (
                                                <Typography variant="body2" sx={{ mt: 0.75, color: COLORS.textSecondary }}>
                                                    Part of: <b>{partOf.join(", ")}</b>
                                                </Typography>
                                            )}

                                            <Divider sx={{ my: 1.25, borderColor: COLORS.border }} />

                                            {provides.length === 0 && requires.length === 0 && comms.length === 0 ? (
                                                <Typography variant="body2" sx={{ color: COLORS.textSecondary }}>
                                                    No relations yet.
                                                </Typography>
                                            ) : (
                                                <Stack spacing={1}>
                                                    {provides.length > 0 && (
                                                        <Box>
                                                            <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 0.5 }}>
                                                                <Chip label="PROVIDES" size="small" sx={relChip("PROVIDES")} />
                                                                <Typography variant="body2" sx={{ color: COLORS.textSecondary }}>
                                                                    {provides.length} capability(s)
                                                                </Typography>
                                                            </Stack>
                                                            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                                                                {provides.map((cap) => (
                                                                    <Chip
                                                                        key={cap}
                                                                        label={cap}
                                                                        size="small"
                                                                        sx={{ bgcolor: COLORS.subtleBg, color: COLORS.textPrimary, border: `1px solid ${COLORS.border}` }}
                                                                    />
                                                                ))}
                                                            </Stack>
                                                        </Box>
                                                    )}

                                                    {requires.length > 0 && (
                                                        <Box>
                                                            <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 0.5 }}>
                                                                <Chip label="REQUIRES" size="small" sx={relChip("REQUIRES")} />
                                                                <Typography variant="body2" sx={{ color: COLORS.textSecondary }}>
                                                                    {requires.length} capability(s)
                                                                </Typography>
                                                            </Stack>
                                                            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                                                                {requires.map((cap) => (
                                                                    <Chip
                                                                        key={cap}
                                                                        label={cap}
                                                                        size="small"
                                                                        sx={{ bgcolor: COLORS.subtleBg, color: COLORS.textPrimary, border: `1px solid ${COLORS.border}` }}
                                                                    />
                                                                ))}
                                                            </Stack>
                                                        </Box>
                                                    )}

                                                    {comms.length > 0 && (
                                                        <Box>
                                                            <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 0.5 }}>
                                                                <Chip label="COMMUNICATES_WITH" size="small" sx={relChip("COMMUNICATES_WITH")} />
                                                                <Typography variant="body2" sx={{ color: COLORS.textSecondary }}>
                                                                    {comms.length} target(s)
                                                                </Typography>
                                                            </Stack>
                                                            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                                                                {comms.map((t) => (
                                                                    <Chip
                                                                        key={t}
                                                                        label={t}
                                                                        size="small"
                                                                        sx={{ bgcolor: COLORS.subtleBg, color: COLORS.textPrimary, border: `1px solid ${COLORS.border}` }}
                                                                    />
                                                                ))}
                                                            </Stack>
                                                        </Box>
                                                    )}
                                                </Stack>
                                            )}
                                        </CardContent>
                                    </Card>
                                );
                            })}

                            {graph.edges
                                .filter(
                                    (e) => layout.nodes.some((n) => n.id === e.from) && layout.nodes.some((n) => n.id === e.to)
                                )
                                .map((edge, i) => {
                                    const fromNode = layout.nodes.find((n) => n.id === edge.from);
                                    const toNode = layout.nodes.find((n) => n.id === edge.to);
                                    if (!fromNode || !toNode) return null;

                                    const fromX = parseFloat(String(fromNode.style.left).replace("px", "")) || 0;
                                    const toX = parseFloat(String(toNode.style.left).replace("px", "")) || 0;

                                    const startAnchor = fromX <= toX ? "right" : "left";
                                    const endAnchor = fromX <= toX ? "left" : "right";

                                    return (
                                        <Xarrow
                                            key={`${edge.from}-${edge.to}-${edge.type}-${i}`}
                                            start={edge.from}
                                            end={edge.to}
                                            color={COLORS.rel[edge.type] ?? COLORS.textSecondary}
                                            strokeWidth={2}
                                            headSize={5}
                                            path="smooth"
                                            startAnchor={startAnchor}
                                            endAnchor={endAnchor}
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
                                    );
                                })}
                        </Xwrapper>
                    </Box>
                </Box>
            </CardContent>
        </Card>
    );
}

export default function App() {
    const [view, setView] = useState("graph");
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);
    const [msgError, setMsgError] = useState(null);
    const [messages, setMessages] = useState([]);
    const [autoRefresh, setAutoRefresh] = useState(true);
    const [loginTraceId, setLoginTraceId] = useState("");
    const [productTraceId, setProductTraceId] = useState("");
    const [loginGraph, setLoginGraph] = useState({ nodes: [], edges: [] });
    const [productGraph, setProductGraph] = useState({ nodes: [], edges: [] });
    const [showMessageNodes, setShowMessageNodes] = useState(false);

    const updateXarrow = useXarrow();

    const loadMessages = async () => {
        try {
            setMsgError(null);
            const res = await fetch("/api/blackboard/messages?limit=200"); // load more so we can auto-pick traceIds
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const data = await res.json();
            const arr = Array.isArray(data) ? data : [];
            setMessages(arr);
            setLoginTraceId((prev) => (prev && prev.trim() ? prev : pickTraceId(arr, "Authentication")));
            setProductTraceId((prev) => (prev && prev.trim() ? prev : pickTraceId(arr, "ProductList")));
        } catch (e) {
            setMsgError(String(e?.message ?? e));
        }
    };

    const loadGraphFor = async (traceId, setter) => {
        let url = "/api/blackboard/graph";
        if (traceId && traceId.trim() !== "") url += "?traceId=" + encodeURIComponent(traceId.trim());
        const res = await fetch(url);
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        const nodes = Array.isArray(data?.nodes) ? data.nodes : [];
        const edges = Array.isArray(data?.edges) ? data.edges : [];
        setter({ nodes, edges });
    };

    const loadBothGraphs = async () => {
        try {
            setLoading(true);
            setError(null);
            await Promise.all([
                loadGraphFor(loginTraceId, setLoginGraph),
                loadGraphFor(productTraceId, setProductGraph),
            ]);
        } catch (e) {
            setError(String(e?.message ?? e));
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadMessages();
    }, []);

    useEffect(() => {
        loadBothGraphs();
    }, [loginTraceId, productTraceId]);

    useEffect(() => {
        if (!autoRefresh) return;
        const interval = setInterval(() => {
            loadMessages();
            loadBothGraphs();
        }, 2000);
        return () => clearInterval(interval);
    }, [autoRefresh, loginTraceId, productTraceId]);

    const makeDerived = (graph) =>
        useMemo(() => {
            const byId = new Map();
            graph.nodes.forEach((n) => byId.set(n.id, n));

            const provides = new Map();
            const requires = new Map();
            const comms = new Map();
            const partOf = new Map();

            const add = (map, key, value) => {
                if (!map.has(key)) map.set(key, new Set());
                map.get(key).add(value);
            };

            for (const e of graph.edges) {
                const from = byId.get(e.from);
                const to = byId.get(e.to);
                if (!from || !to) continue;

                if (e.type === "PROVIDES") add(provides, e.from, to.label);
                if (e.type === "REQUIRES") add(requires, e.from, to.label);
                if (e.type === "COMMUNICATES_WITH") add(comms, e.from, to.label);
                if (e.type === "PART_OF") add(partOf, e.from, to.label);
            }

            const toArrMap = (m) =>
                Object.fromEntries(Array.from(m.entries()).map(([k, v]) => [k, Array.from(v)]));

            return {
                byId,
                provides: toArrMap(provides),
                requires: toArrMap(requires),
                comms: toArrMap(comms),
                partOf: toArrMap(partOf),
            };
        }, [graph]);

    const loginDerived = makeDerived(loginGraph);
    const productDerived = makeDerived(productGraph);

    const legend = (
        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <Chip label="REQUIRES" size="small" sx={relChip("REQUIRES")} />
            <Chip label="PROVIDES" size="small" sx={relChip("PROVIDES")} />
            <Chip label="COMMUNICATES_WITH" size="small" sx={relChip("COMMUNICATES_WITH")} />
            <Chip label="PART_OF" size="small" sx={relChip("PART_OF")} />
        </Stack>
    );

    const GraphsView = () => (
        <Stack spacing={2} sx={{ height: "100%", overflow: "auto" }}>
            <GraphPanel
                title="Login Trace (Authentication)"
                traceId={loginTraceId}
                setTraceId={setLoginTraceId}
                graph={loginGraph}
                derived={loginDerived}
                showMessageNodes={showMessageNodes}
                updateXarrow={updateXarrow}
                loading={loading}
            />
            <GraphPanel
                title="ProductList Trace (ProductList + Auth check)"
                traceId={productTraceId}
                setTraceId={setProductTraceId}
                graph={productGraph}
                derived={productDerived}
                showMessageNodes={showMessageNodes}
                updateXarrow={updateXarrow}
                loading={loading}
            />
        </Stack>
    );

    const MessagesView = () => (
        <Box sx={{ display: "flex", flexWrap: "wrap", gap: 2, overflowY: "auto", height: "100%" }}>
            {msgError && (
                <Alert severity="error" sx={{ width: "100%", borderRadius: 2 }}>
                    {msgError}
                </Alert>
            )}

            {messages.map((m, idx) => {
                const key = m.id ?? `${m.sender}-${m.timestamp}-${idx}`;
                return (
                    <Card
                        key={key}
                        sx={{
                            width: 320,
                            bgcolor: COLORS.cardPaper,
                            borderRadius: 3,
                            border: `1px solid ${COLORS.border}`,
                            boxShadow: "0 10px 30px rgba(0,0,0,0.55)",
                            display: "flex",
                            color: COLORS.textPrimary,
                            flexShrink: 0,
                            overflow: "hidden",
                        }}
                    >
                        <Box sx={{ width: 6, bgcolor: stripeColor(m.eventType) }} />
                        <CardContent sx={{ flex: 1 }}>
                            <Stack direction="row" justifyContent="space-between" alignItems="center">
                                <Typography variant="caption" sx={{ color: COLORS.textSecondary }}>
                                    {m.id ?? "—"}
                                </Typography>
                                <Chip label={m.eventType ?? "—"} size="small" sx={relChip(m.eventType)} />
                            </Stack>

                            <Typography variant="h6" sx={{ mt: 1, lineHeight: 1.2 }}>
                                {m.capability ?? "—"}
                            </Typography>

                            <Typography variant="body2" sx={{ mt: 1, color: COLORS.textSecondary }}>
                                👤 <b>{m.sender ?? "—"}</b>
                                {m.receiver ? (
                                    <>
                                        {" "}
                                        → <b>{m.receiver}</b>
                                    </>
                                ) : null}
                            </Typography>

                            <Typography variant="body2" sx={{ mt: 0.75, color: COLORS.textSecondary }}>
                                Trace: <b>{m.traceId ?? "—"}</b>
                            </Typography>

                            {m.payload && String(m.payload).trim() !== "" && (
                                <pre
                                    style={{
                                        fontSize: "0.78rem",
                                        background: COLORS.subtleBg,
                                        padding: 10,
                                        borderRadius: 10,
                                        marginTop: 10,
                                        whiteSpace: "pre-wrap",
                                        color: COLORS.textPrimary,
                                        wordBreak: "break-word",
                                        border: `1px solid ${COLORS.border}`,
                                    }}
                                >
                  {String(m.payload)}
                </pre>
                            )}

                            <Typography variant="caption" sx={{ color: COLORS.textSecondary, mt: 1, display: "block" }}>
                                Timestamp: {formatTimestamp(m.timestamp)}
                            </Typography>
                        </CardContent>
                    </Card>
                );
            })}

            {messages.length === 0 && !msgError && (
                <Typography sx={{ opacity: 0.7, color: COLORS.textSecondary }}>No messages available yet.</Typography>
            )}
        </Box>
    );

    const RelationsView = () => (
        <Box sx={{ height: "100%", overflow: "auto" }}>
            <Stack spacing={2}>
                <Card sx={{ bgcolor: COLORS.cardPaper, border: `1px solid ${COLORS.border}`, borderRadius: 3 }}>
                    <CardContent>
                        <Typography variant="h6">Info</Typography>
                        <Typography variant="body2" sx={{ color: COLORS.textSecondary, mt: 0.5 }}>
                            Zwei getrennte Graphen anhand von TraceIds. Du kannst die TraceIds oben pro Panel überschreiben.
                        </Typography>
                    </CardContent>
                </Card>
            </Stack>
        </Box>
    );

    return (
        <Box
            sx={{
                fontFamily: "system-ui",
                bgcolor: COLORS.backgroundDefault,
                color: COLORS.textPrimary,
                minHeight: "100vh",
                display: "flex",
                flexDirection: "column",
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
                            onClick={() => {
                                loadMessages();
                                loadBothGraphs();
                            }}
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

            <Box sx={{ px: 2, pt: 2, display: "flex", alignItems: "center", justifyContent: "space-between", gap: 2 }}>
                {legend}
                <Typography variant="body2" sx={{ color: COLORS.textSecondary }}>
                    Messages: <b>{messages.length}</b>
                </Typography>
            </Box>

            <Tabs
                value={view}
                onChange={(e, v) => setView(v)}
                sx={{
                    borderBottom: `1px solid ${COLORS.border}`,
                    px: 2,
                    mt: 1,
                    "& .MuiTab-root": { color: COLORS.textSecondary, fontWeight: 800 },
                    "& .Mui-selected": { color: COLORS.textPrimary },
                    "& .MuiTabs-indicator": { backgroundColor: COLORS.primary },
                }}
            >
                <Tab label="Graphs" value="graph" />
                <Tab label="Messages" value="messages" />
                <Tab label="Info" value="relations" />
            </Tabs>

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

                {view === "graph" && <GraphsView />}
                {view === "messages" && <MessagesView />}
                {view === "relations" && <RelationsView />}
            </Box>
        </Box>
    );
}