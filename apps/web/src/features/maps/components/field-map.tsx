import { useEffect, useRef, useState } from 'react'
import type { Feature, FeatureCollection, LineString, Polygon, Position } from 'geojson'
import { LocateFixed, Maximize2 } from 'lucide-react'
import { LngLatBounds, Map, Marker, type GeoJSONSource, type IControl } from 'maplibre-gl'
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
  setFarmOutline,
  setFieldPlots,
  setSatelliteOverlay,
  setTerrainMode,
  type PlotFeature,
} from '@/features/maps/mountPlotMap.ts'
import { mapaClaro } from '@/features/maps/style.ts'
import { centroid } from '@/features/maps/wkt.ts'
import { svgDaCultura } from '@/features/plots/crop-icon.ts'
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
  farmOutline = null,
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
  /** Contorno da fazenda: só serve para enquadrar, nunca é desenhado no mapa principal. */
  farmOutline?: Array<{ latitude: number; longitude: number }> | null
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
  const [maqueteKey, setMaqueteKey] = useState(0)

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
      // Prévia do talhão: área clara + linha clara sobre borda escura, legível no satélite.
      map.addLayer({
        id: 'nexo-draft-fill',
        type: 'fill',
        source: 'nexo-draft',
        filter: ['==', '$type', 'Polygon'],
        paint: { 'fill-color': '#F7C948', 'fill-opacity': 0.25 },
      })
      map.addLayer({
        id: 'nexo-draft-casing',
        type: 'line',
        source: 'nexo-draft',
        filter: ['==', '$type', 'LineString'],
        layout: { 'line-cap': 'round', 'line-join': 'round' },
        paint: { 'line-color': '#0F291E', 'line-width': 6, 'line-opacity': 0.7 },
      })
      map.addLayer({
        id: 'nexo-draft-line',
        type: 'line',
        source: 'nexo-draft',
        filter: ['==', '$type', 'LineString'],
        layout: { 'line-cap': 'round', 'line-join': 'round' },
        paint: { 'line-color': '#F7C948', 'line-width': 3 },
      })
      return
    }
    ;(map.getSource('nexo-draft') as GeoJSONSource).setData(draftCollection(draft))
  }, [draft, ready])

  useEffect(() => {
    const map = mapRef.current
    // Durante o desenho os ícones saem do caminho: um clique sobre eles não marcaria o canto.
    if (!map || !ready || drawing) return
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
      // Ícone da cultura no centro do talhão, com anel na cor da situação.
      button.style.setProperty('--estado', corEstado[estado] ?? '#0F291E')
      button.innerHTML = svgDaCultura(feature.properties.tipoCultura)
      button.title = feature.properties.nome
      button.addEventListener('click', (event) => {
        event.stopPropagation()
        if (drawingRef.current) return
        onSelectRef.current(id)
      })
      return [new Marker({ element: button, anchor: 'center' }).setLngLat([longitude, latitude]).addTo(map)]
    })
    return () => markers.forEach((marker) => marker.remove())
  }, [dataKey, drawing, ready, selectedId])

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

  useEffect(() => {
    const map = mapRef.current
    if (!map || !ready) return
    setFarmOutline(map, farmOutline)
  }, [farmOutline, ready])

  useEffect(() => {
    const map = mapRef.current
    if (!map || !ready || !drawing) return
    // Pino numerado em cada canto marcado, para ver exatamente onde o clique caiu.
    const pinos = draft.map((posicao, indice) => {
      const elemento = document.createElement('div')
      elemento.className = 'draft-pin'
      elemento.textContent = String(indice + 1)
      elemento.setAttribute('aria-hidden', 'true')
      return new Marker({ element: elemento, anchor: 'center' }).setLngLat([posicao[0]!, posicao[1]!]).addTo(map)
    })
    return () => pinos.forEach((pino) => pino.remove())
  }, [draft, drawing, ready])

  const emMaquete = modo === 'maquete' && !drawing
  const selecionado = selectedId ? features.find((item) => item.properties.talhaoId === selectedId) ?? null : null

  function verFazenda() {
    if (emMaquete) {
      // Remonta a maquete, que volta para a câmera inicial.
      setMaqueteKey((valor) => valor + 1)
      return
    }
    const map = mapRef.current
    if (!map) return
    const duration = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 0 : 500
    const padding = { top: 88, right: 72, bottom: 72, left: padLeft }
    const bounds = new LngLatBounds()
    const plots = boundsOf(features)
    if (plots) bounds.extend(plots)
    farmOutline?.forEach((ponto) => bounds.extend([ponto.longitude, ponto.latitude]))
    if (!bounds.isEmpty()) {
      map.fitBounds(bounds, { padding, maxZoom: 16, duration })
    } else if (center) {
      map.easeTo({ center: [center.longitude, center.latitude], zoom: 14, duration })
    }
  }

  function verTalhao() {
    const map = mapRef.current
    if (!map || !selecionado) return
    const duration = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 0 : 500
    focusPlot(map, selecionado, { duration, padding: { top: 88, right: 80, bottom: 80, left: padLeft }, maxZoom: 17 })
  }

  const podeVerFazenda = emMaquete || features.length > 0 || Boolean(farmOutline?.length) || center != null

  return (
    <section aria-label={emMaquete ? 'Maquete da fazenda' : 'Mapa da fazenda'} className="relative h-full min-h-80">
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
      {!drawing && (
        <div className="absolute top-20 right-4 z-20 flex flex-col gap-1 rounded-controle border border-linha bg-cerca p-1 shadow-sm">
          <button
            type="button"
            className="flex size-11 items-center justify-center rounded-[8px] text-tinta hover:bg-claridade disabled:opacity-40"
            title="Ver a fazenda inteira"
            aria-label="Ver a fazenda inteira"
            disabled={!podeVerFazenda}
            onClick={verFazenda}
          >
            <Maximize2 aria-hidden="true" size={20} />
          </button>
          {!emMaquete && (
            <button
              type="button"
              className="flex size-11 items-center justify-center rounded-[8px] text-tinta hover:bg-claridade disabled:opacity-40"
              title={selecionado ? `Centralizar em ${selecionado.properties.nome}` : 'Selecione um talhão para centralizar'}
              aria-label={selecionado ? `Centralizar em ${selecionado.properties.nome}` : 'Centralizar no talhão selecionado'}
              disabled={!selecionado}
              onClick={verTalhao}
            >
              <LocateFixed aria-hidden="true" size={20} />
            </button>
          )}
        </div>
      )}
      <div ref={container} className={`h-full w-full ${emMaquete ? 'pointer-events-none invisible' : ''}`} />
      {emMaquete && (
        <FieldMaquette
          key={maqueteKey}
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

function draftCollection(draft: Position[]): FeatureCollection<LineString | Polygon> {
  const features: Feature<LineString | Polygon>[] = []
  if (draft.length >= 3) {
    features.push({ type: 'Feature', properties: {}, geometry: { type: 'Polygon', coordinates: [[...draft, draft[0]!]] } })
    // Linha fechada para mostrar como o talhão vai ficar.
    features.push({ type: 'Feature', properties: {}, geometry: { type: 'LineString', coordinates: [...draft, draft[0]!] } })
  } else if (draft.length === 2) {
    features.push({ type: 'Feature', properties: {}, geometry: { type: 'LineString', coordinates: draft } })
  }
  return { type: 'FeatureCollection', features }
}
