import { useEffect, useRef, useState } from 'react';
import { Map, NavigationControl, type StyleSpecification } from 'maplibre-gl';
import 'maplibre-gl/dist/maplibre-gl.css';

import { createMapTilerLogo, createMapTilerTileJsonUrl, loadMapTilerSatellite } from './mapTilerSatellite';
import { focusPlot, mountPlotMap, setPlotSelected, setSatelliteOverlay, type PlotFeature } from './mountPlotMap';
import './plotMap.css';

const mapTilerTileJson = createMapTilerTileJsonUrl(import.meta.env.VITE_MAPTILER_KEY);

interface PlotMapProps {
  plot: PlotFeature;
  mapStyle: string | StyleSpecification;
  onSelect: (plot: PlotFeature) => void;
}

interface MapReadiness {
  plot: PlotFeature;
  style: PlotMapProps['mapStyle'];
  ready: boolean;
}

/** Renders the map and reports selection to the screen that hosts it. */
export function PlotMap({ plot, mapStyle, onSelect }: PlotMapProps) {
  const mapContainer = useRef<HTMLDivElement>(null);
  const mapInstance = useRef<Map | null>(null);
  const selectedPlot = useRef<PlotFeature | null>(null);
  const imageryRequest = useRef<ReturnType<typeof loadMapTilerSatellite> | null>(null);
  const onSelectRef = useRef(onSelect);
  const [readiness, setReadiness] = useState<MapReadiness | null>(null);
  const [showImagery, setShowImagery] = useState(Boolean(mapTilerTileJson));
  const [imageryUnavailable, setImageryUnavailable] = useState(false);
  const currentReadiness = readiness?.plot === plot && readiness.style === mapStyle ? readiness.ready : undefined;

  useEffect(() => {
    onSelectRef.current = onSelect;
  }, [onSelect]);

  useEffect(() => {
    const container = mapContainer.current;
    if (!container) return;

    const timeout = window.setTimeout(() => {
      setReadiness({ plot, style: mapStyle, ready: false });
    }, 20_000);

    try {
      const map = mountPlotMap(Map, container, plot, mapStyle,
        (selected) => {
          selectedPlot.current = selected;
          onSelectRef.current(selected);
        },
        () => {
          window.clearTimeout(timeout);
          setPlotSelected(map, plot, selectedPlot.current === plot);
          setReadiness({ plot, style: mapStyle, ready: true });
        },
      );
      map.addControl(new NavigationControl({ showCompass: false }), 'top-right');
      mapInstance.current = map;
      return () => {
        window.clearTimeout(timeout);
        mapInstance.current = null;
        map.remove();
      };
    } catch {
      window.clearTimeout(timeout);
      queueMicrotask(() => setReadiness({ plot, style: mapStyle, ready: false }));
    }
  }, [plot, mapStyle]);

  useEffect(() => {
    const map = mapInstance.current;
    if (!map || !currentReadiness || !mapTilerTileJson) return;
    if (!showImagery) {
      setSatelliteOverlay(map, undefined, false);
      return;
    }

    let active = true;
    let logo: ReturnType<typeof createMapTilerLogo> | undefined;
    const request = imageryRequest.current ??= loadMapTilerSatellite(mapTilerTileJson);
    void request.then((source) => {
      if (!active) return;

      setSatelliteOverlay(map, source, true);
      logo = createMapTilerLogo();
      map.addControl(logo, 'bottom-left');
    }).catch(() => {
      if (imageryRequest.current === request) imageryRequest.current = null;
      if (!active) return;

      setShowImagery(false);
      setImageryUnavailable(true);
    });

    return () => {
      active = false;
      if (logo && mapInstance.current === map) map.removeControl(logo);
    };
  }, [currentReadiness, mapStyle, plot, showImagery]);

  function selectPlot() {
    selectedPlot.current = plot;
    if (mapInstance.current) setPlotSelected(mapInstance.current, plot, true);
    onSelect(plot);
  }

  function recenterPlot() {
    const map = mapInstance.current;
    if (!map) return;

    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    focusPlot(map, plot, { duration: reduceMotion ? 0 : 450 });
  }

  return (
    <section aria-label={`Mapa de ${plot.properties.nome}`} className="plot-map">
      <div className="plot-map__controls">
        {mapTilerTileJson && (
          <button aria-pressed={showImagery} disabled={!currentReadiness} type="button" onClick={() => {
            setImageryUnavailable(false);
            setShowImagery(!showImagery);
          }}>
            Imagem de satélite
          </button>
        )}
        <button disabled={!currentReadiness} onClick={recenterPlot} type="button">
          Enquadrar talhão
        </button>
      </div>
      <div className="plot-map__canvas" ref={mapContainer} aria-busy={currentReadiness === undefined} />
      {currentReadiness === undefined && <p role="status">Carregando mapa do talhão…</p>}
      {currentReadiness === false && <p role="status">O mapa do talhão não carregou.</p>}
      {imageryUnavailable && <p role="status">Imagem de satélite indisponível. Exibindo mapa-base.</p>}
      <button onClick={selectPlot} type="button">
        Selecionar {plot.properties.nome}
      </button>
    </section>
  );
}
