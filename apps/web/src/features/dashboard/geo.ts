import type { Polygon } from 'geojson'
import type { Talhao } from '@/features/farms/types/index.ts'
import type { PlotFeature } from '@/features/maps/mountPlotMap.ts'
import { parsePolygonWkt } from '@/features/maps/wkt.ts'
import type { EstadoTalhao } from '@/features/plots/status.ts'

export function montarFeatures(
  nomeFazenda: string,
  talhoes: Talhao[],
  estados: EstadoTalhao[],
): PlotFeature[] {
  return talhoes.flatMap((talhao, index) => {
    let geometry: Polygon
    try {
      geometry = parsePolygonWkt(talhao.limiteWkt)
    } catch {
      return []
    }
    return [{
      type: 'Feature' as const,
      id: talhao.id,
      geometry,
      properties: {
        nomeFazenda,
        nome: talhao.nome,
        tipoCultura: talhao.cultura ?? 'Sem cultura',
        areaHectares: talhao.areaHectares ?? 0,
        talhaoId: talhao.id,
        estado: estados[index] ?? 'MONITORAMENTO',
      },
    }]
  })
}

export function areaDaFazenda(areaTotal: number | null, talhoes: Talhao[]): number | null {
  if (areaTotal != null) return areaTotal
  const areas = talhoes.flatMap((item) => (item.areaHectares == null ? [] : [item.areaHectares]))
  if (areas.length === 0) return null
  return areas.reduce((total, item) => total + item, 0)
}
