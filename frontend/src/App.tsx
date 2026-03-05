import { useState } from 'react'
import './App.css'
import { AuthProvider, useAuth } from './auth/AuthContext'
import { AccountPanel } from './auth/AccountPanel'

function HomePage() {
    const { user } = useAuth()
    const [showAccount, setShowAccount] = useState(false)
    const [q, setQ] = useState('')

    return (
        <>
            <header className="bm-header">
                <div className="container bm-header__inner">
                    <a className="bm-logo" href="#" aria-label="BidMart">
                        <span className="c1">B</span><span className="c2">i</span><span className="c3">d</span><span className="c4">Mart</span>
                    </a>

                    <div className="bm-search" role="search">
                        <input
                            value={q}
                            onChange={(e) => setQ(e.target.value)}
                            placeholder="Search for anything"
                            aria-label="Search"
                        />
                        <button className="bm-btnPrimary" onClick={() => void 0} aria-label="Search">
                            Search
                        </button>
                    </div>

                    <div className="bm-user">
                        <span className="bm-pill">
                            {user ? (
                                <>Hi, <b>{user.username}</b> <span className="bm-muted">({user.role})</span></>
                            ) : (
                                <span className="bm-muted">Guest</span>
                            )}
                        </span>
                        <button onClick={() => setShowAccount(true)} aria-haspopup="dialog">
                            Account
                        </button>
                    </div>
                </div>

                <nav className="bm-nav" aria-label="Primary">
                    <div className="container bm-nav__inner">
                        <a href="#">Motors</a>
                        <a href="#">Electronics</a>
                        <a href="#">Collectibles</a>
                        <a href="#">Fashion</a>
                        <a href="#">Home & Garden</a>
                        <a href="#">Sports</a>
                        <a href="#">Toys</a>
                        <a href="#">Daily deals</a>
                    </div>
                </nav>
            </header>

            <main className="container bm-main">
                <section className="bm-hero">
                    <h1>Browse auctions</h1>
                    <p className="bm-muted" style={{ margin: '8px 0 0' }}>
                        Browse listings without signing in. Sign in is required to bid, sell, and manage wallet.
                    </p>
                </section>

                <section style={{ marginTop: 16 }}>
                    <div className="bm-grid">
                        <div className="bm-card">
                            <p className="bm-cardTitle">Popular item #1</p>
                            <p className="bm-cardMeta">Starting at $10 · Ends in 2h</p>
                        </div>
                        <div className="bm-card">
                            <p className="bm-cardTitle">Popular item #2</p>
                            <p className="bm-cardMeta">Starting at $35 · Ends tomorrow</p>
                        </div>
                        <div className="bm-card">
                            <p className="bm-cardTitle">Popular item #3</p>
                            <p className="bm-cardMeta">Starting at $7 · Ends in 5h</p>
                        </div>
                    </div>
                </section>
            </main>

            {showAccount && (
                <div className="bm-overlay" role="dialog" aria-modal="true" aria-label="Account">
                    <div className="bm-modal">
                        <div className="bm-modalHeader">
                            <h2>Account</h2>
                            <button onClick={() => setShowAccount(false)} aria-label="Close account dialog">
                                Close
                            </button>
                        </div>
                        <div className="bm-modalBody">
                            <AccountPanel />
                        </div>
                    </div>
                </div>
            )}
        </>
    )
}

export default function App() {
    return (
        <AuthProvider>
            <HomePage />
        </AuthProvider>
    )
}