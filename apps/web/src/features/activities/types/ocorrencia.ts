export const CATEGORIAS_OCORRENCIA = [
  'PRAGA',
  'DOENCA',
  'PLANTA_DANINHA',
  'DEFICIENCIA_NUTRICIONAL',
  'CLIMA',
  'OUTRO',
] as const

export const SEVERIDADES = ['BAIXA', 'MEDIA', 'ALTA'] as const

export type CategoriaOcorrencia = (typeof CATEGORIAS_OCORRENCIA)[number]
export type Severidade = (typeof SEVERIDADES)[number]
export type SituacaoOcorrencia = 'ABERTA' | 'RESOLVIDA'

export type Ocorrencia = {
  id: string
  talhaoId: string
  categoria: CategoriaOcorrencia
  severidade: Severidade
  situacao: SituacaoOcorrencia
  descricao: string
  dataObservacao: string
  evidenciaId: string | null
  responsavelId: string
  criadaEm: string
}

export type OcorrenciaRequest = {
  categoria: CategoriaOcorrencia
  severidade: Severidade
  descricao: string
  dataObservacao: string
  evidenciaId: string | null
}
