import type { PlotFeature } from '@/features/maps/mountPlotMap.ts'

const colunasGrade = 72
const linhasGrade = 72

export type ModeloMaquete = {
  oeste: number
  sul: number
  leste: number
  norte: number
  largura: number
  profundidade: number
  colunas: number
  linhas: number
  alturas: Float32Array
  terreno: boolean
  satelite: boolean
  imagem: HTMLCanvasElement | null
  amostrar: (longitude: number, latitude: number) => number
  projetar: (longitude: number, latitude: number) => { x: number; norte: number }
}

type Quadro = {
  oeste: number
  sul: number
  leste: number
  norte: number
}

type TileRelevo = {
  x: number
  y: number
  zoom: number
  largura: number
  pixels: Uint8ClampedArray
}

export async function carregarMaquete(
  features: PlotFeature[],
  center: { latitude: number; longitude: number } | null,
  signal: AbortSignal,
): Promise<ModeloMaquete | null> {
  const quadro = enquadrar(features, center)
  if (!quadro) return null

  const origemLon = (quadro.oeste + quadro.leste) / 2
  const origemLat = (quadro.sul + quadro.norte) / 2
  const metrosLon = 111_320 * Math.cos((origemLat * Math.PI) / 180)
  const metrosLat = 110_540
  const largura = Math.max((quadro.leste - quadro.oeste) * metrosLon, 1)
  const profundidade = Math.max((quadro.norte - quadro.sul) * metrosLat, 1)
  const chave = import.meta.env.VITE_MAPTILER_KEY?.trim()

  const [tiles, imagem] = chave
    ? await Promise.all([
        baixarRelevo(quadro, chave, signal),
        baixarSatelite(quadro, chave, signal),
      ])
    : [[], null]

  const alturas = new Float32Array(colunasGrade * linhasGrade)
  let validas = 0
  let soma = 0
  for (let linha = 0; linha < linhasGrade; linha += 1) {
    const v = linha / (linhasGrade - 1)
    const latitude = quadro.norte - v * (quadro.norte - quadro.sul)
    for (let coluna = 0; coluna < colunasGrade; coluna += 1) {
      const u = coluna / (colunasGrade - 1)
      const longitude = quadro.oeste + u * (quadro.leste - quadro.oeste)
      const altura = lerAltitude(tiles, longitude, latitude)
      const indice = linha * colunasGrade + coluna
      if (altura == null) {
        alturas[indice] = Number.NaN
      } else {
        alturas[indice] = altura
        validas += 1
        soma += altura
      }
    }
  }

  const terreno = validas > 0
  const media = terreno ? soma / validas : 0
  let minimo = Number.POSITIVE_INFINITY
  let maximo = Number.NEGATIVE_INFINITY
  for (let indice = 0; indice < alturas.length; indice += 1) {
    const altura = alturas[indice] ?? Number.NaN
    const firme = Number.isFinite(altura) ? altura : media
    alturas[indice] = firme
    minimo = Math.min(minimo, firme)
    maximo = Math.max(maximo, firme)
  }
  if (!terreno) {
    minimo = 0
    maximo = 0
  }

  const desnivel = Math.max(maximo - minimo, 0)
  const diagonal = Math.hypot(largura, profundidade)
  const exagero = desnivel > 1 ? Math.min(6, Math.max(1, (diagonal * 0.14) / desnivel)) : 1

  return {
    oeste: quadro.oeste,
    sul: quadro.sul,
    leste: quadro.leste,
    norte: quadro.norte,
    largura,
    profundidade,
    colunas: colunasGrade,
    linhas: linhasGrade,
    alturas,
    terreno,
    satelite: imagem != null,
    imagem,
    projetar(longitude, latitude) {
      return {
        x: (longitude - origemLon) * metrosLon,
        norte: (latitude - origemLat) * metrosLat,
      }
    },
    amostrar(longitude, latitude) {
      if (!terreno) return 0
      const u = limitar((longitude - quadro.oeste) / (quadro.leste - quadro.oeste), 0, 1)
      const v = limitar((quadro.norte - latitude) / (quadro.norte - quadro.sul), 0, 1)
      const x = u * (colunasGrade - 1)
      const y = v * (linhasGrade - 1)
      const x0 = Math.floor(x)
      const y0 = Math.floor(y)
      const x1 = Math.min(x0 + 1, colunasGrade - 1)
      const y1 = Math.min(y0 + 1, linhasGrade - 1)
      const tx = x - x0
      const ty = y - y0
      const h00 = alturas[y0 * colunasGrade + x0] ?? minimo
      const h10 = alturas[y0 * colunasGrade + x1] ?? minimo
      const h01 = alturas[y1 * colunasGrade + x0] ?? minimo
      const h11 = alturas[y1 * colunasGrade + x1] ?? minimo
      const altura = h00 * (1 - tx) * (1 - ty) + h10 * tx * (1 - ty) + h01 * (1 - tx) * ty + h11 * tx * ty
      return (altura - minimo) * exagero
    },
  }
}

function enquadrar(
  features: PlotFeature[],
  center: { latitude: number; longitude: number } | null,
): Quadro | null {
  let oeste = Number.POSITIVE_INFINITY
  let sul = Number.POSITIVE_INFINITY
  let leste = Number.NEGATIVE_INFINITY
  let norte = Number.NEGATIVE_INFINITY

  for (const feature of features) {
    for (const anel of feature.geometry.coordinates) {
      for (const posicao of anel) {
        const longitude = posicao[0]
        const latitude = posicao[1]
        if (longitude == null || latitude == null || !Number.isFinite(longitude) || !Number.isFinite(latitude)) continue
        oeste = Math.min(oeste, longitude)
        sul = Math.min(sul, latitude)
        leste = Math.max(leste, longitude)
        norte = Math.max(norte, latitude)
      }
    }
  }

  if (!Number.isFinite(oeste)) {
    if (!center) return null
    oeste = center.longitude
    leste = center.longitude
    sul = center.latitude
    norte = center.latitude
  }

  const meioLon = (oeste + leste) / 2
  const meioLat = (sul + norte) / 2
  const meioLargura = Math.max((leste - oeste) / 2, 0.004) * 1.35
  const meioAltura = Math.max((norte - sul) / 2, 0.004) * 1.35
  return {
    oeste: meioLon - meioLargura,
    leste: meioLon + meioLargura,
    sul: meioLat - meioAltura,
    norte: meioLat + meioAltura,
  }
}

async function baixarRelevo(quadro: Quadro, chave: string, signal: AbortSignal): Promise<TileRelevo[]> {
  const zoom = zoomDoQuadro(quadro)
  const x0 = Math.floor(colunaDoLongitude(quadro.oeste, zoom))
  const x1 = Math.floor(colunaDoLongitude(quadro.leste, zoom))
  const y0 = Math.floor(linhaDaLatitude(quadro.norte, zoom))
  const y1 = Math.floor(linhaDaLatitude(quadro.sul, zoom))
  const pedidos: Array<Promise<TileRelevo | null>> = []

  for (let y = y0; y <= y1; y += 1) {
    for (let x = x0; x <= x1; x += 1) {
      const url = new URL(`https://api.maptiler.com/tiles/terrain-rgb/${zoom}/${x}/${y}.png`)
      url.searchParams.set('key', chave)
      pedidos.push(lerTile(url, x, y, zoom, signal))
    }
  }

  const tiles = await Promise.all(pedidos)
  return tiles.flatMap((tile) => (tile ? [tile] : []))
}

async function lerTile(
  url: URL,
  x: number,
  y: number,
  zoom: number,
  signal: AbortSignal,
): Promise<TileRelevo | null> {
  try {
    const resposta = await fetch(url, { signal: AbortSignal.any([signal, AbortSignal.timeout(8000)]) })
    if (!resposta.ok) return null
    const bitmap = await createImageBitmap(await resposta.blob())
    const tela = document.createElement('canvas')
    tela.width = bitmap.width
    tela.height = bitmap.height
    const contexto = tela.getContext('2d', { willReadFrequently: true })
    if (!contexto) {
      bitmap.close()
      return null
    }
    contexto.drawImage(bitmap, 0, 0)
    const pixels = contexto.getImageData(0, 0, bitmap.width, bitmap.height).data
    const largura = bitmap.width
    bitmap.close()
    return { x, y, zoom, largura, pixels }
  } catch {
    return null
  }
}

function lerAltitude(tiles: TileRelevo[], longitude: number, latitude: number): number | null {
  const tile = tiles.find((item) => itemContem(item, longitude, latitude))
  if (!tile) return null
  const fx = colunaDoLongitude(longitude, tile.zoom) - tile.x
  const fy = linhaDaLatitude(latitude, tile.zoom) - tile.y
  const px = Math.min(tile.largura - 1, Math.max(0, Math.floor(fx * tile.largura)))
  const py = Math.min(tile.largura - 1, Math.max(0, Math.floor(fy * tile.largura)))
  const indice = (py * tile.largura + px) * 4
  const vermelho = tile.pixels[indice]
  const verde = tile.pixels[indice + 1]
  const azul = tile.pixels[indice + 2]
  const alfa = tile.pixels[indice + 3]
  if (vermelho == null || verde == null || azul == null || alfa == null || alfa === 0) return null
  const altura = -10_000 + (vermelho * 65_536 + verde * 256 + azul) * 0.1
  if (altura < -500 || altura > 9000) return null
  return altura
}

function itemContem(tile: TileRelevo, longitude: number, latitude: number): boolean {
  const x = Math.floor(colunaDoLongitude(longitude, tile.zoom))
  const y = Math.floor(linhaDaLatitude(latitude, tile.zoom))
  return tile.x === x && tile.y === y
}

function zoomDoQuadro(quadro: Quadro): number {
  for (let zoom = 12; zoom >= 0; zoom -= 1) {
    const colunas = Math.floor(colunaDoLongitude(quadro.leste, zoom)) - Math.floor(colunaDoLongitude(quadro.oeste, zoom)) + 1
    const linhas = Math.floor(linhaDaLatitude(quadro.sul, zoom)) - Math.floor(linhaDaLatitude(quadro.norte, zoom)) + 1
    if (colunas * linhas <= 4 || zoom === 0) return zoom
  }
  return 0
}

function colunaDoLongitude(longitude: number, zoom: number): number {
  return ((longitude + 180) / 360) * 2 ** zoom
}

function linhaDaLatitude(latitude: number, zoom: number): number {
  const radiano = (latitude * Math.PI) / 180
  const mercator = Math.log(Math.tan(radiano) + 1 / Math.cos(radiano))
  return ((1 - mercator / Math.PI) / 2) * 2 ** zoom
}

/**
 * Monta a textura da maquete com os tiles de satélite do MapTiler, os mesmos do modo Satélite.
 * A API de mapas estáticos não entra no plano gratuito e respondia erro, deixando a maquete cinza.
 */
async function baixarSatelite(quadro: Quadro, chave: string, signal: AbortSignal): Promise<HTMLCanvasElement | null> {
  const zoom = zoomDaImagem(quadro)
  const x0 = Math.floor(colunaDoLongitude(quadro.oeste, zoom))
  const x1 = Math.floor(colunaDoLongitude(quadro.leste, zoom))
  const y0 = Math.floor(linhaDaLatitude(quadro.norte, zoom))
  const y1 = Math.floor(linhaDaLatitude(quadro.sul, zoom))
  const lado = 512

  const pedidos: Array<Promise<{ x: number; y: number; bitmap: ImageBitmap } | null>> = []
  for (let y = y0; y <= y1; y += 1) {
    for (let x = x0; x <= x1; x += 1) {
      const url = new URL(`https://api.maptiler.com/tiles/satellite-v2/${zoom}/${x}/${y}.jpg`)
      url.searchParams.set('key', chave)
      pedidos.push(
        fetch(url, { signal: AbortSignal.any([signal, AbortSignal.timeout(8000)]) })
          .then(async (resposta) => (resposta.ok ? { x, y, bitmap: await createImageBitmap(await resposta.blob()) } : null))
          .catch(() => null),
      )
    }
  }
  const tiles = (await Promise.all(pedidos)).flatMap((tile) => (tile ? [tile] : []))
  if (!tiles.length) return null

  // Recorta exatamente a caixa da maquete dentro do mosaico de tiles.
  const esquerda = (colunaDoLongitude(quadro.oeste, zoom) - x0) * lado
  const direita = (colunaDoLongitude(quadro.leste, zoom) - x0) * lado
  const topo = (linhaDaLatitude(quadro.norte, zoom) - y0) * lado
  const base = (linhaDaLatitude(quadro.sul, zoom) - y0) * lado
  const escala = Math.min(1, 1536 / Math.max(direita - esquerda, base - topo))
  const tela = document.createElement('canvas')
  tela.width = Math.max(1, Math.round((direita - esquerda) * escala))
  tela.height = Math.max(1, Math.round((base - topo) * escala))
  const contexto = tela.getContext('2d')
  if (!contexto) {
    tiles.forEach((tile) => tile.bitmap.close())
    return null
  }
  contexto.scale(escala, escala)
  contexto.translate(-esquerda, -topo)
  for (const tile of tiles) {
    contexto.drawImage(tile.bitmap, (tile.x - x0) * lado, (tile.y - y0) * lado, lado, lado)
    tile.bitmap.close()
  }
  return tiles.length === (x1 - x0 + 1) * (y1 - y0 + 1) ? tela : null
}

function zoomDaImagem(quadro: Quadro): number {
  for (let zoom = 17; zoom >= 0; zoom -= 1) {
    const colunas = Math.floor(colunaDoLongitude(quadro.leste, zoom)) - Math.floor(colunaDoLongitude(quadro.oeste, zoom)) + 1
    const linhas = Math.floor(linhaDaLatitude(quadro.sul, zoom)) - Math.floor(linhaDaLatitude(quadro.norte, zoom)) + 1
    if (colunas * linhas <= 9 || zoom === 0) return zoom
  }
  return 0
}

function limitar(valor: number, minimo: number, maximo: number): number {
  return Math.min(maximo, Math.max(minimo, valor))
}
