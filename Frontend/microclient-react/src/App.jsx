import { useEffect, useState } from "react";
import {
    AppBar,
    Toolbar,
    Typography,
    Button,
    Tabs,
    Tab,
    Box,
    Card,
    CardContent,
    Chip,
    Alert,
} from "@mui/material";

export default function App() {
    const [messages, setMessages] = useState([]);
    const [view, setView] = useState("messages");
    const [msgError, setMsgError] = useState(null);

    useEffect(() => {
        loadMessages();
        const interval = setInterval(loadMessages, 2000);
        return () => clearInterval(interval);
    }, []);

    async function loadMessages() {
        try {
            const res = await fetch("http://localhost:8080/api/messages");
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            const data = await res.json();
            if (!Array.isArray(data)) {
                setMsgError("Backend liefert kein Array!");
                return;
            }
            setMessages(data);
            setMsgError(null);
        } catch (e) {
            setMsgError(e.message);
            console.error("Fehler beim Laden der Messages:", e);
        }
    }

    const colorForEvent = (eventType) => {
        switch (eventType) {
            case "PROVIDES":
                return "success";
            case "REQUIRES":
                return "info";
            case "ERROR":
                return "error";
            default:
                return "default";
        }
    };
    return (
        <Box sx={{ fontFamily: "system-ui" }}>
            <AppBar position="static">
                <Toolbar>
                    <Typography variant="h6">Live Blackboard (React + MUI)</Typography>
                </Toolbar>
            </AppBar>
            <Tabs
                value={view}
                onChange={(e, v) => setView(v)}
                sx={{ borderBottom: 1, borderColor: "divider", mb: 2 }}
            >
                <Tab label="Messages" value="messages" />
            </Tabs>
            <Box sx={{ p: 2 }}>
                {view === "messages" && (
                    <>
                        {msgError && (
                            <Alert severity="error" sx={{ mb: 2 }}>
                                {msgError}
                            </Alert>
                        )}

                        <Box
                            sx={{
                                display: "flex",
                                flexWrap: "wrap",
                                gap: 2,
                            }}
                        >
                            {messages.map((m, idx) => {
                                const key = m.elementId || m.id || `msg-${idx}`;

                                return (
                                    <Card
                                        key={key}
                                        sx={{
                                            width: 260,
                                            background:
                                                m.eventType === "PROVIDES"
                                                    ? "#d0f2d0"
                                                    : m.eventType === "REQUIRES"
                                                        ? "#d0e4ff"
                                                        : "#ececec",
                                        }}
                                    >
                                        <CardContent>
                                            <Typography
                                                variant="caption"
                                                sx={{ opacity: 0.7 }}
                                            >
                                                {key}
                                            </Typography>

                                            <Chip
                                                label={m.eventType}
                                                color={colorForEvent(m.eventType)}
                                                size="small"
                                                sx={{ mt: 1, mb: 1 }}
                                            />

                                            <Typography variant="subtitle1">
                                                {m.capability}
                                            </Typography>

                                            <Typography variant="body2" sx={{ mt: 1 }}>
                                                👤 {m.sender || "—"}
                                            </Typography>

                                            {m.payload && (
                                                <pre
                                                    style={{
                                                        fontSize: "0.75rem",
                                                        background: "rgba(0,0,0,0.05)",
                                                        padding: 6,
                                                        borderRadius: 4,
                                                        marginTop: 8,
                                                        whiteSpace: "pre-wrap",
                                                    }}
                                                >
                          {m.payload}
                        </pre>
                                            )}

                                            <Typography
                                                variant="caption"
                                                sx={{ opacity: 0.7, mt: 1 }}
                                            >
                                                {m.timestamp}
                                            </Typography>
                                        </CardContent>
                                    </Card>
                                );
                            })}

                            {messages.length === 0 && !msgError && (
                                <Typography sx={{ opacity: 0.7 }}>
                                    Keine Messages vorhanden.
                                </Typography>
                            )}
                        </Box>
                    </>
                )}
                {view === "graph" && <GraphPanel />}
            </Box>
        </Box>
    );
}
