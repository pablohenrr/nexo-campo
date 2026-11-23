import type { Atividade, AtividadeRequest } from '@/features/activities/types/index.ts'
import { api } from '@/shared/lib/api.ts'

export function listarAtividades(talhaoId: string) {
  return api<Atividade[]>(`/api/talhoes/${talhaoId}/atividades`)
}

export function criarAtividade(talhaoId: string, body: AtividadeRequest) {
  return api<Atividade>(`/api/talhoes/${talhaoId}/atividades`, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}
