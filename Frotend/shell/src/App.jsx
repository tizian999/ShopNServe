function App() {
    return (
        <div style={{ padding: 20 }}>
            <h1>ShopNServe MicroClients Shell</h1>
            <div style={{ display: 'flex', gap: '2rem' }}>
                <iframe
                    src="http://localhost:5174"
                    title="React Client"
                    width="45%"
                    height="400"
                    style={{ border: '1px solid #ccc', borderRadius: '10px' }}
                />
                <iframe
                    src="http://localhost:5175"
                    title="Vue Client"
                    width="45%"
                    height="400"
                    style={{ border: '1px solid #ccc', borderRadius: '10px' }}
                />
            </div>
        </div>
    )
}
export default App
