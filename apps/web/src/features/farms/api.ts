import type { Fazenda, FazendaRequest } from '@/features/farms/types/index.ts'
import { api } from '@/shared/lib/api.ts'

export function listarFazendas() {
  return api<Fazenda[]>('/api/fazendas')
}

export function criarFazenda(body: FazendaRequest) {
  return api<Fazenda>('/api/fazendas', { method: 'POST', body: JSON.stringify(body) })
}
