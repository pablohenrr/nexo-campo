import type { Feature, Polygon } from 'geojson';
import type {
  FitBoundsOptions,
  LngLatBoundsLike,
  Map as MapLibreMap,
  RasterDEMSourceSpecification,
  RasterSourceSpecification,
  StyleSpecification,
} from 'maplibre-gl';

export type PlotFeature = Feature<Polygon, {
  nomeFazenda: string;
  nome: string;
  tipoCultura: string;
  areaHectares: number;
}>;

const sourceId = 'nexo-plot';
const fillLayerId = 'nexo-plot-fill';
const satelliteLayerId = 'nexo-satellite';
const aerialLayerId = 'nexo-aerial';
const terrainSourceId = 'nexo-terrain';
const fitBoundsOptions = { padding: 48, maxZoom: 17, duration: 0 };
const mapLocale = {
  'AttributionControl.ToggleAttribution': 'Mostrar ou ocultar créditos do mapa',
  'FullscreenControl.Enter': 'Entrar em tela cheia',
  'FullscreenControl.Exit': 'Sair da tela cheia',
  'Map.Title': 'Mapa do talhão',
  'NavigationControl.ResetBearing': 'Arraste para girar o mapa ou clique para apontar para o norte',
  'NavigationControl.ZoomIn': 'Aproximar',
  'NavigationControl.ZoomOut': 'Afastar',
};

function getPlotBounds(plot: PlotFeature): LngLatBoundsLike {
  let west = Infinity;
  let south = Infinity;
  let east = -Infinity;
  let north = -Infinity;

  for (const ring of plot.geometry.coordinates) {
    const first = ring[0];
    const last = ring[ring.length - 1];
    if (ring.length < 4 || !first || !last || first.length !== last.length
      || first.some((value, index) => value !== last[index])) {
      throw new Error('Cada anel do talhão precisa ter pelo menos quatro posições e terminar no ponto inicial.');
    }

    for (const [longitude, latitude] of ring) {
      if (!Number.isFinite(longitude) || !Number.isFinite(latitude)
        || Math.abs(longitude) > 180 || Math.abs(latitude) > 90) {
        throw new Error('O talhão precisa de coordenadas válidas para ser exibido no mapa.');
      }
      west = Math.min(west, longitude);
      south = Math.min(south, latitude);
      east = Math.max(east, longitude);
      north = Math.max(north, latitude);
    }
  }

  if (west >= east || south >= north) {
    throw new Error('O contorno do talhão precisa delimitar uma área.');
  }

  return [[west, south], [east, north]];
}

export function focusPlot(map: MapLibreMap, plot: PlotFeature, options?: FitBoundsOptions): void {
  map.fitBounds(getPlotBounds(plot), { ...fitBoundsOptions, ...options });
}

export function isPlotVisible(map: MapLibreMap): boolean {
  return Boolean(map.getLayer(fillLayerId) && map.queryRenderedFeatures({ layers: [fillLayerId] }).length);
}

export function setPlotSelected(map: MapLibreMap, plot: PlotFeature, selected: boolean): void {
  if (map.getSource(sourceId)) {
    map.setFeatureState({ source: sourceId, id: plot.id ?? 0 }, { selected });
  }
}

function setRasterOverlay(
  map: MapLibreMap,
  id: string,
  enabled: boolean,
  source?: RasterSourceSpecification,
): void {
  if (enabled && source && !map.getSource(id)) {
    map.addSource(id, source);
  }

  if (enabled && source && !map.getLayer(id)) {
    map.addLayer({ id, type: 'raster', source: id }, fillLayerId);
  }

  if (map.getLayer(id)) {
    map.setLayoutProperty(id, 'visibility', enabled && source ? 'visible' : 'none');
  }
}

/** Adds optional local imagery above the global scene and below the parcel. */
export function setSatelliteOverlay(
  map: MapLibreMap,
  source: RasterSourceSpecification | undefined,
  enabled: boolean,
  aerial?: RasterSourceSpecification,
): void {
  setRasterOverlay(map, satelliteLayerId, enabled, source);
  setRasterOverlay(map, aerialLayerId, enabled, aerial);
}

export function setTerrainMode(map: MapLibreMap, source: RasterDEMSourceSpecification, enabled: boolean): void {
  if (enabled && !map.getSource(terrainSourceId)) {
    map.addSource(terrainSourceId, source);
  }

  map.setTerrain(enabled ? { source: terrainSourceId, exaggeration: 1.25 } : null);
}

export function mountPlotMap(
  Map: typeof MapLibreMap,
  container: HTMLElement,
  plot: PlotFeature,
  style: string | StyleSpecification,
  onSelect: (plot: PlotFeature) => void,
  onReady?: () => void,
): MapLibreMap {
  const bounds = getPlotBounds(plot);
  const map = new Map({
    container,
    style,
    bounds,
    fitBoundsOptions,
    locale: mapLocale,
    attributionControl: { compact: false },
  });

  map.once('load', () => {
    map.resize();
    focusPlot(map, plot);
    const feature = { source: sourceId, id: plot.id ?? 0 };
    map.addSource(sourceId, { type: 'geojson', data: { ...plot, id: feature.id } });
    map.addLayer({
      id: fillLayerId,
      type: 'fill',
      source: sourceId,
      paint: {
        'fill-color': '#76db7c',
        'fill-opacity': ['case', ['boolean', ['feature-state', 'selected'], false], 0.28,
          ['boolean', ['feature-state', 'hover'], false], 0.22, 0.12],
      },
    });
    map.addLayer({
      id: 'nexo-plot-halo',
      type: 'line',
      source: sourceId,
      paint: { 'line-color': '#153425', 'line-width': 5, 'line-opacity': 0.85 },
    });
    map.addLayer({
      id: 'nexo-plot-outline',
      type: 'line',
      source: sourceId,
      paint: { 'line-color': '#a5f19d', 'line-width': 2 },
    });

    map.on('click', fillLayerId, () => {
      setPlotSelected(map, plot, true);
      onSelect(plot);
    });
    map.on('mouseenter', fillLayerId, () => {
      map.setFeatureState(feature, { hover: true });
      map.getCanvas().style.cursor = 'pointer';
    });
    map.on('mouseleave', fillLayerId, () => {
      map.setFeatureState(feature, { hover: false });
      map.getCanvas().style.cursor = '';
    });

    if (!onReady) return;

    const confirmRender = () => {
      if (!container.clientWidth || !container.clientHeight) return;
      if (!isPlotVisible(map)) return;

      map.off('idle', confirmRender);
      onReady();
    };
    map.on('idle', confirmRender);
  });

  return map;
}
