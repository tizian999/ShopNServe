import React from "react";
import "./App.css";
import VueShopWrapper from './VueShopWrapper.jsx';
import ReactMicroclientWrapper from './ReactMicroclientWrapper.jsx';

function App() {
    return (
        <div className="app-root">
            <header className="app-header">
                <h1>ShopNServe</h1>
                <p>Blackboard-basierte Microfrontend-Plattform</p>
            </header>

            <main className="app-main-grid">
                <div className="panel">
                    <h2>🛒 Shop </h2>
                    <VueShopWrapper />
                </div>

                <div className="panel">
                    <h2>📡 Communication</h2>
                    <div style={{
                        width: '100%',
                        height: '600px',
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
