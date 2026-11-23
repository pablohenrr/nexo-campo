import { useEffect, useRef, useState } from 'react'
import type { FeatureCollection, LineString, Position } from 'geojson'
import { Map, Marker, type GeoJSONSource, type IControl } from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'
import { FieldMaquette } from '@/features/maps/components/field-maquette.tsx'
import {
  createMapTilerLogo,
  createMapTilerTileJsonUrl,
  loadMapTilerSatellite,
} from '@/features/maps/mapTilerSatellite.ts'
import {
  boundsOf,
  focusPlot,
  mountFieldMap,
  setFieldPlots,
  setSatelliteOverlay,
  setTerrainMode,
  type PlotFeature,
} from '@/features/maps/mountPlotMap.ts'
import { mapaClaro } from '@/features/maps/style.ts'
import { centroid } from '@/features/maps/wkt.ts'
import { corEstado, rotuloEstado, type EstadoTalhao } from '@/features/plots/status.ts'
import '@/features/maps/plotMap.css'

export type ModoMapa = 'maquete' | 'satelite' | 'mapa'

const chaveMapa = import.meta.env.VITE_MAPTILER_KEY

export function FieldMap({
  features,
  selectedId,
  modo,
  onModo,
  drawing,
  draft,
  onSelect,
  onAddVertex,
  center,
  fitKey,
  visivel,
  padLeft = 48,
}: {
  features: PlotFeature[]
  selectedId: string | null
  modo: ModoMapa
  onModo: (modo: ModoMapa) => void
  drawing: boolean
  draft: Position[]
  onSelect: (id: string) => void
  onAddVertex: (longitude: number, latitude: number) => void
  center: { latitude: number; longitude: number } | null
  fitKey: string
  visivel: boolean
  padLeft?: number
}) {
  const container = useRef<HTMLDivElement>(null)
  const mapRef = useRef<Map | null>(null)
  const logoRef = useRef<IControl | null>(null)
  const onSelectRef = useRef(onSelect)
  const onVertexRef = useRef(onAddVertex)
  const drawingRef = useRef(drawing)
  const featuresRef = useRef(features)
  const centerRef = useRef(center)
  const [ready, setReady] = useState(false)
  const [medido, setMedido] = useState(false)
  const [aviso, setAviso] = useState('')

  const dataKey = features.map((feature) => `${feature.properties.talhaoId ?? ''}:${feature.properties.estado ?? ''}`).join('|')

  useEffect(() => {
    onSelectRef.current = onSelect
    onVertexRef.current = onAddVertex
    drawingRef.current = drawing
    featuresRef.current = features
    centerRef.current = center
  }, [center, drawing, features, onAddVertex, onSelect])

  useEffect(() => {
    const element = container.current
    if (!element) return
    const map = mountFieldMap(Map, element, mapaClaro, (id) => {
      if (drawingRef.current) return
      onSelectRef.current(id)
    }, () => setReady(true))
    map.on('click', (event) => {
      if (!drawingRef.current) return
      onVertexRef.current(event.lngLat.lng, event.lngLat.lat)
    })
    mapRef.current = map
    const encaixar = () => {
      element.style.width = '100%'
      element.style.height = '100%'
      map.resize()
    }
    const observer = new ResizeObserver(() => {
      encaixar()
      if (element.clientHeight > 0) setMedido(true)
    })
    observer.observe(element.parentElement ?? element)
    encaixar()
    return () => {
      observer.disconnect()
      map.remove()
      mapRef.current = null
      logoRef.current = null
      setReady(false)
    }
  }, [])

  useEffect(() => {
    if (visivel && modo !== 'maquete') mapRef.current?.resize()
  }, [modo, visivel])

  useEffect(() => {
    const map = mapRef.current
    if (!map || !ready) return
    setFieldPlots(map, featuresRef.current, selectedId)
  }, [dataKey, ready, selectedId])

  useEffect(() => {
    const map = mapRef.current
    if (!map || !ready || !medido || drawing || !visivel) return
    let ativo = true
    const enquadrar = () => {
      if (!ativo) return
      const duration = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 0 : 500
      const plots = featuresRef.current
      const seat = centerRef.current
      if (plots.length > 0) {
        const bounds = boundsOf(plots)
        if (bounds) map.fitBounds(bounds, { padding: { top: 72, right: 72, bottom: 72, left: padLeft }, maxZoom: 16, duration })
        return
      }
      if (seat) map.easeTo({ center: [seat.longitude, seat.latitude], zoom: 13, duration })
    }
    enquadrar()
    map.once('idle', enquadrar)
    return () => {
      ativo = false
    }
  }, [drawing, fitKey, medido, padLeft, ready, visivel])

  useEffect(() => {
    const map = mapRef.current
    if (!map || !ready || !medido || !visivel || !selectedId || drawingRef.current) return
    const feature = featuresRef.current.find((item) => item.properties.talhaoId === selectedId)
    if (!feature) return
    const duration = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 0 : 500
    focusPlot(map, feature, { duration, padding: { top: 80, right: 80, bottom: 80, left: padLeft }, maxZoom: 17 })
  }, [fitKey, medido, padLeft, ready, selectedId, visivel])

  useEffect(() => {
    const map = mapRef.current
    if (!map || !ready) return
    if (!map.getSource('nexo-draft')) {
      map.addSource('nexo-draft', { type: 'geojson', data: draftCollection(draft) })
      map.addLayer({
        id: 'nexo-draft-line',
        type: 'line',
        source: 'nexo-draft',
        paint: { 'line-color': '#0F291E', 'line-width': 2, 'line-dasharray': [1.5, 1] },
      })
      return
    }
    ;(map.getSource('nexo-draft') as GeoJSONSource).setData(draftCollection(draft))
  }, [draft, ready])

  useEffect(() => {
    const map = mapRef.current
    if (!map || !ready) return
    const markers = featuresRef.current.flatMap((feature) => {
      const id = feature.properties.talhaoId
      const longitude = centroid(feature.geometry)[0]
      const latitude = centroid(feature.geometry)[1]
      if (!id || longitude == null || latitude == null) return []
      const estado = (feature.properties.estado ?? 'MONITORAMENTO') as EstadoTalhao
      const button = document.createElement('button')
      button.type = 'button'
      button.className = 'field-pin'
      button.setAttribute('aria-label', `${feature.properties.nome}, ${rotuloEstado[estado] ?? estado}`)
      button.setAttribute('aria-pressed', String(id === selectedId))
      const dot = document.createElement('span')
      dot.style.background = corEstado[estado] ?? '#0F291E'
      button.append(dot)
      button.addEventListener('click', (event) => {
        event.stopPropagation()
        if (drawingRef.current) return
        onSelectRef.current(id)
      })
      return [new Marker({ element: button, anchor: 'center' }).setLngLat([longitude, latitude]).addTo(map)]
    })
    return () => markers.forEach((marker) => marker.remove())
  }, [dataKey, ready, selectedId])

  useEffect(() => {
    const map = mapRef.current
    if (!map || !ready) return
    let active = true
    const duration = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 0 : 500

    async function aplicar() {
      if (modo === 'mapa' || modo === 'maquete') {
        setSatelliteOverlay(map!, undefined, false)
        setTerrainMode(map!, undefined, false)
        map!.easeTo({ pitch: 0, duration: modo === 'maquete' ? 0 : duration })
        if (active) setAviso('')
        return
      }

      const sateliteUrl = createMapTilerTileJsonUrl(chaveMapa)
      if (modo === 'satelite') {
        setTerrainMode(map!, undefined, false)
        map!.easeTo({ pitch: 0, duration })
        if (!sateliteUrl) {
          if (active) setAviso('Imagem de satélite indisponível. Exibindo mapa-base.')
          return
        }
        try {
          const source = await loadMapTilerSatellite(sateliteUrl)
          if (!active) return
          setSatelliteOverlay(map!, source, true)
          mostrarLogo(map!)
          setAviso('')
        } catch {
          if (active) setAviso('Imagem de satélite indisponível. Exibindo mapa-base.')
        }
        return
      }
    }

    function mostrarLogo(alvo: Map) {
      if (logoRef.current) return
      const logo = createMapTilerLogo()
      logoRef.current = logo
      alvo.addControl(logo, 'bottom-right')
    }

    void aplicar()
    return () => {
      active = false
    }
  }, [modo, ready])

  return (
    <section aria-label={modo === 'maquete' && !drawing ? 'Maquete da fazenda' : 'Mapa da fazenda'} className="relative h-full min-h-80">
      <div className="absolute top-4 right-4 z-20 flex gap-1 rounded-full bg-cerca p-1">
        {([
          ['maquete', 'Maquete'],
          ['satelite', 'Satélite'],
          ['mapa', 'Mapa'],
        ] as const).map(([valor, rotulo]) => (
          <button
            key={valor}
            type="button"
            aria-pressed={modo === valor}
            className={`min-h-12 rounded-full px-4 text-base font-medium ${modo === valor ? 'bg-chuva text-cerca' : 'text-nevoa'}`}
            onClick={() => onModo(valor)}
          >
            {rotulo}
          </button>
        ))}
      </div>
      <div ref={container} className={`h-full w-full ${modo === 'maquete' && !drawing ? 'pointer-events-none invisible' : ''}`} />
      {modo === 'maquete' && !drawing && (
        <FieldMaquette
          features={features}
          selectedId={selectedId}
          onSelect={onSelect}
          center={center}
          visivel={visivel}
        />
      )}
      {aviso && (
        <p role="status" className="absolute right-3 bottom-3 z-10 max-w-xs rounded-controle border border-linha bg-cerca px-3 py-2 text-base">
          {aviso}
        </p>
      )}
    </section>
  )
}

function draftCollection(draft: Position[]): FeatureCollection<LineString> {
  return {
    type: 'FeatureCollection',
    features: draft.length < 2
      ? []
      : [{
          type: 'Feature',
          properties: {},
          geometry: { type: 'LineString', coordinates: draft },
        }],
  }
}
