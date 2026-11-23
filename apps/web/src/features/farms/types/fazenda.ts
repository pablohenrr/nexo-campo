export type Fazenda = {
  id: string
  nome: string
  descricao: string | null
  municipio: string | null
  estado: string | null
  areaTotalHectares: number | null
  latitude: number | null
  longitude: number | null
}

export type FazendaRequest = {
  nome: string
  descricao: string | null
  municipio: string | null
  estado: string | null
  areaTotalHectares: number | null
  latitude: number | null
  longitude: number | null
}
