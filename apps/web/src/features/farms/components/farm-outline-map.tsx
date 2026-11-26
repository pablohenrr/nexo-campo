import { useEffect, useRef, useState } from 'react'
import type { Feature, FeatureCollection, LineString, Polygon } from 'geojson'
import { LngLatBounds, Map, Marker, NavigationControl, type GeoJSONSource } from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'
import '@/features/farms/components/farmOutline.css'
import type { Coordenada } from '@/features/farms/types/index.ts'
import { createMapTilerTileJsonUrl, loadMapTilerSatellite } from '@/features/maps/mapTilerSatellite.ts'
import { mapaClaro } from '@/features/maps/style.ts'

const chaveMapa = import.meta.env.VITE_MAPTILER_KEY
const fonteContorno = 'nexo-contorno-fazenda'
const camadaSatelite = 'nexo-contorno-satelite'

/** Visão inicial: Brasil inteiro, para quem não usar a busca. */
const brasil: [[number, number], [number, number]] = [[-74, -34], [-34, 5.5]]

export type Destino = { longitude: number; latitude: number; limites: [number, number, number, number] | null }

/**
 * Mapa do cadastro da fazenda. Cada clique marca um canto; os pinos podem ser arrastados para corrigir.
 * O contorno só existe aqui (cadastro e edição): o mapa principal continua mostrando apenas os talhões.
 */
export function FarmOutlineMap({
  pinos,
  onAdicionar,
  onMover,
  destino,
  desabilitado,
}: {
  pinos: Coordenada[]
  onAdicionar: (pino: Coordenada) => void
  onMover: (indice: number, pino: Coordenada) => void
  destino: Destino | null
  desabilitado: boolean
}) {
  const container = useRef<HTMLDivElement>(null)
  const mapRef = useRef<Map | null>(null)
  const onAdicionarRef = useRef(onAdicionar)
  const onMoverRef = useRef(onMover)
  const desabilitadoRef = useRef(desabilitado)
  const pinosIniciais = useRef(pinos)
  const [pronto, setPronto] = useState(false)
  const [aviso, setAviso] = useState('')

  useEffect(() => {
    onAdicionarRef.current = onAdicionar
    onMoverRef.current = onMover
    desabilitadoRef.current = desabilitado
  }, [desabilitado, onAdicionar, onMover])

  useEffect(() => {
    const element = container.current
    if (!element) return
    const inicio = pinosIniciais.current
    const map = new Map({
      container: element,
      style: mapaClaro,
      bounds: inicio.length ? limitesDe(inicio) : brasil,
      fitBoundsOptions: { padding: 48, maxZoom: 16 },
      attributionControl: { compact: true },
      doubleClickZoom: false,
    })
    map.addControl(new NavigationControl({ showCompass: false }), 'top-right')
    map.getCanvas().style.cursor = 'crosshair'
    map.on('click', (event) => {
      if (desabilitadoRef.current) return
      onAdicionarRef.current({ latitude: event.lngLat.lat, longitude: event.lngLat.lng })
    })
    map.on('load', async () => {
      map.addSource(fonteContorno, { type: 'geojson', data: colecao([]) })
      map.addLayer({
        id: `${fonteContorno}-area`,
        type: 'fill',
        source: fonteContorno,
        filter: ['==', '$type', 'Polygon'],
        paint: { 'fill-color': '#0F291E', 'fill-opacity': 0.15 },
      })
      map.addLayer({
        id: `${fonteContorno}-linha`,
        type: 'line',
        source: fonteContorno,
        paint: { 'line-color': '#0F291E', 'line-width': 2.5 },
      })
      setPronto(true)

      const url = createMapTilerTileJsonUrl(chaveMapa)
      if (!url) return
      try {
        const satelite = await loadMapTilerSatellite(url)
        if (!mapRef.current) return
        map.addSource(camadaSatelite, satelite)
        // Satélite abaixo do contorno, para enxergar cercas e estradas ao marcar os cantos.
        map.addLayer({ id: camadaSatelite, type: 'raster', source: camadaSatelite }, `${fonteContorno}-area`)
      } catch (error) {
        const status = Number(/HTTP (\d+)/.exec(error instanceof Error ? error.message : '')?.[1])
        setAviso(
          status === 401 || status === 403
            ? `Satélite indisponível: o MapTiler recusou a chave (HTTP ${status}) para ${window.location.origin}.`
            : 'Imagem de satélite indisponível. Exibindo mapa-base.',
        )
      }
    })
    mapRef.current = map
    const observer = new ResizeObserver(() => map.resize())
    observer.observe(element)
    return () => {
      observer.disconnect()
      map.remove()
      mapRef.current = null
      setPronto(false)
    }
  }, [])

  useEffect(() => {
    const map = mapRef.current
    if (!map || !pronto) return
    ;(map.getSource(fonteContorno) as GeoJSONSource | undefined)?.setData(colecao(pinos))
  }, [pinos, pronto])

  useEffect(() => {
    const map = mapRef.current
    if (!map) return
    const markers = pinos.map((pino, indice) => {
      const element = document.createElement('div')
      element.className = 'farm-outline-pin'
      element.textContent = String(indice + 1)
      element.setAttribute('aria-label', `Canto ${indice + 1}`)
      element.addEventListener('click', (event) => event.stopPropagation())
      const marker = new Marker({ element, draggable: !desabilitado, anchor: 'center' })
        .setLngLat([pino.longitude, pino.latitude])
        .addTo(map)
      marker.on('dragend', () => {
        const { lng, lat } = marker.getLngLat()
        onMoverRef.current(indice, { latitude: lat, longitude: lng })
      })
      return marker
    })
    return () => markers.forEach((marker) => marker.remove())
  }, [desabilitado, pinos])

  useEffect(() => {
    const map = mapRef.current
    if (!map || !destino) return
    const duration = window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 0 : 700
    if (destino.limites) {
      const [oeste, sul, leste, norte] = destino.limites
      map.fitBounds([[oeste, sul], [leste, norte]], { padding: 48, maxZoom: 16, duration })
    } else {
      map.easeTo({ center: [destino.longitude, destino.latitude], zoom: 15, duration })
    }
  }, [destino])

  return (
    <div className="relative h-full min-h-80 overflow-hidden rounded-controle border border-linha">
      <div ref={container} className="h-full w-full" />
      {aviso && (
        <p className="absolute bottom-2 left-2 rounded-controle bg-cerca/90 px-3 py-1 text-sm text-tinta">{aviso}</p>
      )}
    </div>
  )
}

function colecao(pinos: Coordenada[]): FeatureCollection<Polygon | LineString> {
  const posicoes = pinos.map((pino) => [pino.longitude, pino.latitude])
  const features: Feature<Polygon | LineString>[] = []
  if (posicoes.length >= 3) {
    features.push({ type: 'Feature', properties: {}, geometry: { type: 'Polygon', coordinates: [[...posicoes, posicoes[0]]] } })
  } else if (posicoes.length === 2) {
    features.push({ type: 'Feature', properties: {}, geometry: { type: 'LineString', coordinates: posicoes } })
  }
  return { type: 'FeatureCollection', features }
}

function limitesDe(pinos: Coordenada[]): LngLatBounds {
  const bounds = new LngLatBounds()
  pinos.forEach((pino) => bounds.extend([pino.longitude, pino.latitude]))
  return bounds
}
