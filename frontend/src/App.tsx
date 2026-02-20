import { useMemo, useState } from 'react'
import './App.css'

type MeResponse = { username: string }
type PingResponse = { db: number }
type LoginResponse = { token: string }

export default function App() {
  const [username, setUsername] = useState('demo')
  const [password, setPassword] = useState('demo')
  const [log, setLog] = useState<string>('')

  const token = useMemo(() => localStorage.getItem('authToken') ?? '', [])

  async function register() {
    setLog('registering...')
    const res = await fetch('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    })
    setLog(res.ok ? 'register ok' : `register failed: ${res.status}`)
  }

  async function login() {
    setLog('logging in...')
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    })
    if (!res.ok) {
      setLog(`login failed: ${res.status}`)
      return
    }
    const data = (await res.json()) as LoginResponse
    localStorage.setItem('authToken', data.token)
    setLog(`login ok, token saved`)
    window.location.reload()
  }

  async function me() {
    setLog('calling /me...')
    const res = await fetch('/api/auth/me', {
      headers: { 'X-Auth-Token': localStorage.getItem('authToken') ?? '' },
    })
    if (!res.ok) {
      setLog(`me failed: ${res.status}`)
      return
    }
    const data = (await res.json()) as MeResponse
    setLog(`me: ${data.username}`)
  }

  async function dbPing() {
    setLog('calling /db/ping...')
    const res = await fetch('/api/db/ping')
    if (!res.ok) {
      setLog(`db ping failed: ${res.status}`)
      return
    }
    const data = (await res.json()) as PingResponse
    setLog(`db ping ok: ${data.db}`)
  }

  return (
    <div>
      <h1>BidMart Auth (dummy)</h1>

      <div style={{ display: 'grid', gap: 8, maxWidth: 360, margin: '0 auto' }}>
        <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="username" />
        <input value={password} onChange={(e) => setPassword(e.target.value)} placeholder="password" type="password" />

        <button onClick={register}>Register</button>
        <button onClick={login}>Login</button>

        <hr />

        <button onClick={me}>Who am I (/api/auth/me)</button>
        <button onClick={dbPing}>DB Ping (/api/db/ping)</button>

        <small>Current token: {token ? token.slice(0, 8) + '...' : '(none)'}</small>
        <pre style={{ textAlign: 'left', whiteSpace: 'pre-wrap' }}>{log}</pre>
      </div>
    </div>
  )
}
