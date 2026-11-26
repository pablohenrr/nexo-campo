import type { Feature, FeatureCollection, MultiLineString, Polygon, Position } from 'geojson';
import type {
  ExpressionSpecification,
  FitBoundsOptions,
  GeoJSONSource,
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
  talhaoId?: string;
  estado?: string;
}>;

const sourceId = 'nexo-plot';
const fillLayerId = 'nexo-plot-fill';
const satelliteLayerId = 'nexo-satellite';
const aerialLayerId = 'nexo-aerial';
const terrainSourceId = 'nexo-terrain';
const hillshadeLayerId = 'nexo-hillshade';
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
    // Imagem sempre por baixo: dos cantos da fazenda, se existirem, e dos talhões.
    const before = [farmFill, fillLayerId, 'nexo-field-fill'].find((layer) => map.getLayer(layer));
    map.addLayer({ id, type: 'raster', source: id }, before);
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

export function setTerrainMode(
  map: MapLibreMap,
  source: RasterDEMSourceSpecification | undefined,
  enabled: boolean,
): void {
  if (enabled && source && !map.getSource(terrainSourceId)) {
    map.addSource(terrainSourceId, source);
  }

  if (!map.getSource(terrainSourceId)) {
    map.setTerrain(null);
    return;
  }

  map.setTerrain(enabled ? { source: terrainSourceId, exaggeration: 1.5 } : null);

  if (enabled && !map.getLayer(hillshadeLayerId)) {
    const before = map.getLayer(fillLayerId)
      ? fillLayerId
      : map.getLayer('nexo-field-fill')
        ? 'nexo-field-fill'
        : undefined;
    map.addLayer({
      id: hillshadeLayerId,
      type: 'hillshade',
      source: terrainSourceId,
      paint: {
        'hillshade-exaggeration': 0.45,
        'hillshade-shadow-color': '#1a2e24',
        'hillshade-highlight-color': '#f4f7f2',
        'hillshade-accent-color': '#6b7c72',
      },
    }, before);
  }

  if (map.getLayer(hillshadeLayerId)) {
    map.setLayoutProperty(hillshadeLayerId, 'visibility', enabled ? 'visible' : 'none');
  }
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

const fieldFill = 'nexo-field-fill';
const fieldLine = 'nexo-field-line';
const fieldCasing = 'nexo-field-casing';
const farmSource = 'nexo-farm';
const farmFill = 'nexo-farm-fill';
const farmCornersSource = 'nexo-farm-corners';
const farmCornersCasing = 'nexo-farm-corners-casing';
const farmCorners = 'nexo-farm-corners-line';

/** Fração de cada lado desenhada a partir do canto (cantoneiras da fazenda). */
const cornerFraction = 0.18;

function addFarmLayers(map: MapLibreMap): void {
  map.addSource(farmSource, { type: 'geojson', data: { type: 'FeatureCollection', features: [] } });
  map.addSource(farmCornersSource, { type: 'geojson', data: { type: 'FeatureCollection', features: [] } });
  // Área da fazenda bem suave; os talhões vêm por cima, mais fortes.
  map.addLayer({
    id: farmFill,
    type: 'fill',
    source: farmSource,
    paint: { 'fill-color': '#FFFFFF', 'fill-opacity': 0.07 },
  });
  map.addLayer({
    id: farmCornersCasing,
    type: 'line',
    source: farmCornersSource,
    layout: { 'line-cap': 'round', 'line-join': 'round' },
    paint: { 'line-color': '#0F291E', 'line-width': 6, 'line-opacity': 0.55 },
  });
  map.addLayer({
    id: farmCorners,
    type: 'line',
    source: farmCornersSource,
    layout: { 'line-cap': 'round', 'line-join': 'round' },
    paint: { 'line-color': '#F6F8F3', 'line-width': 3 },
  });
}

/**
 * Marca a fazenda só pelos cantos: em cada vértice, um trecho curto de cada lado.
 * Fica legível no satélite sem competir com os talhões.
 */
export function setFarmOutline(map: MapLibreMap, outline: Array<{ latitude: number; longitude: number }> | null): void {
  const area = map.getSource(farmSource) as GeoJSONSource | undefined;
  const corners = map.getSource(farmCornersSource) as GeoJSONSource | undefined;
  if (!area || !corners) return;
  const ring: Position[] = (outline ?? []).map((point) => [point.longitude, point.latitude]);
  if (ring.length < 3) {
    area.setData({ type: 'FeatureCollection', features: [] });
    corners.setData({ type: 'FeatureCollection', features: [] });
    return;
  }
  area.setData({
    type: 'Feature',
    properties: {},
    geometry: { type: 'Polygon', coordinates: [[...ring, ring[0]!]] },
  });
  const lines: Position[][] = ring.map((vertex, index) => {
    const previous = ring[(index - 1 + ring.length) % ring.length]!;
    const next = ring[(index + 1) % ring.length]!;
    return [toward(vertex, previous), vertex, toward(vertex, next)];
  });
  const geometry: MultiLineString = { type: 'MultiLineString', coordinates: lines };
  corners.setData({ type: 'Feature', properties: {}, geometry });
}

function toward(from: Position, to: Position): Position {
  return [from[0]! + (to[0]! - from[0]!) * cornerFraction, from[1]! + (to[1]! - from[1]!) * cornerFraction];
}
const fillColor: ExpressionSpecification = [
  'match',
  ['coalesce', ['get', 'estado'], 'MONITORAMENTO'],
  'INFESTACAO_ALTA', '#B91C1C',
  'ALERTA_MEDIO', '#B45309',
  'ALERTA_BAIXO', '#067647',
  'COLHEITA', '#B45309',
  'INATIVO', '#8A978F',
  '#D7E6DC',
];

export function boundsOf(features: PlotFeature[]): LngLatBoundsLike | null {
  let west = Infinity;
  let south = Infinity;
  let east = -Infinity;
  let north = -Infinity;

  for (const feature of features) {
    for (const ring of feature.geometry.coordinates) {
      for (const position of ring) {
        const longitude = position[0];
        const latitude = position[1];
        if (longitude == null || latitude == null) continue;
        west = Math.min(west, longitude);
        south = Math.min(south, latitude);
        east = Math.max(east, longitude);
        north = Math.max(north, latitude);
      }
    }
  }

  if (!Number.isFinite(west) || west >= east || south >= north) return null;
  return [[west, south], [east, north]];
}

export function setFieldPlots(map: MapLibreMap, features: PlotFeature[], selectedId: string | null): void {
  const source = map.getSource(sourceId) as GeoJSONSource | undefined;
  if (!source || !map.getLayer(fieldFill)) return;

  const collection: FeatureCollection<Polygon, PlotFeature['properties']> = {
    type: 'FeatureCollection',
    features: features.map((feature) => ({
      ...feature,
      id: feature.properties.talhaoId ?? feature.id,
    })),
  };
  source.setData(collection);

  for (const feature of collection.features) {
    if (feature.id == null) continue;
    map.setFeatureState(
      { source: sourceId, id: feature.id },
      { selected: feature.id === selectedId },
    );
  }
}

/** Mapa da fazenda inteira. O talhão único continua em mountPlotMap. */
export function mountFieldMap(
  Map: typeof MapLibreMap,
  container: HTMLElement,
  style: string | StyleSpecification,
  onSelect: (talhaoId: string) => void,
  onReady?: () => void,
): MapLibreMap {
  const map = new Map({
    container,
    style,
    center: [-51.5, -24.6],
    zoom: 6,
    locale: mapLocale,
    attributionControl: { compact: true },
  });

  map.once('load', () => {
    map.resize();
    map.addSource(sourceId, {
      type: 'geojson',
      promoteId: 'talhaoId',
      data: { type: 'FeatureCollection', features: [] },
    });
    addFarmLayers(map);
    map.addLayer({
      id: fieldFill,
      type: 'fill',
      source: sourceId,
      paint: {
        'fill-color': fillColor,
        'fill-opacity': ['case', ['boolean', ['feature-state', 'selected'], false], 0.62, 0.48],
      },
    });
    // Contorno do talhão em duas camadas (borda escura + linha clara) para aparecer no satélite e no mapa claro.
    map.addLayer({
      id: fieldCasing,
      type: 'line',
      source: sourceId,
      layout: { 'line-join': 'round' },
      paint: {
        'line-color': '#0F291E',
        'line-width': ['case', ['boolean', ['feature-state', 'selected'], false], 6, 4.5],
        'line-opacity': 0.75,
      },
    });
    map.addLayer({
      id: fieldLine,
      type: 'line',
      source: sourceId,
      layout: { 'line-join': 'round' },
      paint: {
        'line-color': ['case', ['boolean', ['feature-state', 'selected'], false], '#F7C948', '#FFFFFF'],
        'line-width': ['case', ['boolean', ['feature-state', 'selected'], false], 3, 2],
      },
    });
    map.on('click', fieldFill, (event) => {
      const talhaoId = event.features?.[0]?.properties?.talhaoId;
      if (typeof talhaoId === 'string' && talhaoId.length > 0) onSelect(talhaoId);
    });
    map.on('mouseenter', fieldFill, () => {
      map.getCanvas().style.cursor = 'pointer';
    });
    map.on('mouseleave', fieldFill, () => {
      map.getCanvas().style.cursor = '';
    });
    onReady?.();
  });

  return map;
}
