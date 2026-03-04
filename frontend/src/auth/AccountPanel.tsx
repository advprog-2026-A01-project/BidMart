import { useEffect, useMemo, useState } from 'react'
import type { CSSProperties } from 'react'
import * as Api from '../api/auth'
import { useAuth } from './AuthContext'

type Tab = 'auth' | 'profile' | 'sessions' | 'admin'

export function AccountPanel() {
    const {
        user, tokens, loading, error,
        lastVerificationToken, pendingMfa,
        register, verifyEmail, login, submitMfa, cancelMfa,
        enable2faEmail, disable2fa, becomeSeller,
        logout, refresh,
    } = useAuth()

    const accessToken = tokens?.accessToken ?? null
    const isAdmin = user?.role === 'ADMIN'

    const [tab, setTab] = useState<Tab>('auth')

    // Auth fields
    const [username, setUsername] = useState('demo')
    const [password, setPassword] = useState('demo')
    const [requestedRole, setRequestedRole] = useState<'BUYER' | 'SELLER'>('BUYER')
    const [verifyToken, setVerifyToken] = useState('')
    const [otp, setOtp] = useState('')

    // Profile fields
    const [profile, setProfile] = useState<Api.UserProfile>({ displayName: null, photoUrl: null, shippingAddress: null })
    const [profileMsg, setProfileMsg] = useState('')

    // Sessions
    const [sessions, setSessions] = useState<Api.SessionRow[] | null>(null)
    const [sessionsMsg, setSessionsMsg] = useState('')

    // Admin users
    const [adminUsers, setAdminUsers] = useState<Api.AdminUserRow[] | null>(null)
    const [adminMsg, setAdminMsg] = useState('')
    const [roleInput, setRoleInput] = useState('')

    // Admin RBAC
    const [roles, setRoles] = useState<string[]>([])
    const [perms, setPerms] = useState<Api.PermissionRow[]>([])
    const [newRole, setNewRole] = useState('')
    const [newPermKey, setNewPermKey] = useState('bid:place')
    const [newPermDesc, setNewPermDesc] = useState('Place bid')
    const [selectedRole, setSelectedRole] = useState('BUYER')
    const [selectedRolePerms, setSelectedRolePerms] = useState<string[]>([])
    const [rbacMsg, setRbacMsg] = useState('')

    // Auto-fill verify token if backend returns it
    useEffect(() => {
        if (lastVerificationToken) setVerifyToken(lastVerificationToken)
    }, [lastVerificationToken])

    // Helpers
    const canCall = !!accessToken

    async function onRegister() {
        await register(username, password, requestedRole)
    }
    async function onVerifyEmail() {
        await verifyEmail(verifyToken)
    }
    async function onLogin() {
        await login(username, password)
    }
    async function onVerifyOtp() {
        await submitMfa(otp)
        setOtp('')
    }

    async function loadProfile() {
        if (!accessToken) return
        setProfileMsg('loading...')
        try {
            const p = await Api.getMyProfile(accessToken)
            setProfile(p)
            setProfileMsg('loaded')
        } catch (e) {
            setProfileMsg('failed')
        }
    }

    async function saveProfile() {
        if (!accessToken) return
        setProfileMsg('saving...')
        try {
            await Api.updateMyProfile(accessToken, profile)
            setProfileMsg('saved')
        } catch {
            setProfileMsg('failed')
        }
    }

    async function loadSessions() {
        if (!accessToken) return
        setSessionsMsg('loading...')
        try {
            const s = await Api.sessions(accessToken)
            setSessions(s)
            setSessionsMsg('loaded')
        } catch {
            setSessionsMsg('failed')
        }
    }

    async function revokeOneSession(token: string) {
        if (!accessToken) return
        setSessionsMsg('revoking...')
        try {
            await Api.revokeSession(accessToken, token)
            await loadSessions()
            setSessionsMsg('revoked')
        } catch {
            setSessionsMsg('failed')
        }
    }

    async function loadAdminUsers() {
        if (!accessToken) return
        setAdminMsg('loading...')
        try {
            const u = await Api.adminListUsers(accessToken)
            setAdminUsers(u)
            setAdminMsg('loaded')
        } catch {
            setAdminMsg('failed')
        }
    }

    async function adminDisable(id: number, disabled: boolean) {
        if (!accessToken) return
        setAdminMsg('updating...')
        try {
            await Api.adminDisableUser(accessToken, id, disabled)
            await loadAdminUsers()
            setAdminMsg('updated')
        } catch {
            setAdminMsg('failed')
        }
    }

    async function adminSetRole(id: number) {
        if (!accessToken) return
        const r = roleInput.trim()
        if (!r) return
        setAdminMsg('updating role...')
        try {
            await Api.adminSetUserRole(accessToken, id, r)
            await loadAdminUsers()
            setAdminMsg('role updated')
        } catch {
            setAdminMsg('failed')
        }
    }

    async function loadRbac() {
        if (!accessToken) return
        setRbacMsg('loading...')
        try {
            const [r, p] = await Promise.all([Api.adminListRoles(accessToken), Api.adminListPermissions(accessToken)])
            setRoles(r)
            setPerms(p)
            setSelectedRole(r.includes(selectedRole) ? selectedRole : (r[0] ?? 'BUYER'))
            setRbacMsg('loaded')
        } catch {
            setRbacMsg('failed')
        }
    }

    async function loadRolePerms(role: string) {
        if (!accessToken) return
        setRbacMsg('loading role perms...')
        try {
            const rp = await Api.adminListRolePerms(accessToken, role)
            setSelectedRolePerms(rp)
            setRbacMsg('loaded')
        } catch {
            setRbacMsg('failed')
        }
    }

    async function createRole() {
        if (!accessToken) return
        setRbacMsg('creating role...')
        try {
            await Api.adminCreateRole(accessToken, newRole)
            setNewRole('')
            await loadRbac()
            setRbacMsg('role created')
        } catch {
            setRbacMsg('failed')
        }
    }

    async function createPerm() {
        if (!accessToken) return
        setRbacMsg('creating permission...')
        try {
            await Api.adminCreatePermission(accessToken, newPermKey, newPermDesc)
            await loadRbac()
            setRbacMsg('permission created')
        } catch {
            setRbacMsg('failed')
        }
    }

    async function saveRolePerms() {
        if (!accessToken) return
        setRbacMsg('saving role perms...')
        try {
            await Api.adminSetRolePerms(accessToken, selectedRole, selectedRolePerms)
            setRbacMsg('saved')
        } catch {
            setRbacMsg('failed')
        }
    }

    // When tab changes and logged in, auto load relevant data
    useEffect(() => {
        if (!accessToken) return
        if (tab === 'profile') void loadProfile()
        if (tab === 'sessions') void loadSessions()
        if (tab === 'admin' && isAdmin) {
            void loadAdminUsers()
            void loadRbac()
            void loadRolePerms(selectedRole)
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [tab, accessToken])

    const tabs = useMemo(() => {
        const base: { key: Tab; label: string; show: boolean }[] = [
            { key: 'auth', label: 'Auth', show: true },
            { key: 'profile', label: 'Profile', show: !!user },
            { key: 'sessions', label: 'Sessions', show: !!user },
            { key: 'admin', label: 'Admin', show: !!user && isAdmin },
        ]
        return base.filter(t => t.show)
    }, [user, isAdmin])

    return (
        <div style={{ display: 'grid', gap: 12, maxWidth: 900, margin: '0 auto' }}>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                {tabs.map(t => (
                    <button key={t.key} onClick={() => setTab(t.key)} disabled={loading} style={tab === t.key ? activeTab : tabBtn}>
                        {t.label}
                    </button>
                ))}
            </div>

            {/* AUTH TAB */}
            {tab === 'auth' && (
                <div style={card}>
                    {!user ? (
                        <>
                            <h3 style={{ marginTop: 0 }}>Sign in / Register</h3>

                            <label>
                                Username / Email
                                <input value={username} onChange={(e) => setUsername(e.target.value)} />
                            </label>

                            <label>
                                Password
                                <input value={password} onChange={(e) => setPassword(e.target.value)} type="password" />
                            </label>

                            <label>
                                Register as
                                <select value={requestedRole} onChange={(e) => setRequestedRole(e.target.value as any)}>
                                    <option value="BUYER">Buyer</option>
                                    <option value="SELLER">Seller</option>
                                </select>
                            </label>

                            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                                <button onClick={() => void onRegister()} disabled={loading}>Register</button>
                                <button onClick={() => void onLogin()} disabled={loading}>Login</button>
                            </div>

                            <div style={subcard}>
                                <div style={{ fontWeight: 700 }}>Email verification</div>
                                {lastVerificationToken ? (
                                    <div style={{ fontSize: 12, marginTop: 6 }}>
                                        Token: <code>{lastVerificationToken}</code>
                                    </div>
                                ) : null}
                                <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 10 }}>
                                    <input value={verifyToken} onChange={(e) => setVerifyToken(e.target.value)} placeholder="paste token" style={{ flex: '1 1 280px' }} />
                                    <button onClick={() => void onVerifyEmail()} disabled={loading || !verifyToken}>Verify</button>
                                </div>
                            </div>

                            {pendingMfa ? (
                                <div style={subcard}>
                                    <div style={{ fontWeight: 700 }}>2FA required ({pendingMfa.method})</div>
                                    {pendingMfa.devCode ? (
                                        <div style={{ fontSize: 12, marginTop: 6 }}>
                                            Dev OTP: <code>{pendingMfa.devCode}</code>
                                        </div>
                                    ) : null}
                                    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 10 }}>
                                        <input value={otp} onChange={(e) => setOtp(e.target.value)} placeholder="6 digit" style={{ flex: '1 1 140px' }} />
                                        <button onClick={() => void onVerifyOtp()} disabled={loading || !otp}>Verify OTP</button>
                                        <button onClick={cancelMfa} disabled={loading}>Cancel</button>
                                    </div>
                                </div>
                            ) : null}
                        </>
                    ) : (
                        <>
                            <h3 style={{ marginTop: 0 }}>Account</h3>
                            <div>Logged in as <b>{user.username}</b> ({user.role})</div>

                            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                                <button onClick={() => void refresh()} disabled={loading}>Refresh token</button>
                                <button onClick={() => void logout()} disabled={loading}>Logout</button>
                                {user.role === 'BUYER' && (
                                    <button onClick={() => void becomeSeller()} disabled={loading}>Become a Seller</button>
                                )}
                            </div>

                            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                                <button onClick={() => void enable2faEmail()} disabled={loading}>Enable 2FA (Email)</button>
                                <button onClick={() => void disable2fa()} disabled={loading}>Disable 2FA</button>
                            </div>
                        </>
                    )}

                    <div style={{ fontSize: 12, marginTop: 8, opacity: 0.9 }}>
                        Status: {loading ? 'loading…' : 'idle'}{error ? ` | error: ${error}` : ''}
                    </div>
                </div>
            )}

            {/* PROFILE TAB */}
            {tab === 'profile' && user && (
                <div style={card}>
                    <h3 style={{ marginTop: 0 }}>Profile</h3>
                    <div style={{ display: 'grid', gap: 10 }}>
                        <label>
                            Display name
                            <input value={profile.displayName ?? ''} onChange={(e) => setProfile({ ...profile, displayName: e.target.value || null })} />
                        </label>
                        <label>
                            Photo URL
                            <input value={profile.photoUrl ?? ''} onChange={(e) => setProfile({ ...profile, photoUrl: e.target.value || null })} />
                        </label>
                        <label>
                            Shipping address
                            <textarea value={profile.shippingAddress ?? ''} onChange={(e) => setProfile({ ...profile, shippingAddress: e.target.value || null })} />
                        </label>

                        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                            <button onClick={() => void loadProfile()} disabled={!canCall || loading}>Reload</button>
                            <button onClick={() => void saveProfile()} disabled={!canCall || loading}>Save</button>
                            <span style={{ fontSize: 12, opacity: 0.8 }}>{profileMsg}</span>
                        </div>
                    </div>
                </div>
            )}

            {/* SESSIONS TAB */}
            {tab === 'sessions' && user && (
                <div style={card}>
                    <h3 style={{ marginTop: 0 }}>Active sessions</h3>
                    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
                        <button onClick={() => void loadSessions()} disabled={!canCall || loading}>Reload</button>
                        <span style={{ fontSize: 12, opacity: 0.8 }}>{sessionsMsg}</span>
                    </div>

                    {sessions?.length ? (
                        <div style={{ overflowX: 'auto', marginTop: 10 }}>
                            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                                <thead>
                                <tr>
                                    <th style={th}>Token</th>
                                    <th style={th}>Created</th>
                                    <th style={th}>Last seen</th>
                                    <th style={th}>Revoked</th>
                                    <th style={th}></th>
                                </tr>
                                </thead>
                                <tbody>
                                {sessions.map(s => (
                                    <tr key={s.token}>
                                        <td style={td}><code>{s.token.slice(0, 8)}…</code></td>
                                        <td style={td}>{fmt(s.createdAt)}</td>
                                        <td style={td}>{fmt(s.lastSeenAt)}</td>
                                        <td style={td}>{s.revokedAt ? fmt(s.revokedAt) : '-'}</td>
                                        <td style={td}>
                                            <button onClick={() => void revokeOneSession(s.token)} disabled={loading}>
                                                Revoke
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    ) : (
                        <div style={{ opacity: 0.8, marginTop: 10 }}>No sessions</div>
                    )}
                </div>
            )}

            {/* ADMIN TAB */}
            {tab === 'admin' && user && isAdmin && (
                <div style={card}>
                    <h3 style={{ marginTop: 0 }}>Admin</h3>

                    <div style={subcard}>
                        <div style={{ fontWeight: 700 }}>Users</div>
                        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center', marginTop: 8 }}>
                            <button onClick={() => void loadAdminUsers()} disabled={!canCall || loading}>Reload users</button>
                            <span style={{ fontSize: 12, opacity: 0.8 }}>{adminMsg}</span>
                        </div>

                        {adminUsers?.length ? (
                            <div style={{ overflowX: 'auto', marginTop: 10 }}>
                                <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                                    <thead>
                                    <tr>
                                        <th style={th}>ID</th>
                                        <th style={th}>Username</th>
                                        <th style={th}>Role</th>
                                        <th style={th}>Disabled</th>
                                        <th style={th}>Actions</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {adminUsers.map(u => (
                                        <tr key={u.id}>
                                            <td style={td}>{u.id}</td>
                                            <td style={td}>{u.username}</td>
                                            <td style={td}>{u.role}</td>
                                            <td style={td}>{u.disabled ? 'yes' : 'no'}</td>
                                            <td style={td}>
                                                <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                                                    <input
                                                        placeholder="role"
                                                        value={roleInput}
                                                        onChange={(e) => setRoleInput(e.target.value)}
                                                        style={{ width: 120 }}
                                                    />
                                                    <button onClick={() => void adminSetRole(u.id)} disabled={loading}>Set role</button>
                                                    <button onClick={() => void adminDisable(u.id, !u.disabled)} disabled={loading}>
                                                        {u.disabled ? 'Enable' : 'Disable'}
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>
                        ) : (
                            <div style={{ opacity: 0.8, marginTop: 10 }}>No users loaded</div>
                        )}
                    </div>

                    <div style={subcard}>
                        <div style={{ fontWeight: 700 }}>RBAC (roles & permissions)</div>

                        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 8, alignItems: 'center' }}>
                            <button onClick={() => void loadRbac()} disabled={!canCall || loading}>Reload RBAC</button>
                            <span style={{ fontSize: 12, opacity: 0.8 }}>{rbacMsg}</span>
                        </div>

                        <div style={{ display: 'grid', gap: 10, marginTop: 10 }}>
                            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                                <input placeholder="NEW ROLE (e.g. MODERATOR)" value={newRole} onChange={(e) => setNewRole(e.target.value)} />
                                <button onClick={() => void createRole()} disabled={loading || !newRole}>Create role</button>
                            </div>

                            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                                <input placeholder="perm key (bid:place)" value={newPermKey} onChange={(e) => setNewPermKey(e.target.value)} />
                                <input placeholder="description" value={newPermDesc} onChange={(e) => setNewPermDesc(e.target.value)} />
                                <button onClick={() => void createPerm()} disabled={loading || !newPermKey}>Create permission</button>
                            </div>

                            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
                                <label>
                                    Role:
                                    <select
                                        value={selectedRole}
                                        onChange={(e) => {
                                            const r = e.target.value
                                            setSelectedRole(r)
                                            void loadRolePerms(r)
                                        }}
                                    >
                                        {roles.map(r => <option key={r} value={r}>{r}</option>)}
                                    </select>
                                </label>
                                <button onClick={() => void loadRolePerms(selectedRole)} disabled={loading}>Load role perms</button>
                                <button onClick={() => void saveRolePerms()} disabled={loading}>Save role perms</button>
                            </div>

                            <div style={{ display: 'grid', gap: 6 }}>
                                <div style={{ fontSize: 12, opacity: 0.8 }}>Permissions (tick to grant to role)</div>
                                <div style={{ display: 'grid', gap: 6, gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))' }}>
                                    {perms.map(p => {
                                        const checked = selectedRolePerms.includes(p.key)
                                        return (
                                            <label key={p.key} style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                                                <input
                                                    type="checkbox"
                                                    checked={checked}
                                                    onChange={() => {
                                                        setSelectedRolePerms(prev => checked ? prev.filter(x => x !== p.key) : [...prev, p.key])
                                                    }}
                                                />
                                                <span><code>{p.key}</code> <span style={{ opacity: 0.7 }}>{p.description ?? ''}</span></span>
                                            </label>
                                        )
                                    })}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            <div style={{ fontSize: 12, opacity: 0.85 }}>
                {tokens?.accessToken ? <>Access: <code>{tokens.accessToken.slice(0, 10)}…</code></> : null}
            </div>
        </div>
    )
}

const card: CSSProperties = { border: '1px solid #333', borderRadius: 12, padding: 14 }
const subcard: CSSProperties = { border: '1px solid #444', borderRadius: 12, padding: 12, marginTop: 12 }
const tabBtn: CSSProperties = { padding: '6px 10px', borderRadius: 10, border: '1px solid #333' }
const activeTab: CSSProperties = { ...tabBtn, fontWeight: 700, background: '#222' }
const th: CSSProperties = { borderBottom: '1px solid #ccc', padding: 6, textAlign: 'left' }
const td: CSSProperties = { borderBottom: '1px solid #eee', padding: 6, verticalAlign: 'top' }

function fmt(iso: string) {
    const d = new Date(iso)
    if (Number.isNaN(d.getTime())) return iso
    return d.toLocaleString()
}