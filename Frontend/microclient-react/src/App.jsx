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
    },
    node: {
        UIComponent: "rgba(124,58,237,0.15)",
        BackendComponent: "rgba(159,122,234,0.14)",
        Capability: "rgba(230,230,250,0.07)",
        MicroClient: "rgba(230,230,250,0.06)",
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

export default function App() {
    const [view, setView] = useState("graph");

    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(false);

    const [msgError, setMsgError] = useState(null);
    const [messages, setMessages] = useState([]);

    const [autoRefresh, setAutoRefresh] = useState(true);
    const [graph, setGraph] = useState({ nodes: [], edges: [] });

    const updateXarrow = useXarrow();
    const loadGraph = async () => {
        try {
            setLoading(true);
            setError(null);

            const res = await fetch("/api/blackboard/graph");
            if (!res.ok) throw new Error(`HTTP ${res.status}`);

            const data = await res.json();
            const nodes = Array.isArray(data?.nodes) ? data.nodes : [];
            const edges = Array.isArray(data?.edges) ? data.edges : [];

            setGraph({ nodes, edges });
        } catch (e) {
            setError(String(e?.message ?? e));
        } finally {
            setLoading(false);
        }
    };

    const loadMessages = async () => {
        try {
            setMsgError(null);
            const res = await fetch("/api/blackboard/messages?limit=50");
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const data = await res.json();
            setMessages(Array.isArray(data) ? data : []);
        } catch (e) {
            setMsgError(String(e?.message ?? e));
        }
    };

    useEffect(() => {
        loadGraph();
        loadMessages();

        if (!autoRefresh) return;

        const interval = setInterval(() => {
            loadGraph();
            loadMessages();
        }, 2000);

        return () => clearInterval(interval);
    }, [autoRefresh]);
    const derived = useMemo(() => {
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
    const layout = useMemo(() => {
        const groups = {
            UIComponent: [],
            MicroClient: [],
            BackendComponent: [],
            Capability: [],
            Other: [],
        };

        for (const n of graph.nodes) {
            if (groups[n.type]) groups[n.type].push(n);
            else groups.Other.push(n);
        }

        Object.values(groups).forEach((arr) => arr.sort((a, b) => a.label.localeCompare(b.label)));

        const columns = ["UIComponent", "MicroClient", "BackendComponent", "Capability", "Other"]
            .filter((k) => groups[k].length > 0)
            .map((k) => ({ key: k, nodes: groups[k] }));

        const COLUMN_WIDTH = 360;
        const ROW_HEIGHT = 220;
        const PADDING_X = 24;
        const PADDING_Y = 24;

        const positioned = [];
        columns.forEach((col, colIdx) => {
            col.nodes.forEach((n, rowIdx) => {
                positioned.push({
                    ...n,
                    style: {
                        position: "absolute",
                        left: `${PADDING_X + colIdx * COLUMN_WIDTH}px`,
                        top: `${PADDING_Y + rowIdx * ROW_HEIGHT}px`,
                        width: 320,
                    },
                });
            });
        });

        const width = `${PADDING_X * 2 + columns.length * COLUMN_WIDTH}px`;
        const height = `${PADDING_Y * 2 + Math.max(1, ...columns.map((c) => c.nodes.length)) * ROW_HEIGHT}px`;

        return { nodes: positioned, width, height };
    }, [graph.nodes]);

    useLayoutEffect(() => {
        const t = setTimeout(updateXarrow, 150);
        return () => clearTimeout(t);
    }, [layout.nodes, graph.edges, updateXarrow]);

    const legend = (
        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
            <Chip label="REQUIRES" size="small" sx={relChip("REQUIRES")} />
            <Chip label="PROVIDES" size="small" sx={relChip("PROVIDES")} />
            <Chip label="COMMUNICATES_WITH" size="small" sx={relChip("COMMUNICATES_WITH")} />
            <Chip label="PART_OF" size="small" sx={relChip("PART_OF")} />
        </Stack>
    );

    const GraphView = () => (
        <Box sx={{ width: "100%", height: "100%", overflow: "auto", borderRadius: 2 }}>
            <Box sx={{ position: "relative", width: layout.width, height: layout.height, minHeight: 420 }}>
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

                    {graph.edges.map((edge, i) => (
                        <Xarrow
                            key={`${edge.from}-${edge.to}-${edge.type}-${i}`}
                            start={edge.from}
                            end={edge.to}
                            color={COLORS.rel[edge.type] ?? COLORS.textSecondary}
                            strokeWidth={2}
                            headSize={5}
                            path="grid"
                            startAnchor="right"
                            endAnchor="left"
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
                <Typography sx={{ opacity: 0.7, color: COLORS.textSecondary }}>
                    No messages available yet. Send events (Login / ProductList) and reload.
                </Typography>
            )}
        </Box>
    );

    const RelationsView = () => (
        <Box sx={{ height: "100%", overflow: "auto" }}>
            <Stack spacing={2}>
                <Card sx={{ bgcolor: COLORS.cardPaper, border: `1px solid ${COLORS.border}`, borderRadius: 3 }}>
                    <CardContent>
                        <Typography variant="h6">Nodes ({graph.nodes.length})</Typography>
                        <Typography variant="body2" sx={{ color: COLORS.textSecondary, mt: 0.5 }}>
                            Überblick über alle Knoten, gruppiert nach Typ.
                        </Typography>

                        <Divider sx={{ my: 1.5, borderColor: COLORS.border }} />

                        {["UIComponent", "MicroClient", "BackendComponent", "Capability", "Other"].map((t) => {
                            const list = graph.nodes.filter((n) => n.type === t);
                            if (list.length === 0) return null;
                            return (
                                <List
                                    key={t}
                                    dense
                                    subheader={
                                        <ListSubheader sx={{ bgcolor: "transparent", color: COLORS.textSecondary }}>
                                            {t} ({list.length})
                                        </ListSubheader>
                                    }
                                >
                                    {list.map((n) => (
                                        <ListItem key={n.id} disableGutters>
                                            <ListItemText
                                                primary={<span style={{ color: COLORS.textPrimary, fontWeight: 800 }}>{n.label}</span>}
                                                secondary={<span style={{ color: COLORS.textSecondary }}>{n.id}</span>}
                                            />
                                        </ListItem>
                                    ))}
                                </List>
                            );
                        })}
                    </CardContent>
                </Card>

                <Card sx={{ bgcolor: COLORS.cardPaper, border: `1px solid ${COLORS.border}`, borderRadius: 3 }}>
                    <CardContent>
                        <Typography variant="h6">Edges ({graph.edges.length})</Typography>
                        <Typography variant="body2" sx={{ color: COLORS.textSecondary, mt: 0.5 }}>
                            Alle Beziehungen aus Neo4j.
                        </Typography>

                        <Divider sx={{ my: 1.5, borderColor: COLORS.border }} />

                        <List dense>
                            {graph.edges.map((e, idx) => (
                                <ListItem key={idx} disableGutters>
                                    <ListItemText
                                        primary={
                                            <span style={{ color: COLORS.textPrimary }}>
                        <b>{e.type}</b>: {e.from} → {e.to}
                      </span>
                                        }
                                        secondary={<span style={{ color: COLORS.textSecondary }}>({e.from.split(":")[0]} → {e.to.split(":")[0]})</span>}
                                    />
                                </ListItem>
                            ))}
                            {graph.edges.length === 0 && (
                                <Typography sx={{ opacity: 0.7, color: COLORS.textSecondary }}>No edges available.</Typography>
                            )}
                        </List>
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

                        <Button
                            onClick={() => {
                                loadGraph();
                                loadMessages();
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
                    Nodes: <b>{graph.nodes.length}</b> · Edges: <b>{graph.edges.length}</b> · Messages:{" "}
                    <b>{messages.length}</b>
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
                <Tab label="Graph" value="graph" />
                <Tab label="Messages" value="messages" />
                <Tab label="Relations" value="relations" />
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

                {view === "graph" && <GraphView />}
                {view === "messages" && <MessagesView />}
                {view === "relations" && <RelationsView />}
            </Box>
        </Box>
    );
}
