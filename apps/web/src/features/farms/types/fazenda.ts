export type Coordenada = {
  latitude: number
  longitude: number
}

export type Fazenda = {
  id: string
  nome: string
  descricao: string | null
  municipio: string | null
  estado: string | null
  areaTotalHectares: number | null
  /** Centro calculado pelo backend a partir do contorno. */
  latitude: number | null
  longitude: number | null
  /** Pinos na ordem do desenho. null em fazendas antigas, cadastradas sem contorno. */
  contorno: Coordenada[] | null
}

export type FazendaRequest = {
  nome: string
  descricao: string | null
  municipio: string | null
  estado: string | null
  /** Pinos na ordem em que formam o contorno, sem repetir o primeiro no fim. */
  contorno: Coordenada[]
}
