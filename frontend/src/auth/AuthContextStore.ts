import { createContext } from 'react'
import type * as AuthApi from '../api/auth'
import type { StoredTokens } from './tokenStorage'

export type PendingMfa = {
    challengeId: string
    method: string
    expiresIn: number
    devCode?: string | null
}

export type AuthState = {
    tokens: StoredTokens | null
    user: AuthApi.MeResponse | null
    loading: boolean
    error: string | null
    lastVerificationToken: string | null
    pendingMfa: PendingMfa | null
}

export type AuthActions = {
    register: (username: string, password: string, requestedRole?: 'BUYER' | 'SELLER') => Promise<void>
    verifyEmail: (token: string) => Promise<void>
    login: (username: string, password: string) => Promise<void>
    submitMfa: (code: string) => Promise<void>
    cancelMfa: () => void
    enable2faEmail: () => Promise<void>
    disable2fa: () => Promise<void>
    becomeSeller: () => Promise<void>
    logout: () => Promise<void>
    refresh: () => Promise<void>
    reloadMe: () => Promise<void>
}

export type AuthContextValue = AuthState & AuthActions

export const AuthContext = createContext<AuthContextValue | null>(null)