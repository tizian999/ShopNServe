// javascript
import React, { useEffect, useState } from "react";
import {
    AppBar,
    Toolbar,
    Typography,
    Tabs,
    Tab,
    Box,
    Card,
    CardContent,
    Chip,
    Alert,
} from "@mui/material";

export default function App() {
    const COLORS = {
        primary: "#7c3aed", // purple
        backgroundDefault: "#07070a", // near-black page bg
        cardPaper: "#0f1724", // dark card surface
        textPrimary: "#e6e6fa", // very light text
        textSecondary: "rgba(230,230,250,0.7)",
        stripe: {
            PROVIDES: "#9f7aea", // lighter purple
            REQUIRES: "#7c3aed", // purple
            ERROR: "#ef4444", // keep errors visible
        },
        chipText: "#ffffff",
        payloadBg: "rgba(255,255,255,0.03)", // subtle highlight on dark bg
    };

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
            // eslint-disable-next-line no-console
            console.error("Fehler beim Laden der Messages:", e);
        }
    }

    const chipStyleFor = (eventType) => {
        const bg = COLORS.stripe[eventType] || "transparent";
        return {
            backgroundColor: bg,
            color: COLORS.chipText,
            fontWeight: 700,
            height: 28,
        };
    };

    const stripeColor = (eventType) => COLORS.stripe[eventType] || "transparent";

    return (
        <Box
            sx={{
                fontFamily: "system-ui",
                bgcolor: COLORS.backgroundDefault,
                color: COLORS.textPrimary,
                minHeight: "100vh",
            }}
        >
            <AppBar
                position="static"
                sx={{
                    backgroundColor: COLORS.primary,
                    boxShadow: "0 1px 6px rgba(0,0,0,0.6)",
                }}
            >
                <Toolbar>
                    <Typography variant="h6" sx={{ color: COLORS.chipText }}>
                        Live Blackboard
                    </Typography>
                </Toolbar>
            </AppBar>

            <Tabs
                value={view}
                onChange={(e, v) => setView(v)}
                sx={{
                    borderBottom: 1,
                    borderColor: "rgba(255,255,255,0.06)",
                    mb: 2,
                    px: 2,
                    color: COLORS.textPrimary,
                }}
            >
                <Tab label="Messages" value="messages" />
            </Tabs>

            <Box sx={{ p: 2 }}>
                {view === "messages" && (
                    <>
                        {msgError && (
                            <Alert
                                severity="error"
                                sx={{
                                    mb: 2,
                                    borderRadius: 1,
                                    backgroundColor: "rgba(239,68,68,0.12)",
                                    color: COLORS.textPrimary,
                                }}
                            >
                                {msgError}
                            </Alert>
                        )}

                        <Box sx={{ display: "flex", flexWrap: "wrap", gap: 2 }}>
                            {messages.map((m, idx) => {
                                const key = m.elementId || m.id || `msg-${idx}`;
                                return (
                                    <Card
                                        key={key}
                                        sx={{
                                            width: 280,
                                            bgcolor: COLORS.cardPaper,
                                            borderRadius: 2,
                                            boxShadow: "0 6px 18px rgba(0,0,0,0.6)",
                                            display: "flex",
                                            alignItems: "stretch",
                                            overflow: "visible",
                                            color: COLORS.textPrimary,
                                        }}
                                    >
                                        <Box
                                            sx={{
                                                width: 6,
                                                bgcolor: stripeColor(m.eventType),
                                                borderTopLeftRadius: 8,
                                                borderBottomLeftRadius: 8,
                                            }}
                                        />
                                        <CardContent sx={{ flex: 1 }}>
                                            <Typography variant="caption" sx={{ color: COLORS.textSecondary }}>
                                                {key}
                                            </Typography>

                                            <Chip
                                                label={m.eventType}
                                                size="small"
                                                sx={{ mt: 1, mb: 1, ...chipStyleFor(m.eventType) }}
                                            />

                                            <Typography variant="subtitle1" sx={{ color: COLORS.textPrimary }}>
                                                {m.capability}
                                            </Typography>

                                            <Typography variant="body2" sx={{ mt: 1, color: COLORS.textSecondary }}>
                                                👤 {m.sender || "—"}
                                            </Typography>

                                            {m.payload && (
                                                <pre
                                                    style={{
                                                        fontSize: "0.78rem",
                                                        background: COLORS.payloadBg,
                                                        padding: 8,
                                                        borderRadius: 6,
                                                        marginTop: 8,
                                                        whiteSpace: "pre-wrap",
                                                        color: COLORS.textPrimary,
                                                    }}
                                                >
                          {m.payload}
                        </pre>
                                            )}

                                            <Typography
                                                variant="caption"
                                                sx={{ color: COLORS.textSecondary, mt: 1, display: "block" }}
                                            >
                                                {m.timestamp}
                                            </Typography>
                                        </CardContent>
                                    </Card>
                                );
                            })}

                            {messages.length === 0 && !msgError && (
                                <Typography sx={{ opacity: 0.7, color: COLORS.textSecondary }}>
                                    Keine Messages vorhanden.
                                </Typography>
                            )}
                        </Box>
                    </>
                )}
            </Box>
        </Box>
    );
}
