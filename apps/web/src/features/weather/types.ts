export type CategoriaAlerta =
  | 'CHUVA'
  | 'VENTO'
  | 'TEMPESTADE'
  | 'GRANIZO'
  | 'GEADA'
  | 'UMIDADE_BAIXA'
  | 'TORNADO'

export type FrequenciaAlerta = 'POR_EVENTO' | 'DIARIO' | 'SEMANAL'

export type FazendaDoAlerta = {
  id: string
  nome: string
}

export type AlertaClimatico = {
  id: string
  categoria: CategoriaAlerta
  fonte: string
  frase: string
  texto: string | null
  inicio: string
  fim: string
  lido: boolean
  fazendas: FazendaDoAlerta[]
}

export type AlertasClimaticos = {
  naoLidos: number
  eventos: AlertaClimatico[]
}

export type PreferenciaAlerta = {
  fazendaId: string
  raioKm: number
  categorias: CategoriaAlerta[]
  frequencia: FrequenciaAlerta
}

export const categoriasAlerta: { id: CategoriaAlerta; label: string }[] = [
  { id: 'CHUVA', label: 'Chuva' },
  { id: 'VENTO', label: 'Vento' },
  { id: 'TEMPESTADE', label: 'Tempestade' },
  { id: 'GRANIZO', label: 'Granizo' },
  { id: 'GEADA', label: 'Geada' },
  { id: 'UMIDADE_BAIXA', label: 'Umidade baixa' },
  { id: 'TORNADO', label: 'Tornado' },
]

export const frequenciasAlerta: { id: FrequenciaAlerta; label: string }[] = [
  { id: 'POR_EVENTO', label: 'A cada evento' },
  { id: 'DIARIO', label: 'Uma vez por dia' },
  { id: 'SEMANAL', label: 'Uma vez por semana' },
]

export function nomeCategoria(categoria: string) {
  return categoriasAlerta.find((item) => item.id === categoria)?.label ?? categoria
}

export function nomeFonte(fonte: string) {
  if (fonte === 'INMET') return 'INMET'
  if (fonte === 'OPEN_METEO') return 'Open-Meteo'
  return fonte
}
