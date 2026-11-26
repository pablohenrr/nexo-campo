/** Busca de endereço para posicionar o mapa. É só um atalho: o resultado nunca vira o centro da fazenda. */
export type ResultadoEndereco = {
  id: string
  nome: string
  longitude: number
  latitude: number
  /** [oeste, sul, leste, norte], quando o provedor informa. */
  limites: [number, number, number, number] | null
}

const chaveMapa = import.meta.env.VITE_MAPTILER_KEY?.trim()

export const buscaDisponivel = Boolean(chaveMapa)

export async function buscarEndereco(texto: string, signal?: AbortSignal): Promise<ResultadoEndereco[]> {
  const consulta = texto.trim()
  if (!consulta || !chaveMapa) return []
  const query = new URLSearchParams({ key: chaveMapa, language: 'pt', country: 'br', limit: '5' })
  const response = await fetch(
    `https://api.maptiler.com/geocoding/${encodeURIComponent(consulta)}.json?${query}`,
    { signal },
  )
  if (!response.ok) throw new Error('Busca de endereço indisponível.')
  const body = (await response.json()) as {
    features?: { id: string; place_name?: string; text?: string; center?: number[]; bbox?: number[] }[]
  }
  return (body.features ?? []).flatMap((feature) => {
    const [longitude, latitude] = feature.center ?? []
    if (typeof longitude !== 'number' || typeof latitude !== 'number') return []
    const bbox = feature.bbox?.length === 4 ? (feature.bbox as [number, number, number, number]) : null
    return [{ id: feature.id, nome: feature.place_name ?? feature.text ?? consulta, longitude, latitude, limites: bbox }]
  })
}
