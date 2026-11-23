import { useEffect, useRef, useState, type ReactNode } from 'react'
import { Mic, Square } from 'lucide-react'
import { audioParaWav } from '@/features/diagnosis/wav.ts'
import { Button } from '@/shared/components/ui/button.tsx'

const limiteSegundos = 120

export function GravadorAudio({
  arquivo,
  onArquivo,
  onGravando,
  desabilitado,
  children,
}: {
  arquivo: File | null
  onArquivo: (arquivo: File | null) => void
  onGravando: (gravando: boolean) => void
  desabilitado: boolean
  children?: ReactNode
}) {
  const gravador = useRef<MediaRecorder | null>(null)
  const fluxo = useRef<MediaStream | null>(null)
  const pedacos = useRef<Blob[]>([])
  const limite = useRef<number | null>(null)
  const ativo = useRef(true)
  const ouvirRef = useRef('')
  const [gravando, setGravando] = useState(false)
  const [preparando, setPreparando] = useState(false)
  const [inicio, setInicio] = useState<number | null>(null)
  const [agora, setAgora] = useState(0)
  const [erro, setErro] = useState('')
  const [fonte, setFonte] = useState(arquivo)
  const [ouvir, setOuvir] = useState('')

  if (arquivo !== fonte) {
    setFonte(arquivo)
    if (!arquivo && ouvir) {
      URL.revokeObjectURL(ouvir)
      ouvirRef.current = ''
      setOuvir('')
    }
  }

  useEffect(() => {
    ativo.current = true
    return () => {
      ativo.current = false
      window.clearTimeout(limite.current ?? undefined)
      if (gravador.current?.state === 'recording') gravador.current.stop()
      fluxo.current?.getTracks().forEach((faixa) => faixa.stop())
      if (ouvirRef.current) URL.revokeObjectURL(ouvirRef.current)
    }
  }, [])

  useEffect(() => {
    if (inicio == null) return
    const relogio = window.setInterval(() => setAgora(Date.now()), 250)
    return () => window.clearInterval(relogio)
  }, [inicio])

  async function iniciar() {
    setErro('')
    if (!navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === 'undefined') {
      setErro('Este navegador não grava áudio.')
      return
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      fluxo.current = stream
      const mime = tipoSuportado()
      const recorder = new MediaRecorder(stream, mime ? { mimeType: mime } : undefined)
      pedacos.current = []
      recorder.ondataavailable = (evento) => {
        if (evento.data.size > 0) pedacos.current.push(evento.data)
      }
      recorder.onstop = () => {
        stream.getTracks().forEach((faixa) => faixa.stop())
        fluxo.current = null
        const blob = new Blob(pedacos.current, { type: recorder.mimeType || mime || 'audio/webm' })
        void concluir(blob)
      }
      gravador.current = recorder
      onArquivo(null)
      recorder.start()
      const marco = Date.now()
      setInicio(marco)
      setAgora(marco)
      setGravando(true)
      onGravando(true)
      limite.current = window.setTimeout(() => parar(), limiteSegundos * 1000)
    } catch (falha) {
      fluxo.current?.getTracks().forEach((faixa) => faixa.stop())
      fluxo.current = null
      onGravando(false)
      setErro(mensagemDoMicrofone(falha))
    }
  }

  function parar() {
    window.clearTimeout(limite.current ?? undefined)
    limite.current = null
    if (gravador.current?.state === 'recording') gravador.current.stop()
    setGravando(false)
    setInicio(null)
  }

  async function concluir(blob: Blob) {
    if (!ativo.current) return
    setPreparando(true)
    try {
      const wav = await audioParaWav(blob)
      if (!ativo.current) return
      if (ouvirRef.current) URL.revokeObjectURL(ouvirRef.current)
      const criado = URL.createObjectURL(wav)
      ouvirRef.current = criado
      setOuvir(criado)
      onArquivo(wav)
    } catch {
      if (ativo.current) {
        onArquivo(null)
        setErro('Não consegui ler essa gravação. Grave de novo.')
      }
    } finally {
      if (ativo.current) {
        setPreparando(false)
        onGravando(false)
      }
    }
  }

  function descartar() {
    onArquivo(null)
    setErro('')
  }

  const segundos = inicio == null ? 0 : Math.min(limiteSegundos, Math.floor((agora - inicio) / 1000))
  const relogio = `${Math.floor(segundos / 60)}:${String(segundos % 60).padStart(2, '0')}`

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center gap-2">
        {children && <div className="min-w-0 flex-1">{children}</div>}
        {gravando ? (
          <Button type="button" className="shrink-0" onClick={parar} disabled={desabilitado}>
            <Square aria-hidden="true" className="mr-2 size-4 fill-current" />
            Parar
          </Button>
        ) : (
          <Button type="button" className="shrink-0" onClick={() => void iniciar()} disabled={desabilitado || preparando}>
            <Mic aria-hidden="true" className="mr-2 size-4" />
            {preparando ? 'Preparando áudio…' : 'Gravar áudio'}
          </Button>
        )}
        {gravando && (
          <span className="text-base text-tinta tabular-nums" aria-live="polite">
            {relogio} / 2:00
          </span>
        )}
        {ouvir && !gravando && (
          <Button type="button" variant="quiet" onClick={descartar} disabled={desabilitado || preparando}>
            Descartar áudio
          </Button>
        )}
      </div>
      {ouvir && <audio controls src={ouvir} className="w-full" preload="metadata" />}
      {erro && <p className="text-base text-perigo">{erro}</p>}
    </div>
  )
}

function tipoSuportado(): string {
  const tipos = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4']
  return tipos.find((tipo) => MediaRecorder.isTypeSupported(tipo)) ?? ''
}

function mensagemDoMicrofone(falha: unknown): string {
  if (falha instanceof DOMException && falha.name === 'NotAllowedError') {
    return 'O navegador bloqueou o microfone. Permita o acesso para gravar o relato.'
  }
  if (falha instanceof DOMException && falha.name === 'NotFoundError') {
    return 'Nenhum microfone foi encontrado.'
  }
  return 'Não foi possível abrir o microfone.'
}
