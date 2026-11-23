import { useEffect, useRef, useState } from 'react'
import * as THREE from 'three'
import { OrbitControls } from 'three/addons/controls/OrbitControls.js'
import { carregarMaquete, type ModeloMaquete } from '@/features/maps/maquette.ts'
import type { PlotFeature } from '@/features/maps/mountPlotMap.ts'
import { corEstado, type EstadoTalhao } from '@/features/plots/status.ts'

export function FieldMaquette({
  features,
  selectedId,
  onSelect,
  center,
  visivel,
}: {
  features: PlotFeature[]
  selectedId: string | null
  onSelect: (id: string) => void
  center: { latitude: number; longitude: number } | null
  visivel: boolean
}) {
  const host = useRef<HTMLDivElement>(null)
  const cache = useRef<{ chave: string; modelo: ModeloMaquete | null } | null>(null)
  const featuresRef = useRef(features)
  const centerRef = useRef(center)
  const onSelectRef = useRef(onSelect)
  const [aviso, setAviso] = useState('Montando a maquete…')
  const [pronto, setPronto] = useState(false)
  const [credito, setCredito] = useState(false)

  const chave = [
    center?.latitude ?? '',
    center?.longitude ?? '',
    features.map((feature) => {
      const anel = feature.geometry.coordinates[0]?.length ?? 0
      return `${feature.properties.talhaoId ?? ''}:${anel}:${feature.properties.estado ?? ''}`
    }).join('|'),
  ].join(';')

  useEffect(() => {
    featuresRef.current = features
    centerRef.current = center
    onSelectRef.current = onSelect
  }, [center, features, onSelect])

  useEffect(() => {
    const elemento = host.current
    if (!elemento || !visivel) return
    const palco = elemento

    const abortar = new AbortController()
    let cancelado = false
    let quadro = 0
    let renderer: THREE.WebGLRenderer | null = null
    let controles: OrbitControls | null = null
    let observar: ResizeObserver | null = null
    let cena: THREE.Scene | null = null
    let aoPressionar: ((evento: PointerEvent) => void) | null = null
    let aoSoltar: ((evento: PointerEvent) => void) | null = null

    async function montar() {
      setPronto(false)
      setCredito(false)
      setAviso('Montando a maquete…')

      let modelo = cache.current?.chave === chave ? cache.current.modelo : undefined
      if (modelo === undefined) {
        modelo = await carregarMaquete(featuresRef.current, centerRef.current, abortar.signal)
        if (cancelado) return
        cache.current = { chave, modelo }
      }
      if (cancelado) return
      if (!modelo) {
        setAviso('Cadastre a localização da fazenda para montar a maquete.')
        return
      }

      try {
        renderer = new THREE.WebGLRenderer({ antialias: true, logarithmicDepthBuffer: true })
      } catch {
        if (!cancelado) setAviso('Este navegador não abriu a maquete 3D.')
        return
      }
      if (cancelado) {
        renderer.dispose()
        renderer.forceContextLoss()
        return
      }

      const tela = renderer
      tela.setPixelRatio(Math.min(window.devicePixelRatio, 2))
      tela.outputColorSpace = THREE.SRGBColorSpace
      tela.setClearColor(0xf8fafc, 1)
      tela.domElement.className = 'h-full w-full touch-none'
      palco.append(tela.domElement)

      cena = new THREE.Scene()
      const camera = new THREE.PerspectiveCamera(38, 1, 0.5, 100_000)
      const alcance = Math.max(modelo.largura, modelo.profundidade, 80)
      camera.position.set(alcance * 0.15, alcance * 0.7, alcance * 0.95)
      camera.far = alcance * 20
      camera.updateProjectionMatrix()

      cena.add(new THREE.HemisphereLight(0xf8fafc, 0x1a3a2a, 1.1))
      const sol = new THREE.DirectionalLight(0xfff7ea, 1.6)
      sol.position.set(alcance * 0.4, alcance, alcance * 0.2)
      cena.add(sol)
      cena.add(base(modelo))
      cena.add(terreno(modelo, tela))
      const volumes = adicionarTalhoes(cena, modelo, featuresRef.current, selectedId)

      controles = new OrbitControls(camera, tela.domElement)
      controles.target.set(0, alcance * 0.05, 0)
      controles.enableDamping = !window.matchMedia('(prefers-reduced-motion: reduce)').matches
      controles.dampingFactor = 0.08
      controles.maxPolarAngle = Math.PI / 2.08
      controles.minDistance = alcance * 0.25
      controles.maxDistance = alcance * 2.6
      controles.update()

      const ponteiro = new THREE.Vector2()
      const raio = new THREE.Raycaster()
      let inicioX = 0
      let inicioY = 0
      aoPressionar = (evento: PointerEvent) => {
        inicioX = evento.clientX
        inicioY = evento.clientY
      }
      aoSoltar = (evento: PointerEvent) => {
        if (Math.hypot(evento.clientX - inicioX, evento.clientY - inicioY) > 6) return
        const retangulo = tela.domElement.getBoundingClientRect()
        if (retangulo.width === 0) return
        ponteiro.x = ((evento.clientX - retangulo.left) / retangulo.width) * 2 - 1
        ponteiro.y = -((evento.clientY - retangulo.top) / retangulo.height) * 2 + 1
        raio.setFromCamera(ponteiro, camera)
        const acerto = raio.intersectObjects(volumes, false)[0]
        const id = acerto?.object.userData.talhaoId
        if (typeof id === 'string') onSelectRef.current(id)
      }
      tela.domElement.addEventListener('pointerdown', aoPressionar)
      tela.domElement.addEventListener('pointerup', aoSoltar)

      const ajustar = () => {
        const largura = palco.clientWidth
        const altura = palco.clientHeight
        if (largura === 0 || altura === 0) return
        camera.aspect = largura / altura
        camera.updateProjectionMatrix()
        tela.setSize(largura, altura, false)
      }
      observar = new ResizeObserver(ajustar)
      observar.observe(palco)
      ajustar()

      const desenhar = () => {
        controles?.update()
        tela.render(cena!, camera)
        quadro = requestAnimationFrame(desenhar)
      }
      if (cancelado) return
      desenhar()
      setPronto(true)
      setCredito(modelo.terreno || modelo.satelite)
      setAviso(textoDaMaquete(modelo, featuresRef.current.length))
    }

    void montar()

    return () => {
      cancelado = true
      abortar.abort()
      cancelAnimationFrame(quadro)
      observar?.disconnect()
      if (renderer && aoPressionar && aoSoltar) {
        renderer.domElement.removeEventListener('pointerdown', aoPressionar)
        renderer.domElement.removeEventListener('pointerup', aoSoltar)
      }
      controles?.dispose()
      if (cena) descartar(cena)
      if (renderer) {
        renderer.dispose()
        renderer.forceContextLoss()
        renderer.domElement.remove()
      }
    }
  }, [chave, selectedId, visivel])

  const instrucao = features.length > 0
    ? 'Arraste para girar. Clique em um talhão para selecionar.'
    : 'Arraste para girar. Desenhe um talhão para colocá-lo na maquete.'

  return (
    <div className="absolute inset-0 z-10 bg-claridade" role="region" aria-label="Maquete tridimensional da propriedade">
      <div ref={host} className="h-full w-full" />
      {credito && (
        <a
          className="absolute top-4 left-4 z-20 text-base text-chuva underline"
          href="https://www.maptiler.com/copyright/"
          target="_blank"
          rel="noreferrer"
        >
          © MapTiler
        </a>
      )}
      {pronto && !aviso && (
        <p className="pointer-events-none absolute inset-x-3 bottom-3 z-20 flex justify-center">
          <span className="rounded-controle border border-linha bg-cerca px-3 py-2 text-center text-base text-tinta">{instrucao}</span>
        </p>
      )}
      {aviso && (
        <p role="status" className="absolute bottom-12 left-3 z-20 max-w-xs rounded-controle border border-linha bg-cerca px-3 py-2 text-base text-tinta">
          {aviso}
        </p>
      )}
    </div>
  )
}

function textoDaMaquete(modelo: ModeloMaquete, quantidade: number): string {
  if (modelo.terreno && modelo.satelite) return ''
  if (!modelo.terreno && !modelo.satelite) {
    return quantidade > 0
      ? 'Imagem e relevo indisponíveis. A maquete mostra os talhões.'
      : 'Imagem e relevo indisponíveis.'
  }
  if (!modelo.terreno) return 'Relevo indisponível. A maquete segue com a imagem.'
  return 'Imagem de satélite indisponível. A maquete segue com o relevo.'
}

function base(modelo: ModeloMaquete): THREE.Mesh {
  const altura = Math.max(modelo.largura, modelo.profundidade) * 0.035
  const geometria = new THREE.BoxGeometry(modelo.largura * 1.04, altura, modelo.profundidade * 1.04)
  const material = new THREE.MeshStandardMaterial({ color: 0x0f291e, roughness: 0.85, metalness: 0 })
  const malha = new THREE.Mesh(geometria, material)
  malha.position.y = -altura / 2
  return malha
}

function terreno(modelo: ModeloMaquete, renderer: THREE.WebGLRenderer): THREE.Mesh {
  const posicoes: number[] = []
  const uvs: number[] = []
  const indices: number[] = []
  const { colunas, linhas, oeste, leste, sul, norte } = modelo

  for (let linha = 0; linha < linhas; linha += 1) {
    const latitude = norte - (linha / (linhas - 1)) * (norte - sul)
    for (let coluna = 0; coluna < colunas; coluna += 1) {
      const u = coluna / (colunas - 1)
      const longitude = oeste + u * (leste - oeste)
      const ponto = modelo.projetar(longitude, latitude)
      posicoes.push(ponto.x, modelo.amostrar(longitude, latitude), -ponto.norte)
      uvs.push(u, 1 - linha / (linhas - 1))
    }
  }

  for (let linha = 0; linha < linhas - 1; linha += 1) {
    for (let coluna = 0; coluna < colunas - 1; coluna += 1) {
      const a = linha * colunas + coluna
      const b = a + 1
      const c = a + colunas
      const d = c + 1
      indices.push(a, c, b, b, c, d)
    }
  }

  const geometria = new THREE.BufferGeometry()
  geometria.setAttribute('position', new THREE.Float32BufferAttribute(posicoes, 3))
  geometria.setAttribute('uv', new THREE.Float32BufferAttribute(uvs, 2))
  geometria.setIndex(indices)
  geometria.computeVertexNormals()

  const material = new THREE.MeshStandardMaterial({
    color: modelo.imagem ? 0xffffff : 0xc5d4c8,
    map: texturaDe(modelo.imagem, renderer),
    roughness: 0.92,
    metalness: 0,
    side: THREE.DoubleSide,
  })
  return new THREE.Mesh(geometria, material)
}

function adicionarTalhoes(
  cena: THREE.Scene,
  modelo: ModeloMaquete,
  features: PlotFeature[],
  selectedId: string | null,
): THREE.Mesh[] {
  const volumes: THREE.Mesh[] = []
  const alcance = Math.max(modelo.largura, modelo.profundidade, 80)
  const espessura = Math.max(alcance * 0.02, 8)

  for (const feature of features) {
    const id = feature.properties.talhaoId
    const aneis = feature.geometry.coordinates.flatMap((anel) => {
      const aberto = anelAberto(anel)
      return aberto.length >= 3 ? [aberto] : []
    })
    const externo = aneis[0]
    if (!externo) continue

    const forma = new THREE.Shape()
    externo.forEach(([longitude, latitude], indice) => {
      const ponto = modelo.projetar(longitude, latitude)
      if (indice === 0) forma.moveTo(ponto.x, ponto.norte)
      else forma.lineTo(ponto.x, ponto.norte)
    })
    for (const furo of aneis.slice(1)) {
      const caminho = new THREE.Path()
      furo.forEach(([longitude, latitude], indice) => {
        const ponto = modelo.projetar(longitude, latitude)
        if (indice === 0) caminho.moveTo(ponto.x, ponto.norte)
        else caminho.lineTo(ponto.x, ponto.norte)
      })
      forma.holes.push(caminho)
    }

    const estado = (feature.properties.estado ?? 'MONITORAMENTO') as EstadoTalhao
    const cor = new THREE.Color(corEstado[estado] ?? '#0F291E')
    const selecionado = id != null && id === selectedId
    const geometria = new THREE.ExtrudeGeometry(forma, { depth: espessura, bevelEnabled: false })
    const material = new THREE.MeshStandardMaterial({
      color: cor,
      roughness: 0.58,
      metalness: 0,
      emissive: selecionado ? cor : 0x000000,
      emissiveIntensity: selecionado ? 0.45 : 0,
    })
    const malha = new THREE.Mesh(geometria, material)
    malha.rotation.x = -Math.PI / 2
    const piso = Math.max(...externo.map(([longitude, latitude]) => modelo.amostrar(longitude, latitude)))
    malha.position.y = piso + 1.5
    if (id) malha.userData.talhaoId = id
    cena.add(malha)
    volumes.push(malha)

    const pontos = externo.map(([longitude, latitude]) => {
      const ponto = modelo.projetar(longitude, latitude)
      return new THREE.Vector3(ponto.x, piso + espessura + 2, -ponto.norte)
    })
    const primeiro = pontos[0]
    if (primeiro) pontos.push(primeiro.clone())
    cena.add(new THREE.Line(new THREE.BufferGeometry().setFromPoints(pontos), new THREE.LineBasicMaterial({ color: 0x0f291e })))

    if (features.length <= 24 && feature.properties.nome) {
      const centro = centroDoAnel(externo)
      const ponto = modelo.projetar(centro[0], centro[1])
      const letreiro = rotulo(feature.properties.nome)
      letreiro.position.set(ponto.x, piso + espessura + alcance * 0.06, -ponto.norte)
      letreiro.scale.set(alcance * 0.2, alcance * 0.05, 1)
      cena.add(letreiro)
    }
  }

  return volumes
}

function anelAberto(anel: PlotFeature['geometry']['coordinates'][number]): Array<[number, number]> {
  const pontos = anel.flatMap((posicao) => {
    const longitude = posicao[0]
    const latitude = posicao[1]
    if (longitude == null || latitude == null || !Number.isFinite(longitude) || !Number.isFinite(latitude)) return []
    return [[longitude, latitude] as [number, number]]
  })
  const primeiro = pontos[0]
  const ultimo = pontos[pontos.length - 1]
  if (primeiro && ultimo && primeiro[0] === ultimo[0] && primeiro[1] === ultimo[1]) pontos.pop()
  return pontos
}

function centroDoAnel(anel: Array<[number, number]>): [number, number] {
  const soma = anel.reduce((total, ponto) => [total[0] + ponto[0], total[1] + ponto[1]] as [number, number], [0, 0] as [number, number])
  return [soma[0] / anel.length, soma[1] / anel.length]
}

function rotulo(texto: string): THREE.Sprite {
  const tela = document.createElement('canvas')
  tela.width = 512
  tela.height = 128
  const contexto = tela.getContext('2d')
  if (contexto) {
    contexto.fillStyle = '#f6f8f3'
    contexto.fillRect(8, 28, 496, 72)
    contexto.strokeStyle = '#1a2216'
    contexto.lineWidth = 4
    contexto.strokeRect(8, 28, 496, 72)
    contexto.fillStyle = '#1a2216'
    contexto.font = '600 42px Inter, sans-serif'
    contexto.textAlign = 'center'
    contexto.textBaseline = 'middle'
    contexto.fillText(texto, 256, 64, 460)
  }
  const textura = new THREE.CanvasTexture(tela)
  textura.colorSpace = THREE.SRGBColorSpace
  const sprite = new THREE.Sprite(new THREE.SpriteMaterial({ map: textura, transparent: true, depthTest: false }))
  return sprite
}

function texturaDe(imagem: HTMLCanvasElement | null, renderer: THREE.WebGLRenderer): THREE.Texture | null {
  if (!imagem) return null
  const textura = new THREE.Texture(imagem)
  textura.colorSpace = THREE.SRGBColorSpace
  textura.anisotropy = renderer.capabilities.getMaxAnisotropy()
  textura.needsUpdate = true
  return textura
}

function descartar(cena: THREE.Scene) {
  cena.traverse((objeto) => {
    const malha = objeto as THREE.Mesh
    malha.geometry?.dispose()
    const material = malha.material
    if (Array.isArray(material)) material.forEach(descartarMaterial)
    else if (material) descartarMaterial(material)
  })
}

function descartarMaterial(material: THREE.Material) {
  if ('map' in material && material.map instanceof THREE.Texture) material.map.dispose()
  material.dispose()
}
