import type { IControl, RasterDEMSourceSpecification, RasterSourceSpecification } from 'maplibre-gl';

const mapTilerAttribution = '<a href="https://www.maptiler.com/copyright/">© MapTiler</a>';

export function createMapTilerTileJsonUrl(apiKey?: string): string | undefined {
  const key = apiKey?.trim();
  if (!key) return;

  const query = new URLSearchParams({ key });
  return `https://api.maptiler.com/tiles/satellite-v2/tiles.json?${query}`;
}

export async function loadMapTilerSatellite(tileJsonUrl: string): Promise<RasterSourceSpecification> {
  const response = await fetch(tileJsonUrl, { signal: AbortSignal.timeout(5000) });
  if (!response.ok) throw new Error('MapTiler indisponível.');

  const { tiles, minzoom = 0, maxzoom = 22, attribution } = await response.json();
  if (!Array.isArray(tiles) || tiles.length === 0) throw new Error('MapTiler retornou uma fonte sem imagens.');

  return {
    type: 'raster',
    tiles,
    tileSize: 512,
    minzoom,
    maxzoom,
    attribution: typeof attribution === 'string' && attribution.includes('maptiler.com')
      ? attribution
      : [mapTilerAttribution, attribution].filter(Boolean).join(' · '),
  };
}

export function createMapTilerLogo(): IControl {
  const container = document.createElement('div');
  container.className = 'maplibregl-ctrl';
  const link = document.createElement('a');
  link.href = 'https://www.maptiler.com/';
  link.target = '_blank';
  link.rel = 'noopener noreferrer';
  const image = document.createElement('img');
  image.src = 'https://api.maptiler.com/resources/logo.svg';
  image.alt = 'MapTiler';
  image.width = 86;
  link.append(image);
  container.append(link);

  return { onAdd: () => container, onRemove: () => container.remove() };
}

export function createMapTilerTerrainUrl(apiKey?: string): string | undefined {
  const key = apiKey?.trim();
  if (!key) return;

  const query = new URLSearchParams({ key });
  // v2 entrega WebP. O decodificador de relevo do MapLibre trata o arquivo como PNG e a maquete não sobe.
  return `https://api.maptiler.com/tiles/terrain-rgb/tiles.json?${query}`;
}

export async function loadMapTilerTerrain(tileJsonUrl: string): Promise<RasterDEMSourceSpecification> {
  const response = await fetch(tileJsonUrl, { signal: AbortSignal.timeout(5000) });
  if (!response.ok) throw new Error('MapTiler indisponível.');

  const { tiles, minzoom = 0, maxzoom = 12, attribution } = await response.json();
  if (!Array.isArray(tiles) || tiles.length === 0) throw new Error('MapTiler retornou uma fonte sem imagens.');

  return {
    type: 'raster-dem',
    tiles,
    tileSize: 512,
    minzoom,
    maxzoom,
    encoding: 'mapbox',
    attribution: typeof attribution === 'string' && attribution.includes('maptiler.com')
      ? attribution
      : [mapTilerAttribution, attribution].filter(Boolean).join(' · '),
  };
}
