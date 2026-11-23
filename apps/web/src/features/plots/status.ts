import type { Atividade, Ocorrencia } from '@/features/activities/types/index.ts'
import type { Talhao } from '@/features/plots/types.ts'

export type EstadoTalhao =
  | 'INFESTACAO_ALTA'
  | 'ALERTA_MEDIO'
  | 'ALERTA_BAIXO'
  | 'COLHEITA'
  | 'MONITORAMENTO'
  | 'INATIVO'

export const rotuloEstado: Record<EstadoTalhao, string> = {
  INFESTACAO_ALTA: 'Infestação alta',
  ALERTA_MEDIO: 'Alerta médio',
  ALERTA_BAIXO: 'Alerta baixo',
  COLHEITA: 'Colheita',
  MONITORAMENTO: 'Em monitoramento',
  INATIVO: 'Inativo',
}

export const corEstado: Record<EstadoTalhao, string> = {
  INFESTACAO_ALTA: '#B91C1C',
  ALERTA_MEDIO: '#B45309',
  ALERTA_BAIXO: '#067647',
  COLHEITA: '#B45309',
  MONITORAMENTO: '#0F291E',
  INATIVO: '#8A978F',
}

export function estadoDoTalhao(
  talhao: Talhao,
  ocorrencias: Ocorrencia[],
  atividades: Atividade[],
): EstadoTalhao {
  if (talhao.situacao === 'INATIVO') return 'INATIVO'
  const abertas = ocorrencias.filter((item) => item.situacao === 'ABERTA')
  const infestacao = abertas.some(
    (item) => item.severidade === 'ALTA' && (item.categoria === 'PRAGA' || item.categoria === 'DOENCA'),
  )
  if (infestacao) return 'INFESTACAO_ALTA'
  if (abertas.some((item) => item.severidade === 'ALTA' || item.severidade === 'MEDIA')) return 'ALERTA_MEDIO'
  if (abertas.some((item) => item.severidade === 'BAIXA')) return 'ALERTA_BAIXO'
  if (atividades[0]?.tipo.toUpperCase() === 'COLHEITA') return 'COLHEITA'
  return 'MONITORAMENTO'
}
