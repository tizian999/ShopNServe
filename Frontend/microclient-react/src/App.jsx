import React, { useEffect, useState } from "react";
import ForceGraph2D from "react-force-graph-2d";
import axios from "axios";

function App() {
    const [graphData, setGraphData] = useState({ nodes: [], links: [] });

    useEffect(() => {
        axios.get("/api/graph")
            .then(res => setGraphData(res.data))
            .catch(err => console.error("Error loading graph:", err));
    }, []);

    return (
        <div style={{ height: "100vh", background: "#111" }}>
            <h2 style={{ color: "white", textAlign: "center" }}>Neo4j Product Graph</h2>
            <ForceGraph2D
                graphData={graphData}
                nodeLabel="name"
                linkDirectionalArrowLength={4}
                linkDirectionalArrowRelPos={1}
                nodeAutoColorBy="label"
                onNodeClick={(node) => alert(`Clicked ${node.label}: ${node.name}`)}
            />
        </div>
    );
}

export default App;
