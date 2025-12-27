import React from "react";
import "./App.css";
import VueShopWrapper from './VueShopWrapper.jsx';
import ReactMicroclientWrapper from './ReactMicroclientWrapper.jsx';

function App() {
    return (
        <div>
            <header className="app-header">
                <h1>ShopNServe</h1>
                <p>Blackboard-basierte Microfrontend-Plattform</p>
            </header>

            <main style={{ display: 'flex', gap: '1rem', padding: '1rem' }}>
                <div style={{ flex: 1, border: '1px solid #ddd', borderRadius: '8px', padding: '1rem' }}>
                    <h2>🛒 Shop </h2>
                    <VueShopWrapper />
                </div>
                <div style={{ flex: 1, border: '1px solid #ddd', borderRadius: '8px', padding: '1rem', display: 'flex', flexDirection: 'column' }}>
                    <h2>📡 Communication</h2>
                    <div style={{
                        flex: 1,
                        border: '1px solid #ccc',
                        borderRadius: '8px',
                        padding: '1rem',
                        overflow: 'auto'
                    }}>
                        <ReactMicroclientWrapper />
                    </div>
                </div>
            </main>
        </div>
    );
}

export default App;
