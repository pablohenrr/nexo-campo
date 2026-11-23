import type { Talhao, TalhaoRequest } from '@/features/plots/types.ts'
import { api } from '@/shared/lib/api.ts'

export function listarTalhoes(fazendaId: string) {
  return api<Talhao[]>(`/api/fazendas/${fazendaId}/talhoes`)
}

export function criarTalhao(fazendaId: string, body: TalhaoRequest) {
  return api<Talhao>(`/api/fazendas/${fazendaId}/talhoes`, { method: 'POST', body: JSON.stringify(body) })
}
