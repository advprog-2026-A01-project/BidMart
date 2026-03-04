import { apiFetch } from './http'

export type TokenResponse = {
    accessToken: string
    refreshToken: string
    tokenType: 'Bearer' | string
    expiresIn: number
}

export type RegisterResponse = {
    ok: boolean
    verificationToken: string | null
}

export type MfaChallengeResponse = {
    mfaRequired: true
    challengeId: string
    method: 'EMAIL' | 'TOTP' | string
    expiresIn: number
    devCode?: string | null
}

export type LoginResponse = TokenResponse | MfaChallengeResponse

export type MeResponse = {
    username: string
    role: string
}

export type SessionRow = {
    token: string
    createdAt: string
    lastSeenAt: string
    expiresAt: string | null
    revokedAt: string | null
    userAgent: string | null
    ip: string | null
}

export type UserProfile = {
    displayName: string | null
    photoUrl: string | null
    shippingAddress: string | null
}

export type AdminUserRow = {
    id: number
    username: string
    role: string
    disabled: boolean
    createdAt: string
}

export type PermissionRow = { key: string; description: string | null }

export async function register(username: string, password: string, requestedRole?: 'BUYER' | 'SELLER'): Promise<RegisterResponse> {
    return apiFetch('/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password, requestedRole }),
    })
}

export async function verifyEmail(token: string): Promise<{ ok: boolean }> {
    return apiFetch('/api/auth/verify-email', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ token }),
    })
}

export async function login(username: string, password: string): Promise<LoginResponse> {
    return apiFetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
    })
}

export async function verifyMfa(challengeId: string, code: string): Promise<TokenResponse> {
    return apiFetch('/api/auth/2fa/verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ challengeId, code }),
    })
}

export async function enable2faEmail(accessToken: string): Promise<{ ok: boolean }> {
    return apiFetch('/api/auth/2fa/enable-email', { method: 'POST', accessToken })
}

export async function disable2fa(accessToken: string): Promise<{ ok: boolean }> {
    return apiFetch('/api/auth/2fa/disable', { method: 'POST', accessToken })
}

export async function refresh(refreshToken: string): Promise<TokenResponse> {
    return apiFetch('/api/auth/refresh', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
    })
}

export async function me(accessToken: string): Promise<MeResponse> {
    return apiFetch('/api/auth/me', { accessToken })
}

export async function logout(accessToken: string): Promise<{ ok: boolean }> {
    return apiFetch('/api/auth/logout', { method: 'POST', accessToken })
}

export async function sessions(accessToken: string): Promise<SessionRow[]> {
    return apiFetch('/api/auth/sessions', { accessToken })
}

export async function revokeSession(accessToken: string, sessionToken: string): Promise<{ ok: boolean }> {
    return apiFetch(`/api/auth/sessions/${sessionToken}/revoke`, { method: 'POST', accessToken })
}

export async function becomeSeller(accessToken: string): Promise<{ ok: boolean }> {
    return apiFetch('/api/users/me/become-seller', { method: 'POST', accessToken })
}

export async function getMyProfile(accessToken: string): Promise<UserProfile> {
    return apiFetch('/api/users/me/profile', { accessToken })
}

export async function updateMyProfile(accessToken: string, profile: UserProfile): Promise<{ ok: boolean }> {
    return apiFetch('/api/users/me/profile', {
        method: 'PUT',
        accessToken,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(profile),
    })
}

/* ===== Admin: users ===== */
export async function adminListUsers(accessToken: string): Promise<AdminUserRow[]> {
    return apiFetch('/api/admin/users', { accessToken })
}

export async function adminSetUserRole(accessToken: string, id: number, role: string): Promise<{ ok: boolean }> {
    return apiFetch(`/api/admin/users/${id}/role`, {
        method: 'POST',
        accessToken,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ role }),
    })
}

export async function adminDisableUser(accessToken: string, id: number, disabled: boolean): Promise<{ ok: boolean }> {
    return apiFetch(`/api/admin/users/${id}/disable`, {
        method: 'POST',
        accessToken,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ disabled }),
    })
}

/* ===== Admin: RBAC ===== */
export async function adminListRoles(accessToken: string): Promise<string[]> {
    return apiFetch('/api/admin/rbac/roles', { accessToken })
}

export async function adminCreateRole(accessToken: string, name: string): Promise<{ ok: boolean }> {
    return apiFetch('/api/admin/rbac/roles', {
        method: 'POST',
        accessToken,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name }),
    })
}

export async function adminListPermissions(accessToken: string): Promise<PermissionRow[]> {
    return apiFetch('/api/admin/rbac/permissions', { accessToken })
}

export async function adminCreatePermission(accessToken: string, key: string, description: string): Promise<{ ok: boolean }> {
    return apiFetch('/api/admin/rbac/permissions', {
        method: 'POST',
        accessToken,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ key, description }),
    })
}

export async function adminListRolePerms(accessToken: string, role: string): Promise<string[]> {
    return apiFetch(`/api/admin/rbac/roles/${role}/permissions`, { accessToken })
}

export async function adminSetRolePerms(accessToken: string, role: string, permissions: string[]): Promise<{ ok: boolean }> {
    return apiFetch(`/api/admin/rbac/roles/${role}/permissions`, {
        method: 'PUT',
        accessToken,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ permissions }),
    })
}