import type { CategoriaOcorrencia, Severidade } from '@/features/activities/types/ocorrencia.ts'

export type SituacaoMensagem = 'REGISTRADA' | 'SEM_TALHAO' | 'IGNORADA' | 'FALHOU'
export type TipoMensagem = 'ATIVIDADE' | 'OCORRENCIA' | 'OUTRO'

export type InterpretacaoAtividade = {
  tipo: string
  data: string
  descricao: string
  insumos: string | null
  custo: number | null
}

export type InterpretacaoOcorrencia = {
  categoria: CategoriaOcorrencia
  severidade: Severidade
  descricao: string
}

export type Interpretacao = {
  tipo: TipoMensagem
  nomeTalhao: string | null
  resumo: string
  atividade: InterpretacaoAtividade | null
  ocorrencia: InterpretacaoOcorrencia | null
  camposFaltantes: string[]
  confianca: number
}

export type MensagemCampo = {
  id: string
  canal: string
  situacao: SituacaoMensagem
  tipo: TipoMensagem | null
  talhaoId: string | null
  nomeTalhao: string | null
  atividadeId: string | null
  ocorrenciaId: string | null
  evidenciaId: string | null
  texto: string | null
  transcricao: string | null
  resposta: string | null
  interpretacao: Interpretacao | null
  criadaEm: string
}

export type EnvioMensagem = {
  texto: string | null
  arquivo: File | null
}
