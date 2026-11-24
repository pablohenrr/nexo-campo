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

/** `+5511987654321` vira `(11) 98765-4321`; números de fora do Brasil ficam como vieram. */
export function telefoneLegivel(telefone: string | null | undefined): string {
  if (!telefone) return ''
  const digitos = telefone.replace(/\D/g, '')
  if (!digitos.startsWith('55') || (digitos.length !== 12 && digitos.length !== 13)) return telefone
  const ddd = digitos.slice(2, 4)
  const numero = digitos.slice(4)
  const corte = numero.length - 4
  return `(${ddd}) ${numero.slice(0, corte)}-${numero.slice(corte)}`
}

/** `2026-11-20T10:00:00Z` vira `novembro de 2026`. */
export function mesEAno(iso: string): string {
  const data = new Date(iso)
  if (Number.isNaN(data.getTime())) return ''
  return new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' }).format(data)
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
