import type { Sessao } from '@/shared/types/auth.ts'
import { api } from '@/shared/lib/api.ts'

export function entrar(email: string, senha: string) {
  return api<Sessao>('/api/autenticacao/login', {
    method: 'POST',
    body: JSON.stringify({ email, senha }),
  })
}
