import type { Sessao } from '@/shared/types/auth.ts'

const erros: Record<string, string> = {
  ACCOUNT_EMAIL_CONFLICT: 'Já existe uma conta com este e-mail. Entre com a senha. As contas não são vinculadas automaticamente.',
  GOOGLE_EMAIL_UNVERIFIED: 'O Google não confirmou o e-mail. Use uma conta com e-mail verificado.',
  GOOGLE_LOGIN_FAILED: 'O login com Google não foi concluído. Tente novamente.',
}

type MensagemGoogle = {
  type?: string
  ok?: boolean
  error?: string
  data?: unknown
}

export function origemDaApi(): string {
  const configurada = import.meta.env.VITE_API_ORIGIN?.trim()
  if (configurada) return new URL(configurada).origin
  const local = ['localhost', '127.0.0.1', '[::1]'].includes(window.location.hostname)
  if (local) return `${window.location.protocol}//${window.location.hostname}:8080`
  return window.location.origin
}

let clienteAtual: ReturnType<typeof criarLoginGoogle> | null = null

export function loginComGoogle(): Promise<Sessao> {
  clienteAtual ??= criarLoginGoogle(origemDaApi())
  return clienteAtual.login()
}

const LARGURA_MAX = 480
const ALTURA_MAX = 640
const CHROME_POPUP = 88

function coordenada(valor: number, reserva: number) {
  return Number.isFinite(valor) ? valor : reserva
}

function medida(valor: number, reserva: number) {
  return Number.isFinite(valor) && valor > 0 ? valor : reserva
}

function medidasDaJanelaGoogle() {
  const origemX = coordenada(window.screenX, coordenada(window.screenLeft, 0))
  const origemY = coordenada(window.screenY, coordenada(window.screenTop, 0))
  const larguraJanela = medida(window.outerWidth, window.innerWidth)
  const alturaJanela = medida(window.outerHeight, window.innerHeight)
  const larguraTela = medida(window.screen.availWidth, larguraJanela)
  const alturaTela = medida(window.screen.availHeight, alturaJanela)
  const caixaLargura = Math.min(larguraJanela, larguraTela)
  const caixaAltura = Math.min(alturaJanela, alturaTela)
  const largura = Math.round(Math.min(LARGURA_MAX, Math.max(100, caixaLargura - 48)))
  const altura = Math.round(Math.min(ALTURA_MAX, Math.max(100, caixaAltura - CHROME_POPUP - 24)))
  return {
    largura,
    altura,
    esquerda: Math.round(origemX + (larguraJanela - largura) / 2),
    topo: Math.round(origemY + (alturaJanela - altura) / 2),
  }
}

function abrirJanelaGoogle(destino: string): Window | null {
  const { largura, altura, esquerda, topo } = medidasDaJanelaGoogle()
  const janela = window.open(
    'about:blank',
    `nexo-google-${Date.now()}`,
    `left=${esquerda},top=${topo},width=${largura},height=${altura}`,
  )
  if (!janela) return null
  try {
    janela.resizeTo(largura, altura)
    janela.moveTo(esquerda, topo)
  } catch {
    /* O recurso do window.open já pediu a mesma posição. */
  }
  try {
    janela.location.replace(destino)
  } catch {
    janela.close()
    return null
  }
  return janela
}

function criarLoginGoogle(origemApi: string, timeoutMs = 180_000) {
  const url = new URL(origemApi)
  const local = ['localhost', '127.0.0.1', '[::1]'].includes(url.hostname)
  if (url.origin !== origemApi || url.username || url.password
    || !(url.protocol === 'https:' || (url.protocol === 'http:' && local))) {
    throw new Error('A origem da API precisa ser HTTPS, ou HTTP só no localhost.')
  }

  let ativo: { cancelar: () => void } | null = null

  function login(): Promise<Sessao> {
    if (ativo) return Promise.reject(new Error('Já existe um login com Google em andamento.'))
    const aberto = abrirJanelaGoogle(`${origemApi}/oauth2/authorization/google`)
    if (!aberto) return Promise.reject(new Error('O navegador bloqueou a janela. Permita pop-ups para entrar.'))
    const popup: Window = aberto

    return new Promise((resolve, reject) => {
      let intervalo = 0
      let prazo = 0
      let encerrado = false

      function concluir(erro: Error | null, sessao?: Sessao) {
        if (encerrado) return
        encerrado = true
        window.removeEventListener('message', aoMensagem)
        window.clearInterval(intervalo)
        window.clearTimeout(prazo)
        ativo = null
        try {
          popup.close()
        } catch {
          /* A janela já fechou. */
        }
        if (erro) reject(erro)
        else if (sessao) resolve(sessao)
      }

      function aoMensagem(evento: MessageEvent) {
        if (evento.origin !== origemApi || evento.source !== popup) return
        const mensagem = evento.data as MensagemGoogle
        if (!mensagem || mensagem.type !== 'nexo:google-auth') return
        if (mensagem.ok === false) {
          concluir(new Error(erros[mensagem.error ?? ''] ?? erros.GOOGLE_LOGIN_FAILED))
          return
        }
        const sessao = sessaoValida(mensagem.data)
        if (mensagem.ok !== true || !sessao) {
          concluir(new Error('Resposta de autenticação inválida. Tente novamente.'))
          return
        }
        concluir(null, sessao)
      }

      ativo = { cancelar: () => concluir(new Error('Login com Google cancelado.')) }
      window.addEventListener('message', aoMensagem)
      intervalo = window.setInterval(() => {
        if (popup.closed) concluir(new Error('A janela de login foi fechada.'))
      }, 500)
      prazo = window.setTimeout(() => {
        concluir(new Error('O login expirou. Abra novamente a janela do Google.'))
      }, timeoutMs)
    })
  }

  return { login, cancelar: () => ativo?.cancelar() }
}

function sessaoValida(data: unknown): Sessao | null {
  if (!data || typeof data !== 'object') return null
  const valor = data as Partial<Sessao>
  const uuid = /^[\da-f]{8}-(?:[\da-f]{4}-){3}[\da-f]{12}$/i
  if (typeof valor.token !== 'string' || valor.token.length === 0 || valor.token.length > 16_384) return null
  if (typeof valor.usuarioId !== 'string' || !uuid.test(valor.usuarioId)) return null
  if (typeof valor.nome !== 'string' || !valor.nome.trim()) return null
  if (typeof valor.email !== 'string' || !valor.email.trim()) return null
  if (typeof valor.perfil !== 'string' || !valor.perfil.trim()) return null
  return {
    token: valor.token,
    usuarioId: valor.usuarioId,
    nome: valor.nome,
    email: valor.email,
    perfil: valor.perfil,
  }
}
