import { useState } from 'react'
import './App.css'
import { AuthProvider, useAuth } from './auth/AuthContext'
import { AccountPanel } from './auth/AccountPanel'

function HomePage() {
    const { user } = useAuth()
    const [showAccount, setShowAccount] = useState(false)

    return (
        <div style={{ maxWidth: 980, margin: '0 auto', padding: 16 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap', alignItems: 'center' }}>
                <div style={{ fontWeight: 800, fontSize: 18 }}>BidMart</div>
                <div style={{ display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap' }}>
                    {user ? <span>Hi, <b>{user.username}</b> ({user.role})</span> : <span style={{ opacity: 0.8 }}>Guest</span>}
                    <button onClick={() => setShowAccount(v => !v)}>{showAccount ? 'Close' : 'Account'}</button>
                </div>
            </div>

            <div style={{ marginTop: 24 }}>
                <h1 style={{ margin: 0 }}>Browse auctions</h1>
                <p style={{ opacity: 0.8, marginTop: 8 }}>
                    Browse listings without signing in. Sign in is required to bid, sell, and manage wallet.
                </p>

                <div style={{ display: 'grid', gap: 12, gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', marginTop: 16 }}>
                    <div style={card}>Popular item #1</div>
                    <div style={card}>Popular item #2</div>
                    <div style={card}>Popular item #3</div>
                </div>
            </div>

            {showAccount && (
                <div style={{ marginTop: 24 }}>
                    <AccountPanel />
                </div>
            )}
        </div>
    )
}

const card: React.CSSProperties = { border: '1px solid #333', borderRadius: 12, padding: 14, minHeight: 90 }

export default function App() {
    return (
        <AuthProvider>
            <HomePage />
        </AuthProvider>
    )
}