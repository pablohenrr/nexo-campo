import type { Polygon, Position } from 'geojson'

export function parsePolygonWkt(wkt: string): Polygon {
  const body = wkt.trim().replace(/^SRID=\d+;/i, '')
  const match = body.match(/^POLYGON\s*\(\((.*)\)\)$/i)
  if (!match?.[1]) throw new Error('O limite do talhão não está em WKT de polígono.')

  const rings = match[1].split(/\)\s*,\s*\(/).map(parseRing)
  return { type: 'Polygon', coordinates: rings }
}

export function buildPolygonWkt(points: Position[]): string {
  const first = points[0]
  if (!first || points.length < 3) throw new Error('Marque pelo menos três cantos.')
  const ring = [...points, first]
  const body = ring.map(([longitude, latitude]) => `${longitude} ${latitude}`).join(', ')
  return `POLYGON((${body}))`
}

export function centroid(polygon: Polygon): Position {
  const ring = polygon.coordinates[0]?.slice(0, -1) ?? []
  if (ring.length === 0) return [0, 0]
  let longitude = 0
  let latitude = 0
  for (const position of ring) {
    longitude += position[0] ?? 0
    latitude += position[1] ?? 0
  }
  return [longitude / ring.length, latitude / ring.length]
}

function parseRing(text: string): Position[] {
  const positions = text.split(',').map((pair) => {
    const [longitude, latitude] = pair.trim().split(/\s+/).map(Number)
    if (longitude == null || latitude == null || !Number.isFinite(longitude) || !Number.isFinite(latitude)) {
      throw new Error('O limite do talhão tem uma coordenada inválida.')
    }
    return [longitude, latitude] as Position
  })
  if (positions.length < 4) throw new Error('O limite do talhão não fecha um polígono.')
  return positions
}
