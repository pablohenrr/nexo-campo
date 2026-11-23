export type SituacaoTalhao = 'ATIVO' | 'INATIVO'

export type Talhao = {
  id: string
  fazendaId: string
  nome: string
  cultura: string | null
  areaHectares: number | null
  situacao: SituacaoTalhao
  limiteWkt: string
}

export type TalhaoRequest = {
  nome: string
  cultura: string | null
  areaHectares: number | null
  situacao: SituacaoTalhao
  limiteWkt: string
}
