const hectaresFormat = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 })

export function hectares(value: number | null | undefined): string {
  if (value == null) return 'Sem área'
  return `${hectaresFormat.format(value)} ha`
}

export function dataCurta(iso: string): string {
  const [year, month, day] = iso.slice(0, 10).split('-')
  if (!year || !month || !day) return iso
  return `${day}/${month}/${year}`
}

export function inicialDoNome(nome: string): string {
  const letter = nome.trim().charAt(0)
  return letter ? letter.toLocaleUpperCase('pt-BR') : '?'
}

const atividades: Record<string, string> = {
  PLANTIO: 'Plantio',
  ADUBACAO: 'Adubação',
  PULVERIZACAO: 'Pulverização',
  IRRIGACAO: 'Irrigação',
  CAPINA: 'Capina',
  COLHEITA: 'Colheita',
  MONITORAMENTO: 'Monitoramento',
  MANUTENCAO: 'Manutenção',
  OUTRO: 'Outro',
}

export function rotuloAtividade(tipo: string): string {
  return atividades[tipo.toUpperCase()] ?? tipo
}

const categorias: Record<string, string> = {
  PRAGA: 'Praga',
  DOENCA: 'Doença',
  PLANTA_DANINHA: 'Planta daninha',
  DEFICIENCIA_NUTRICIONAL: 'Deficiência nutricional',
  CLIMA: 'Clima',
  OUTRO: 'Outro',
}

export function rotuloCategoria(categoria: string): string {
  return categorias[categoria] ?? categoria
}
