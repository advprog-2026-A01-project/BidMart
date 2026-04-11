import { useEffect, useMemo, useRef, useState } from 'react'
import type { CSSProperties } from 'react'
import * as Api from '../api/auth'
import { useAuth } from './useAuth'
import './AccountPanel.css'

type Tab = 'auth' | 'profile' | 'sessions' | 'admin'
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
        case 'totp_not_configured':
            return 'TOTP belum dikonfigurasi.'
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
        enable2faEmail, disable2fa, becomeSeller,
        logout, refresh,
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

    const [totpSecret, setTotpSecret] = useState<string | null>(null)
    const [totpUri, setTotpUri] = useState<string | null>(null)
    const [totpCode, setTotpCode] = useState('')
    const [mfaMsg, setMfaMsg] = useState('')

    const [profile, setProfile] = useState<Api.UserProfile>({ displayName: null, photoUrl: null, shippingAddress: null })
    const [profileMsg, setProfileMsg] = useState('')
    const [photoOk, setPhotoOk] = useState(true)

    const [sessions, setSessions] = useState<Api.SessionRow[] | null>(null)
    const [sessionsMsg, setSessionsMsg] = useState('')

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

    const showVerifyBox = authMode === 'register' && !!lastVerificationToken
    const showLoginMfaBox = authMode === 'login' && !!pendingMfa

    async function onTotpSetup() {
        if (!accessToken) return
        setMfaMsg('setting up TOTP...')
        try {
            const res = await Api.totpSetup(accessToken)
            setTotpSecret(res.secret)
            setTotpUri(res.otpauthUrl)
            setMfaMsg('TOTP secret generated. Add it to your authenticator app, then enter a code to enable.')
        } catch {
            setMfaMsg('failed to setup TOTP')
        }
    }

    async function onTotpEnable() {
        if (!accessToken || !totpCode.trim()) return
        setMfaMsg('enabling TOTP...')
        try {
            await Api.totpEnable(accessToken, totpCode.trim())
            setTotpCode('')
            setMfaMsg('TOTP enabled. Next login will require authenticator code.')
        } catch {
            setMfaMsg('failed to enable TOTP (code might be wrong)')
        }
    }

    async function onTotpDisable() {
        if (!accessToken) return
        setMfaMsg('disabling TOTP...')
        try {
            await Api.totpDisable(accessToken)
            setTotpSecret(null)
            setTotpUri(null)
            setTotpCode('')
            setMfaMsg('TOTP disabled (secret cleared).')
        } catch {
            setMfaMsg('failed to disable TOTP')
        }
    }

    async function copyToClipboard(text: string) {
        try {
            await navigator.clipboard.writeText(text)
            setMfaMsg('copied to clipboard')
        } catch {
            setMfaMsg('copy failed (browser blocked clipboard)')
        }
    }

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

    async function adminDelete(id: number, username: string, role: string) {
        if (!accessToken) return
        if (role.toUpperCase() === 'ADMIN') {
            setAdminMsg('admin user cannot be deleted')
            return
        }

        const ok = window.confirm(`Delete user "${username}" permanently?`)
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

    useEffect(() => {
        setShowPassword(false)
        setToast(null)
    }, [authMode])

    useEffect(() => {
        if (!didInitErrorEffect.current) {
            didInitErrorEffect.current = true
            return
        }

        if (!error) {
            setToast(null)
            return
        }

        setToast({ type: 'error', message: prettyErrorMessage(error) })
        const timer = window.setTimeout(() => setToast(null), 3500)
        return () => window.clearTimeout(timer)
    }, [error])

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
        <div className="ap-root">
            <div className="ap-tabRow">
                {tabs.map(t => (
                    <button key={t.key} onClick={() => setTab(t.key)} disabled={loading} style={tab === t.key ? activeTab : tabBtn}>
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
                        <>
                            <h3 className="ap-title">Sign in / Register</h3>

                            <div className="ap-inlineRow">
                                <button onClick={() => setAuthMode('login')} disabled={loading} style={authMode === 'login' ? activeTab : tabBtn}>Login</button>
                                <button onClick={() => setAuthMode('register')} disabled={loading} style={authMode === 'register' ? activeTab : tabBtn}>Register</button>
                            </div>

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

                            <div className="ap-inlineRow ap-primaryActions">
                                {authMode === 'register' ? (
                                    <button onClick={() => void onRegister()} disabled={loading}>Register</button>
                                ) : (
                                    <button onClick={() => void onLogin()} disabled={loading}>Login</button>
                                )}
                            </div>

                            {showVerifyBox ? (
                                <div style={subcard} className="ap-subcard">
                                    <div className="ap-subcardTitle">Email verification</div>
                                    <div className="ap-helpText ap-helpInfo">Registrasi berhasil. Masukkan kode verifikasi untuk menyelesaikan aktivasi akun.</div>
                                    <div className="ap-inlineRow ap-stackOnMobile">
                                        <input
                                            value={verifyToken}
                                            onChange={(e) => {
                                                setVerifyToken(e.target.value)
                                                setToast(null)
                                            }}
                                            placeholder="Masukkan kode verifikasi"
                                            autoComplete="off"
                                            spellCheck={false}
                                            className="ap-grow"
                                        />
                                        <button onClick={() => void onVerifyEmail()} disabled={loading || !verifyToken.trim()}>Verify</button>
                                    </div>
                                </div>
                            ) : null}

                            {showLoginMfaBox ? (
                                <div style={subcard} className="ap-subcard">
                                    <div className="ap-subcardTitle">2FA required ({pendingMfa?.method})</div>
                                    <div className="ap-helpText ap-helpInfo">Login membutuhkan OTP. Masukkan kode OTP untuk melanjutkan.</div>
                                    <div className="ap-inlineRow ap-stackOnMobile">
                                        <input
                                            value={otp}
                                            onChange={(e) => {
                                                setOtp(e.target.value)
                                                setToast(null)
                                            }}
                                            placeholder="6 digit OTP"
                                            autoComplete="one-time-code"
                                            spellCheck={false}
                                            className="ap-grow ap-otpInput"
                                        />
                                        <button onClick={() => void onVerifyOtp()} disabled={loading || !otp.trim()}>Verify OTP</button>
                                        <button onClick={cancelMfa} disabled={loading}>Cancel</button>
                                    </div>
                                </div>
                            ) : null}
                        </>
                    ) : (
                        <>
                            <h3 className="ap-title">Account</h3>
                            <div className="ap-userMeta">Logged in as <b>{user.username}</b> ({user.role})</div>

                            <div className="ap-inlineRow ap-primaryActions">
                                <button onClick={() => void refresh()} disabled={loading}>Refresh token</button>
                                <button onClick={() => void logout()} disabled={loading}>Logout</button>
                                {user.role === 'BUYER' && <button onClick={() => void becomeSeller()} disabled={loading}>Become a Seller</button>}
                            </div>

                            {isAdmin ? (
                                <div style={subcard} className="ap-subcard">
                                    <div className="ap-subcardTitle">2FA / MFA</div>
                                    <div className="ap-helpText">Admin can still use real MFA controls. Buyer/Seller demo users are handled by the hardcoded verification code and login OTP flow.</div>

                                    <div className="ap-inlineRow ap-primaryActions">
                                        <button onClick={() => void enable2faEmail()} disabled={loading}>Enable 2FA (Email OTP)</button>
                                        <button onClick={() => void disable2fa()} disabled={loading}>Disable 2FA</button>
                                    </div>

                                    <hr className="ap-divider" />

                                    <div className="ap-subcardTitle">TOTP (Authenticator App)</div>
                                    <div className="ap-inlineRow ap-primaryActions">
                                        <button onClick={() => void onTotpSetup()} disabled={loading}>Setup / Regenerate Secret</button>
                                        <button onClick={() => void onTotpDisable()} disabled={loading}>Disable TOTP (Clear Secret)</button>
                                    </div>

                                    {totpSecret ? (
                                        <div className="ap-copyCard">
                                            <div className="ap-copyLabel">Secret</div>
                                            <div className="ap-copyRow">
                                                <code>{totpSecret}</code>
                                                <button onClick={() => void copyToClipboard(totpSecret)} disabled={loading}>Copy secret</button>
                                            </div>
                                        </div>
                                    ) : null}

                                    {totpUri ? (
                                        <div className="ap-copyCard">
                                            <div className="ap-copyLabel">otpauth URI</div>
                                            <div className="ap-copyRow">
                                                <code className="ap-breakAll">{totpUri}</code>
                                                <button onClick={() => void copyToClipboard(totpUri)} disabled={loading}>Copy URI</button>
                                            </div>
                                        </div>
                                    ) : null}

                                    <div className="ap-fieldBlock">
                                        <div className="ap-helpText">After adding the secret to your authenticator app, enter the 6-digit code below to enable TOTP.</div>
                                        <div className="ap-inlineRow ap-stackOnMobile">
                                            <input
                                                value={totpCode}
                                                onChange={(e) => setTotpCode(e.target.value)}
                                                placeholder="6 digit TOTP code"
                                                autoComplete="one-time-code"
                                                spellCheck={false}
                                                className="ap-grow ap-otpInput"
                                            />
                                            <button onClick={() => void onTotpEnable()} disabled={loading || !totpCode.trim()}>Enable TOTP</button>
                                        </div>
                                    </div>

                                    <div className="ap-helpText ap-mutedText">{mfaMsg || 'Click Setup to generate a secret, add it to your authenticator app, then enable using a valid code.'}</div>
                                </div>
                            ) : null}
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
                                    <img src={profile.photoUrl} alt="Profile" referrerPolicy="no-referrer" onError={() => setPhotoOk(false)} />
                                ) : (
                                    <span aria-hidden="true">{initials(profile.displayName ?? user.username)}</span>
                                )}
                            </div>

                            <div className="ap-previewText">
                                <div className="ap-previewName">{profile.displayName?.trim() || user.username}</div>
                                <div className="ap-previewMeta">{user.username} · {user.role}</div>
                                <div className="ap-previewHint">Paste a photo URL to preview instantly. If the URL is invalid or blocked, the avatar falls back to your initials.</div>
                            </div>
                        </div>

                        <div className="ap-profileForm">
                            <label className="ap-field">
                                <span className="ap-label">Display name</span>
                                <input value={profile.displayName ?? ''} onChange={(e) => setProfile({ ...profile, displayName: e.target.value || null })} />
                            </label>

                            <label className="ap-field">
                                <span className="ap-label">Photo URL</span>
                                <input value={profile.photoUrl ?? ''} onChange={(e) => setProfile({ ...profile, photoUrl: e.target.value || null })} placeholder="https://..." inputMode="url" />
                            </label>

                            <label className="ap-field">
                                <span className="ap-label">Shipping address</span>
                                <textarea value={profile.shippingAddress ?? ''} onChange={(e) => setProfile({ ...profile, shippingAddress: e.target.value || null })} />
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

            {tab === 'sessions' && user && (
                <div style={card} className="ap-card">
                    <h3 className="ap-title">Active sessions</h3>
                    <div className="ap-inlineRow ap-toolbarRow">
                        <button onClick={() => void loadSessions()} disabled={!canCall || loading}>Reload</button>
                        <span className="ap-statusText">{sessionsMsg}</span>
                    </div>

                    {sessions?.length ? (
                        <div className="ap-tableScroll">
                            <table className="ap-table">
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
                                        <td style={td}><button onClick={() => void revokeOneSession(s.token)} disabled={loading}>Revoke</button></td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    ) : (
                        <div className="ap-emptyState">No sessions</div>
                    )}
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
                                        <th style={th}>ID</th>
                                        <th style={th}>Username</th>
                                        <th style={th}>Role</th>
                                        <th style={th}>Disabled</th>
                                        <th style={th}>Actions</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {adminUsers.map(u => {
                                        const options = roles.length ? Array.from(new Set([...roles, u.role])) : [u.role]
                                        const value = roleDraftById[u.id] ?? u.role

                                        return (
                                            <tr key={u.id}>
                                                <td style={td}>{u.id}</td>
                                                <td style={td}>{u.username}</td>
                                                <td style={td}>{u.role}</td>
                                                <td style={td}>{u.disabled ? 'yes' : 'no'}</td>
                                                <td style={td}>
                                                    <div className="ap-adminActionRow">
                                                        {roles.length ? (
                                                            <select value={value} onChange={(e) => setRoleDraftById(prev => ({ ...prev, [u.id]: e.target.value }))} className="ap-roleSelect">
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
                                                        <button onClick={() => void adminDisable(u.id, !u.disabled)} disabled={loading}>{u.disabled ? 'Enable' : 'Disable'}</button>
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

                        <div className="ap-rbacStack">
                            <div className="ap-inlineRow ap-stackOnMobile">
                                <input placeholder="NEW ROLE (e.g. MODERATOR)" value={newRole} onChange={(e) => setNewRole(e.target.value)} className="ap-grow" />
                                <button onClick={() => void createRole()} disabled={loading || !newRole}>Create role</button>
                            </div>

                            <div className="ap-inlineRow ap-stackOnMobile">
                                <input placeholder="perm key (bid:place)" value={newPermKey} onChange={(e) => setNewPermKey(e.target.value)} className="ap-grow" />
                                <input placeholder="description" value={newPermDesc} onChange={(e) => setNewPermDesc(e.target.value)} className="ap-grow" />
                                <button onClick={() => void createPerm()} disabled={loading || !newPermKey}>Create permission</button>
                            </div>

                            <div className="ap-inlineRow ap-stackOnMobile ap-roleControlRow">
                                <label className="ap-inlineLabel">
                                    <span>Role:</span>
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

                            <div className="ap-permSection">
                                <div className="ap-statusText">Permissions (tick to grant to role)</div>
                                <div className="ap-permGrid">
                                    {perms.map(p => {
                                        const checked = selectedRolePerms.includes(p.key)
                                        return (
                                            <label key={p.key} className="ap-permItem">
                                                <input
                                                    type="checkbox"
                                                    checked={checked}
                                                    onChange={() => setSelectedRolePerms(prev => checked ? prev.filter(x => x !== p.key) : [...prev, p.key])}
                                                />
                                                <span><code>{p.key}</code> <span className="ap-permDesc">{p.description ?? ''}</span></span>
                                            </label>
                                        )
                                    })}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            <div className="ap-tokenHint">
                {tokens?.accessToken ? <>Access: <code>{tokens.accessToken.slice(0, 10)}…</code></> : null}
            </div>
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

const th: CSSProperties = { borderBottom: '1px solid #ccc', padding: 6, textAlign: 'left', whiteSpace: 'nowrap' }
const td: CSSProperties = { borderBottom: '1px solid #eee', padding: 6, verticalAlign: 'top' }

function fmt(iso: string) {
    const d = new Date(iso)
    if (Number.isNaN(d.getTime())) return iso
    return d.toLocaleString()
}

function initials(name: string) {
    const parts = name.trim().split(/\s+/g).filter(Boolean).slice(0, 2)
    const s = parts.map(p => p[0]?.toUpperCase() ?? '').join('')
    return s || 'U'
}