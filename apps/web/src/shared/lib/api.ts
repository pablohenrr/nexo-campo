import { definirSessao, sessaoAtual } from '@/features/auth/session.ts'
import type { ApiErrorBody } from '@/shared/types/auth.ts'

export class ApiError extends Error {
  status: number
  detalhes: string[]

  constructor(status: number, mensagem: string, detalhes: string[] = []) {
    super(mensagem)
    this.status = status
    this.detalhes = detalhes
  }
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  if (init.body && !(init.body instanceof FormData) && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const token = sessaoAtual()?.token
  if (token) headers.set('Authorization', `Bearer ${token}`)

  let response: Response
  try {
    response = await fetch(path, { ...init, headers })
  } catch {
    throw new ApiError(0, 'A API não respondeu. Confira se ela está no ar.')
  }

  if (response.status === 401) {
    definirSessao(null)
    throw new ApiError(401, 'A sessão expirou. Entre de novo.')
  }

  if (!response.ok) {
    const body = await response.json().catch(() => null) as ApiErrorBody | null
    throw new ApiError(response.status, body?.mensagem || 'A operação falhou.', body?.detalhes ?? [])
  }

  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}
