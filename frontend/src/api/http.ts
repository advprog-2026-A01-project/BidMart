export type ApiError = { error: string }

/*
Tanggung jawab: wrapper fetch yang:
- inject bearer token (opsional)
- parsing JSON/text
- throw error yang menyimpan {status, payload}
 */
export type ApiFetchOptions = Omit<RequestInit, 'headers'> & {
    headers?: Record<string, string>
    accessToken?: string
}

export async function apiFetch<T>(path: string, opts: ApiFetchOptions = {}): Promise<T> {
    const headers: Record<string, string> = {
        ...(opts.headers ?? {}),
    }

    if (opts.accessToken) {
        headers['Authorization'] = `Bearer ${opts.accessToken}`
    }

    const res = await fetch(path, { ...opts, headers })

    const contentType = res.headers.get('content-type') ?? ''
    const isJson = contentType.includes('application/json')

    if (!res.ok) {
        const payload = isJson ? ((await res.json()) as unknown) : await res.text()
        throw Object.assign(new Error('API_ERROR'), { status: res.status, payload })
    }

    if (isJson) return (await res.json()) as T
    return (await res.text()) as unknown as T
}