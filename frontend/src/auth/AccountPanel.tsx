import { useEffect, useMemo, useRef, useState } from 'react'
import type { CSSProperties } from 'react'
import * as Api from '../api/auth'
import { useAuth } from './useAuth'
import './AccountPanel.css'

type Tab = 'auth' | 'profile' | 'admin'
type AuthMode = 'login' | 'register'

function EyeIcon({ open }: { open: boolean }) {
    return open ? (
        <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7Z" />
            <circle cx="12" cy="12" r="3" />
        </svg>
    ) : (
        <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M3 3l18 18" />
            <path d="M10.6 10.7A3 3 0 0 0 13.4 13.5" />
            <path d="M9.9 5.1A10.9 10.9 0 0 1 12 5c6.5 0 10 7 10 7a17.2 17.2 0 0 1-4.1 5.1" />
            <path d="M6.7 6.7C4.2 8.3 2.7 10.8 2 12c0 0 3.5 7 10 7a9.8 9.8 0 0 0 5.3-1.5" />
        </svg>
    )
}

function prettyErrorMessage(raw: string | null | undefined): string {
    const value = (raw ?? '').trim()

    switch (value) {
        case 'invalid_credentials':
            return 'Username/email atau password salah.'
        case 'invalid_or_expired_token':
            return 'Kode verifikasi atau OTP tidak valid, atau sudah kedaluwarsa.'
        case 'invalid_input':
            return 'Input belum lengkap atau tidak valid.'
        case 'username_taken':
            return 'Username/email sudah terdaftar.'
        case 'email_not_verified':
            return 'Akun belum diverifikasi. Selesaikan verifikasi email terlebih dahulu.'
        case 'unauthorized':
            return 'Kamu tidak punya akses untuk melakukan aksi ini.'
        case 'user_not_found':
            return 'User tidak ditemukan.'
        case 'cannot_delete_self':
            return 'Admin tidak bisa menghapus akun sendiri.'
        case 'cannot_delete_admin':
            return 'Akun admin tidak boleh dihapus.'
        default:
            return value ? value.replaceAll('_', ' ') : 'Terjadi kesalahan. Silakan coba lagi.'
    }
}

export function AccountPanel() {
    const {
        user, tokens, loading, error,
        lastVerificationToken, pendingMfa,
        register, verifyEmail, login, submitMfa, cancelMfa,
        becomeSeller, logout,
    } = useAuth()

    const accessToken = tokens?.accessToken ?? null
    const isAdmin = user?.role === 'ADMIN'

    const [tab, setTab] = useState<Tab>('auth')
    const [authMode, setAuthMode] = useState<AuthMode>('login')

    const [username, setUsername] = useState('')
    const [password, setPassword] = useState('')
    const [showPassword, setShowPassword] = useState(false)
    const [requestedRole, setRequestedRole] = useState<'BUYER' | 'SELLER'>('BUYER')
    const [verifyToken, setVerifyToken] = useState('')
    const [otp, setOtp] = useState('')

    const [toast, setToast] = useState<{ type: 'error'; message: string } | null>(null)
    const didInitErrorEffect = useRef(false)

    const [profile, setProfile] = useState<Api.UserProfile>({
        displayName: null,
        photoUrl: null,
        shippingAddress: null,
    })
    const [profileMsg, setProfileMsg] = useState('')
    const [photoOk, setPhotoOk] = useState(true)

    const [adminUsers, setAdminUsers] = useState<Api.AdminUserRow[] | null>(null)
    const [adminMsg, setAdminMsg] = useState('')
    const [roleDraftById, setRoleDraftById] = useState<Record<number, string>>({})

    const [roles, setRoles] = useState<string[]>([])
    const [perms, setPerms] = useState<Api.PermissionRow[]>([])
    const [newRole, setNewRole] = useState('')
    const [newPermKey, setNewPermKey] = useState('bid:place')
    const [newPermDesc, setNewPermDesc] = useState('Place bid')
    const [selectedRole, setSelectedRole] = useState('BUYER')
    const [selectedRolePerms, setSelectedRolePerms] = useState<string[]>([])
    const [rbacMsg, setRbacMsg] = useState('')

    const canCall = !!accessToken

    async function onRegister() {
        await register(username, password, requestedRole)
        setAuthMode('register')
        setVerifyToken('')
    }

    async function onVerifyEmail() {
        await verifyEmail(verifyToken.trim(), username)
        setVerifyToken('')
    }

    async function onLogin() {
        setOtp('')
        await login(username, password)
    }

    async function onVerifyOtp() {
        await submitMfa(otp.trim())
        setOtp('')
    }

    async function onLogout() {
        setUsername('')
        setPassword('')
        setShowPassword(false)
        setVerifyToken('')
        setOtp('')
        setToast(null)
        setAuthMode('login')
        setTab('auth')
        await logout()
    }

    const showVerifyBox = authMode === 'register' && !!lastVerificationToken
    const showLoginMfaBox = authMode === 'login' && !!pendingMfa

    async function loadProfile() {
        if (!accessToken) return
        setProfileMsg('loading...')
        try {
            const p = await Api.getMyProfile(accessToken)
            setProfile(p)
            setProfileMsg('loaded')
        } catch {
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

    useEffect(() => {
        setPhotoOk(true)
    }, [profile.photoUrl])

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

    async function adminDelete(id: number, usernameText: string, role: string) {
        if (!accessToken) return

        if (role.toUpperCase() === 'ADMIN') {
            setAdminMsg('admin user cannot be deleted')
            return
        }

        const ok = window.confirm(`Delete user "${usernameText}" permanently?`)
        if (!ok) return

        setAdminMsg('deleting...')
        try {
            await Api.adminDeleteUser(accessToken, id)
            await loadAdminUsers()
            setAdminMsg('deleted')
        } catch {
            setAdminMsg('failed')
        }
    }

    async function adminSetRole(id: number) {
        if (!accessToken) return

        const currentRole = adminUsers?.find(u => u.id === id)?.role ?? ''
        const desiredRole = (roleDraftById[id] ?? currentRole).trim()
        if (!desiredRole) return

        if (desiredRole === currentRole) {
            setAdminMsg('no changes')
            return
        }

        setAdminMsg('updating role...')
        try {
            await Api.adminSetUserRole(accessToken, id, desiredRole)

            setRoleDraftById(prev => {
                const next = { ...prev }
                delete next[id]
                return next
            })

            await loadAdminUsers()
            setAdminMsg('role updated (user must re-login)')
        } catch {
            setAdminMsg('failed')
        }
    }

    async function loadRbac() {
        if (!accessToken) return
        setRbacMsg('loading...')
        try {
            const [r, p] = await Promise.all([
                Api.adminListRoles(accessToken),
                Api.adminListPermissions(accessToken),
            ])
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

    useEffect(() => {
        if (!accessToken) return
        if (tab === 'profile') void loadProfile()
        if (tab === 'admin' && isAdmin) {
            void loadAdminUsers()
            void loadRbac()
            void loadRolePerms(selectedRole)
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [tab, accessToken])

    useEffect(() => {
        setShowPassword(false)
        setToast(null)
    }, [authMode])

    useEffect(() => {
        if (user) return
        setUsername('')
        setPassword('')
        setShowPassword(false)
        setVerifyToken('')
        setOtp('')
        setToast(null)
    }, [user])

    useEffect(() => {
        if (!didInitErrorEffect.current) {
            didInitErrorEffect.current = true
            return
        }

        if (!error) {
            setToast(null)
            return
        }

        setToast({
            type: 'error',
            message: prettyErrorMessage(error),
        })

        const timer = window.setTimeout(() => {
            setToast(null)
        }, 3500)

        return () => window.clearTimeout(timer)
    }, [error])

    const tabs = useMemo(() => {
        const base: { key: Tab; label: string; show: boolean }[] = [
            { key: 'auth', label: 'Auth', show: true },
            { key: 'profile', label: 'Profile', show: !!user },
            { key: 'admin', label: 'Admin', show: !!user && isAdmin },
        ]
        return base.filter(t => t.show)
    }, [user, isAdmin])

    return (
        <div className="ap-root">
            <div className="ap-tabRow">
                {tabs.map(t => (
                    <button
                        key={t.key}
                        onClick={() => setTab(t.key)}
                        disabled={loading}
                        style={tab === t.key ? activeTab : tabBtn}
                    >
                        {t.label}
                    </button>
                ))}
            </div>

            {tab === 'auth' && (
                <div style={{ ...card, position: 'relative' }} className="ap-card">
                    {toast ? (
                        <div className="ap-toastWrap">
                            <div role="alert" aria-live="assertive" className="ap-toast ap-toastError">
                                <div className="ap-toastIcon">⚠️</div>
                                <div className="ap-toastBody">
                                    <div className="ap-toastTitle">Error</div>
                                    <div className="ap-toastText">{toast.message}</div>
                                </div>
                                <button
                                    type="button"
                                    onClick={() => setToast(null)}
                                    aria-label="Close notification"
                                    className="ap-toastClose"
                                >
                                    ×
                                </button>
                            </div>
                        </div>
                    ) : null}

                    {!user ? (
                        <div className="ap-authShell">
                            <div className="ap-authFormCard">
                                <div className="ap-authHeader">
                                    <h3 className="ap-authTitle">Sign in / Register</h3>
                                    <p className="ap-authSubtitle">
                                        Masuk untuk mulai bid, jual barang, dan kelola akunmu.
                                    </p>
                                </div>

                                <div className="ap-authModeRow">
                                    <button
                                        onClick={() => setAuthMode('login')}
                                        disabled={loading}
                                        style={authMode === 'login' ? activeTab : tabBtn}
                                    >
                                        Login
                                    </button>
                                    <button
                                        onClick={() => setAuthMode('register')}
                                        disabled={loading}
                                        style={authMode === 'register' ? activeTab : tabBtn}
                                    >
                                        Register
                                    </button>
                                </div>

                                <div className="ap-authFields">
                                    <label className="ap-field">
                                        <span className="ap-label">Username / Email</span>
                                        <input
                                            value={username}
                                            onChange={(e) => {
                                                setUsername(e.target.value)
                                                setVerifyToken('')
                                                setOtp('')
                                                setToast(null)
                                            }}
                                            placeholder="Masukkan username atau email"
                                        />
                                    </label>

                                    <label className="ap-field">
                                        <span className="ap-label">Password</span>
                                        <div className="ap-passwordWrap">
                                            <input
                                                type={showPassword ? 'text' : 'password'}
                                                value={password}
                                                onChange={(e) => {
                                                    setPassword(e.target.value)
                                                    setToast(null)
                                                }}
                                                className="ap-passwordInput"
                                                placeholder="Masukkan password"
                                            />
                                            <button
                                                type="button"
                                                onClick={() => setShowPassword(v => !v)}
                                                aria-label={showPassword ? 'Hide password' : 'Show password'}
                                                title={showPassword ? 'Hide password' : 'Show password'}
                                                className="ap-eyeBtn"
                                            >
                                                <EyeIcon open={showPassword} />
                                            </button>
                                        </div>
                                    </label>

                                    {authMode === 'register' ? (
                                        <label className="ap-field">
                                            <span className="ap-label">Register as</span>
                                            <select
                                                value={requestedRole}
                                                onChange={(e) => {
                                                    const v = e.target.value
                                                    setRequestedRole(v === 'SELLER' ? 'SELLER' : 'BUYER')
                                                }}
                                            >
                                                <option value="BUYER">Buyer</option>
                                                <option value="SELLER">Seller</option>
                                            </select>
                                        </label>
                                    ) : null}
                                </div>

                                <div className="ap-authSubmit">
                                    {authMode === 'register' ? (
                                        <button onClick={() => void onRegister()} disabled={loading} className="ap-primaryBtn">
                                            Register
                                        </button>
                                    ) : (
                                        <button onClick={() => void onLogin()} disabled={loading} className="ap-primaryBtn">
                                            Login
                                        </button>
                                    )}
                                </div>

                                {showVerifyBox ? (
                                    <div style={subcard} className="ap-authSubcard">
                                        <div className="ap-subcardTitle">Email verification</div>
                                        <div className="ap-helpText ap-helpInfo">
                                            Registrasi berhasil. Masukkan kode verifikasi untuk menyelesaikan aktivasi akun.
                                        </div>

                                        <div className="ap-authInlineAction">
                                            <input
                                                value={verifyToken}
                                                onChange={(e) => {
                                                    setVerifyToken(e.target.value)
                                                    setToast(null)
                                                }}
                                                placeholder="Masukkan kode verifikasi"
                                                autoComplete="off"
                                                spellCheck={false}
                                            />
                                            <button onClick={() => void onVerifyEmail()} disabled={loading || !verifyToken.trim()}>
                                                Verify
                                            </button>
                                        </div>
                                    </div>
                                ) : null}

                                {showLoginMfaBox ? (
                                    <div style={subcard} className="ap-authSubcard">
                                        <div className="ap-subcardTitle">2FA required ({pendingMfa?.method})</div>
                                        <div className="ap-helpText ap-helpInfo">
                                            Login membutuhkan OTP. Masukkan kode OTP untuk melanjutkan.
                                        </div>

                                        <div className="ap-authInlineAction">
                                            <input
                                                value={otp}
                                                onChange={(e) => {
                                                    setOtp(e.target.value)
                                                    setToast(null)
                                                }}
                                                placeholder="6 digit OTP"
                                                autoComplete="one-time-code"
                                                spellCheck={false}
                                            />
                                            <button onClick={() => void onVerifyOtp()} disabled={loading || !otp.trim()}>
                                                Verify OTP
                                            </button>
                                            <button onClick={cancelMfa} disabled={loading}>
                                                Cancel
                                            </button>
                                        </div>
                                    </div>
                                ) : null}
                            </div>
                        </div>
                    ) : (
                        <>
                            <h3 className="ap-title">Account</h3>
                            <div className="ap-userMeta">
                                Logged in as <b>{user.username}</b> ({user.role})
                            </div>

                            <div className="ap-inlineRow ap-primaryActions">
                                <button onClick={() => void onLogout()} disabled={loading}>Logout</button>
                                {user.role === 'BUYER' && (
                                    <button onClick={() => void becomeSeller()} disabled={loading}>Become a Seller</button>
                                )}
                            </div>
                        </>
                    )}
                </div>
            )}

            {tab === 'profile' && user && (
                <div style={card} className="ap-card">
                    <h3 className="ap-title">Profile</h3>

                    <div className="ap-profileGrid">
                        <div className="ap-previewCard">
                            <div className="ap-avatarLg" aria-label="Profile photo preview">
                                {profile.photoUrl && photoOk ? (
                                    <img
                                        src={profile.photoUrl}
                                        alt="Profile"
                                        referrerPolicy="no-referrer"
                                        onError={() => setPhotoOk(false)}
                                    />
                                ) : (
                                    <span aria-hidden="true">{initials(profile.displayName ?? user.username)}</span>
                                )}
                            </div>

                            <div className="ap-previewText">
                                <div className="ap-previewName">{profile.displayName?.trim() || user.username}</div>
                                <div className="ap-previewMeta">{user.username} · {user.role}</div>
                                <div className="ap-previewHint">
                                    Paste a photo URL to preview instantly. If the URL is invalid or blocked,
                                    the avatar falls back to your initials.
                                </div>
                            </div>
                        </div>

                        <div className="ap-profileForm">
                            <label className="ap-field">
                                <span className="ap-label">Display name</span>
                                <input
                                    value={profile.displayName ?? ''}
                                    onChange={(e) => setProfile({ ...profile, displayName: e.target.value || null })}
                                />
                            </label>

                            <label className="ap-field">
                                <span className="ap-label">Photo URL</span>
                                <input
                                    value={profile.photoUrl ?? ''}
                                    onChange={(e) => setProfile({ ...profile, photoUrl: e.target.value || null })}
                                    placeholder="https://..."
                                    inputMode="url"
                                />
                            </label>

                            <label className="ap-field">
                                <span className="ap-label">Shipping address</span>
                                <textarea
                                    value={profile.shippingAddress ?? ''}
                                    onChange={(e) => setProfile({ ...profile, shippingAddress: e.target.value || null })}
                                />
                            </label>

                            <div className="ap-actions">
                                <button onClick={() => void loadProfile()} disabled={!canCall || loading}>Reload</button>
                                <button onClick={() => void saveProfile()} disabled={!canCall || loading}>Save</button>
                                <span className="ap-statusText">{profileMsg}</span>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {tab === 'admin' && user && isAdmin && (
                <div style={card} className="ap-card">
                    <h3 className="ap-title">Admin</h3>

                    <div style={subcard} className="ap-subcard">
                        <div className="ap-subcardTitle">Users</div>
                        <div className="ap-inlineRow ap-toolbarRow">
                            <button onClick={() => void loadAdminUsers()} disabled={!canCall || loading}>Reload users</button>
                            <span className="ap-statusText">{adminMsg}</span>
                        </div>

                        {adminUsers?.length ? (
                            <div className="ap-tableScroll">
                                <table className="ap-table">
                                    <thead>
                                    <tr>
                                        <th style={th}>No.</th>
                                        <th style={th}>Username</th>
                                        <th style={th}>Role</th>
                                        <th style={th}>Disabled</th>
                                        <th style={th}>Actions</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {adminUsers.map((u, index) => {
                                        const options = roles.length ? Array.from(new Set([...roles, u.role])) : [u.role]
                                        const value = roleDraftById[u.id] ?? u.role

                                        return (
                                            <tr key={u.id}>
                                                <td style={td} title={`DB ID: ${u.id}`}>{index + 1}</td>
                                                <td style={td}>{u.username}</td>
                                                <td style={td}>{u.role}</td>
                                                <td style={td}>{u.disabled ? 'yes' : 'no'}</td>
                                                <td style={td}>
                                                    <div className="ap-adminActionRow">
                                                        {roles.length ? (
                                                            <select
                                                                value={value}
                                                                onChange={(e) => setRoleDraftById(prev => ({ ...prev, [u.id]: e.target.value }))}
                                                                className="ap-roleSelect"
                                                            >
                                                                {options.map(r => <option key={r} value={r}>{r}</option>)}
                                                            </select>
                                                        ) : (
                                                            <input
                                                                placeholder={`Current: ${u.role}`}
                                                                value={roleDraftById[u.id] ?? ''}
                                                                onChange={(e) => setRoleDraftById(prev => ({ ...prev, [u.id]: e.target.value }))}
                                                                className="ap-roleSelect"
                                                            />
                                                        )}

                                                        <button onClick={() => void adminSetRole(u.id)} disabled={loading}>Set role</button>

                                                        <button onClick={() => void adminDisable(u.id, !u.disabled)} disabled={loading}>
                                                            {u.disabled ? 'Enable' : 'Disable'}
                                                        </button>

                                                        <button
                                                            onClick={() => void adminDelete(u.id, u.username, u.role)}
                                                            disabled={loading || u.role.toUpperCase() === 'ADMIN'}
                                                            title={u.role.toUpperCase() === 'ADMIN' ? 'Admin user cannot be deleted' : 'Delete user'}
                                                        >
                                                            Delete
                                                        </button>
                                                    </div>
                                                </td>
                                            </tr>
                                        )
                                    })}
                                    </tbody>
                                </table>
                            </div>
                        ) : (
                            <div className="ap-emptyState">No users loaded</div>
                        )}
                    </div>

                    <div style={subcard} className="ap-subcard">
                        <div className="ap-subcardTitle">RBAC (roles & permissions)</div>

                        <div className="ap-inlineRow ap-toolbarRow">
                            <button onClick={() => void loadRbac()} disabled={!canCall || loading}>Reload RBAC</button>
                            <span className="ap-statusText">{rbacMsg}</span>
                        </div>

                        <div style={{ display: 'grid', gap: 10, marginTop: 10 }}>
                            <div className="ap-rbacCreateGrid">
                                <div className="ap-rbacCreateCard">
                                    <div className="ap-rbacCreateHeader">
                                        <div className="ap-rbacCreateTitle">Create role</div>
                                        <div className="ap-rbacCreateHint">
                                            Tambahkan role baru untuk kebutuhan permission yang lebih spesifik.
                                        </div>
                                    </div>

                                    <label className="ap-field ap-rbacField">
                                        <span className="ap-label">Role name</span>
                                        <input
                                            placeholder="e.g. MODERATOR"
                                            value={newRole}
                                            onChange={(e) => setNewRole(e.target.value)}
                                        />
                                    </label>

                                    <div className="ap-rbacCreateAction">
                                        <button onClick={() => void createRole()} disabled={loading || !newRole.trim()}>
                                            Create role
                                        </button>
                                    </div>
                                </div>

                                <div className="ap-rbacCreateCard">
                                    <div className="ap-rbacCreateHeader">
                                        <div className="ap-rbacCreateTitle">Create permission</div>
                                        <div className="ap-rbacCreateHint">
                                            Tambahkan permission granular baru yang nanti bisa di-assign ke role tertentu.
                                        </div>
                                    </div>

                                    <div className="ap-rbacPermFields">
                                        <label className="ap-field ap-rbacField">
                                            <span className="ap-label">Permission key</span>
                                            <input
                                                placeholder="e.g. bid:place"
                                                value={newPermKey}
                                                onChange={(e) => setNewPermKey(e.target.value)}
                                            />
                                        </label>

                                        <label className="ap-field ap-rbacField">
                                            <span className="ap-label">Description</span>
                                            <input
                                                placeholder="e.g. Place bid"
                                                value={newPermDesc}
                                                onChange={(e) => setNewPermDesc(e.target.value)}
                                            />
                                        </label>
                                    </div>

                                    <div className="ap-rbacCreateAction">
                                        <button onClick={() => void createPerm()} disabled={loading || !newPermKey.trim()}>
                                            Create permission
                                        </button>
                                    </div>
                                </div>
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
                                                    onChange={() => setSelectedRolePerms(prev => checked ? prev.filter(x => x !== p.key) : [...prev, p.key])}
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
        </div>
    )
}

const card: CSSProperties = {
    border: '1px solid var(--border)',
    borderRadius: 16,
    padding: 16,
    background: '#fff',
    boxShadow: 'var(--shadow)',
}

const subcard: CSSProperties = {
    border: '1px solid var(--border)',
    borderRadius: 16,
    padding: 14,
    marginTop: 12,
    background: 'var(--bg-subtle)',
}

const tabBtn: CSSProperties = {
    padding: '8px 12px',
    borderRadius: 999,
    border: '1px solid var(--border)',
    background: '#fff',
}

const activeTab: CSSProperties = {
    ...tabBtn,
    fontWeight: 800,
    borderColor: 'var(--ebay-blue)',
    background: 'rgba(8, 106, 244, 0.08)',
}

const th: CSSProperties = {
    borderBottom: '1px solid #ccc',
    padding: 6,
    textAlign: 'left',
    whiteSpace: 'nowrap',
}

const td: CSSProperties = {
    borderBottom: '1px solid #eee',
    padding: 6,
    verticalAlign: 'top',
}

function initials(name: string) {
    const parts = name.trim().split(/\s+/g).filter(Boolean).slice(0, 2)
    const s = parts.map(p => p[0]?.toUpperCase() ?? '').join('')
    return s || 'U'
}