import { useEffect, useState } from 'react'
import axios from 'axios'

function App() {
    const [products, setProducts] = useState([])

    useEffect(() => {
        axios.get('/api/products')
            .then(res => setProducts(res.data))
            .catch(console.error)
    }, [])

    return (
        <div style={{ padding: 20 }}>
            <h2>React MicroClient – Produkte</h2>
            {products.length === 0 ? <p>Keine Produkte</p> :
                <ul>{products.map(p => <li key={p.id}>{p.name}</li>)}</ul>}
        </div>
    )
}

export default App
