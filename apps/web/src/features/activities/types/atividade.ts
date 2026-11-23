export const TIPOS_ATIVIDADE = [
  'PLANTIO',
  'ADUBACAO',
  'PULVERIZACAO',
  'IRRIGACAO',
  'CAPINA',
  'COLHEITA',
  'MONITORAMENTO',
  'MANUTENCAO',
  'OUTRO',
] as const

export type TipoAtividade = (typeof TIPOS_ATIVIDADE)[number]

export type Atividade = {
  id: string
  talhaoId: string
  tipo: string
  descricao: string | null
  dataAtividade: string
  custo: number | null
  insumos: string | null
  responsavelId: string
}

export type AtividadeRequest = {
  tipo: string
  descricao: string | null
  dataAtividade: string
  custo: number | null
  insumos: string | null
}
