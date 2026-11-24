import type { Perfil, PerfilRequest, TrocaDeSenhaRequest } from '@/features/profile/types.ts'
import { api } from '@/shared/lib/api.ts'

export function buscarPerfil() {
  return api<Perfil>('/api/usuarios/me')
}

export function salvarPerfil(body: PerfilRequest) {
  return api<Perfil>('/api/usuarios/me', { method: 'PUT', body: JSON.stringify(body) })
}

export function trocarSenha(body: TrocaDeSenhaRequest) {
  return api<void>('/api/usuarios/me/senha', { method: 'PUT', body: JSON.stringify(body) })
}
