import { useEffect, useState } from 'react'

interface Item { id: number; name: string; startingPrice: number; }

function App() {
    const [items, setItems] = useState<Item[]>([]);

    useEffect(() => {
        fetch('http://localhost:8080/api/items')
            .then(res => res.json())
            .then(data => setItems(data))
            .catch(err => console.error("Gagal fetch data:", err));
    }, []);

    return (
        <div style={{ padding: '20px' }}>
            <h1>BidMart - Daftar Barang Lelang</h1>
            <ul>
                {items.map(item => (
                    <li key={item.id}>
                        <strong>{item.name}</strong> - Harga Awal: Rp {item.startingPrice.toLocaleString()}
                    </li>
                ))}
            </ul>
        </div>
    )
}
export default App