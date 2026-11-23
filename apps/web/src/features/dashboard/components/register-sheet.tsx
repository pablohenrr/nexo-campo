import { useRef, useState, type FormEvent } from 'react'
import {
  CATEGORIAS_OCORRENCIA,
  SEVERIDADES,
  TIPOS_ATIVIDADE,
  type AtividadeRequest,
  type CategoriaOcorrencia,
  type OcorrenciaRequest,
  type Severidade,
} from '@/features/activities/types/index.ts'
import type { Talhao } from '@/features/farms/types/index.ts'
import type { Registro } from '@/features/dashboard/state.ts'
import { GravadorAudio } from '@/features/diagnosis/components/gravador-audio.tsx'
import { rotuloAtividade, rotuloCategoria } from '@/shared/lib/format.ts'
import { Button } from '@/shared/components/ui/button.tsx'
import { Field, TextInput } from '@/shared/components/ui/field.tsx'

const controle = 'h-12 w-full rounded-controle border border-linha bg-orvalho px-3 text-base text-tinta'

export function RegisterSheet({
  etapa,
  preenchimento,
  talhaoId,
  talhoes,
  enviando,
  erro,
  onEscolher,
  onTalhao,
  onAtividade,
  onOcorrencia,
  onMensagem,
  onCancelar,
}: {
  etapa: 'escolhendo' | 'escolhendo-talhao' | 'preenchendo'
  preenchimento: Registro | null
  talhaoId: string | null
  talhoes: Talhao[]
  enviando: boolean
  erro: string
  onEscolher: (registro: Registro | 'talhao') => void
  onTalhao: (talhaoId: string) => void
  onAtividade: (body: AtividadeRequest) => void
  onOcorrencia: (body: OcorrenciaRequest) => void
  onMensagem: (texto: string, arquivo: File | null) => void
  onCancelar: () => void
}) {
  return (
    <section aria-label="Registrar" className="t-panel-reveal mt-8 rounded-controle border border-linha p-5" data-open="true">
      {etapa === 'escolhendo' && (
        <div className="flex flex-col gap-2">
          <h2 className="mb-2 text-lg">O que você vai registrar?</h2>
          <Button onClick={() => onEscolher('atividade')}>Atividade</Button>
          <Button onClick={() => onEscolher('ocorrencia')}>Ocorrência</Button>
          <Button onClick={() => onEscolher('mensagem')}>Relato de voz ou foto</Button>
          <Button variant="quiet" onClick={() => onEscolher('talhao')}>
            Desenhar talhão no mapa
          </Button>
          <Button variant="quiet" onClick={onCancelar}>
            Cancelar
          </Button>
        </div>
      )}
      {etapa === 'escolhendo-talhao' && (
        <EscolhaTalhao talhoes={talhoes} onTalhao={onTalhao} onDesenhar={() => onEscolher('talhao')} onCancelar={onCancelar} />
      )}
      {etapa === 'preenchendo' && preenchimento === 'atividade' && talhaoId && (
        <AtividadeForm talhao={nome(talhoes, talhaoId)} enviando={enviando} onSubmit={onAtividade} onCancelar={onCancelar} />
      )}
      {etapa === 'preenchendo' && preenchimento === 'ocorrencia' && talhaoId && (
        <OcorrenciaForm talhao={nome(talhoes, talhaoId)} enviando={enviando} onSubmit={onOcorrencia} onCancelar={onCancelar} />
      )}
      {etapa === 'preenchendo' && preenchimento === 'mensagem' && (
        <MensagemForm enviando={enviando} onSubmit={onMensagem} onCancelar={onCancelar} />
      )}
      {erro && <p className="mt-3 text-base text-perigo">{erro}</p>}
    </section>
  )
}

function EscolhaTalhao({
  talhoes,
  onTalhao,
  onDesenhar,
  onCancelar,
}: {
  talhoes: Talhao[]
  onTalhao: (id: string) => void
  onDesenhar: () => void
  onCancelar: () => void
}) {
  return (
    <div>
      <h2 className="mb-3 text-lg">Em qual talhão?</h2>
      {talhoes.length === 0 && <p className="mb-3 text-tinta">Esta fazenda ainda não tem talhão.</p>}
      <ul className="flex flex-col gap-2">
        {talhoes.map((talhao) => (
          <li key={talhao.id}>
            <Button className="w-full" variant="quiet" onClick={() => onTalhao(talhao.id)}>
              {talhao.nome}
            </Button>
          </li>
        ))}
      </ul>
      <div className="mt-3 flex flex-wrap gap-2">
        <Button onClick={onDesenhar}>Desenhar talhão</Button>
        <Button variant="quiet" onClick={onCancelar}>
          Cancelar
        </Button>
      </div>
    </div>
  )
}

function AtividadeForm({
  talhao,
  enviando,
  onSubmit,
  onCancelar,
}: {
  talhao: string
  enviando: boolean
  onSubmit: (body: AtividadeRequest) => void
  onCancelar: () => void
}) {
  const [tipo, setTipo] = useState<(typeof TIPOS_ATIVIDADE)[number]>('MONITORAMENTO')
  const [descricao, setDescricao] = useState('')
  const [data, setData] = useState(hoje())
  const [insumos, setInsumos] = useState('')
  const [custo, setCusto] = useState('')
  const [erro, setErro] = useState('')

  function enviar(event: FormEvent) {
    event.preventDefault()
    const numero = numeroOuNulo(custo)
    if (custo.trim() && numero == null) {
      setErro('O custo precisa ser um número.')
      return
    }
    if (!data) {
      setErro('Informe a data da atividade.')
      return
    }
    setErro('')
    onSubmit({
      tipo,
      descricao: descricao.trim() || null,
      dataAtividade: data,
      custo: numero,
      insumos: insumos.trim() || null,
    })
  }

  return (
    <form onSubmit={enviar} noValidate>
      <h2 className="mb-3 text-lg">Atividade em {talhao}</h2>
      <Field label="Tipo">
        <select className={controle} value={tipo} onChange={(event) => setTipo(event.target.value as typeof tipo)}>
          {TIPOS_ATIVIDADE.map((item) => (
            <option key={item} value={item}>
              {rotuloAtividade(item)}
            </option>
          ))}
        </select>
      </Field>
      <Field label="Data">
        <TextInput type="date" value={data} onChange={(event) => setData(event.target.value)} required />
      </Field>
      <Field label="Descrição">
        <Area value={descricao} onChange={setDescricao} />
      </Field>
      <Field label="Insumos">
        <TextInput value={insumos} onChange={(event) => setInsumos(event.target.value)} maxLength={500} />
      </Field>
      <Field label="Custo em reais">
        <TextInput inputMode="decimal" value={custo} onChange={(event) => setCusto(event.target.value)} />
      </Field>
      {erro && <p className="mb-3 text-base text-perigo">{erro}</p>}
      <Acoes enviando={enviando} rotulo="Salvar atividade" onCancelar={onCancelar} />
    </form>
  )
}

function OcorrenciaForm({
  talhao,
  enviando,
  onSubmit,
  onCancelar,
}: {
  talhao: string
  enviando: boolean
  onSubmit: (body: OcorrenciaRequest) => void
  onCancelar: () => void
}) {
  const [categoria, setCategoria] = useState<CategoriaOcorrencia>('PRAGA')
  const [severidade, setSeveridade] = useState<Severidade>('MEDIA')
  const [descricao, setDescricao] = useState('')
  const [data, setData] = useState(hoje())
  const [erro, setErro] = useState('')

  function enviar(event: FormEvent) {
    event.preventDefault()
    if (!descricao.trim()) {
      setErro('Descreva o que você viu.')
      return
    }
    if (!data) {
      setErro('Informe a data da observação.')
      return
    }
    setErro('')
    onSubmit({
      categoria,
      severidade,
      descricao: descricao.trim(),
      dataObservacao: data,
      evidenciaId: null,
    })
  }

  return (
    <form onSubmit={enviar} noValidate>
      <h2 className="mb-3 text-lg">Ocorrência em {talhao}</h2>
      <Field label="Categoria">
        <select className={controle} value={categoria} onChange={(event) => setCategoria(event.target.value as CategoriaOcorrencia)}>
          {CATEGORIAS_OCORRENCIA.map((item) => (
            <option key={item} value={item}>
              {rotuloCategoria(item)}
            </option>
          ))}
        </select>
      </Field>
      <Field label="Severidade">
        <select className={controle} value={severidade} onChange={(event) => setSeveridade(event.target.value as Severidade)}>
          {SEVERIDADES.map((item) => (
            <option key={item} value={item}>
              {item === 'BAIXA' ? 'Baixa' : item === 'MEDIA' ? 'Média' : 'Alta'}
            </option>
          ))}
        </select>
      </Field>
      <Field label="Data">
        <TextInput type="date" value={data} onChange={(event) => setData(event.target.value)} required />
      </Field>
      <Field label="Descrição">
        <Area value={descricao} onChange={setDescricao} />
      </Field>
      {erro && <p className="mb-3 text-base text-perigo">{erro}</p>}
      <Acoes enviando={enviando} rotulo="Salvar ocorrência" onCancelar={onCancelar} />
    </form>
  )
}

function MensagemForm({
  enviando,
  onSubmit,
  onCancelar,
}: {
  enviando: boolean
  onSubmit: (texto: string, arquivo: File | null) => void
  onCancelar: () => void
}) {
  const foto = useRef<HTMLInputElement>(null)
  const [texto, setTexto] = useState('')
  const [arquivo, setArquivo] = useState<File | null>(null)
  const [gravando, setGravando] = useState(false)
  const [erro, setErro] = useState('')
  const audio = arquivo?.type.startsWith('audio/') ? arquivo : null

  function enviar(event: FormEvent) {
    event.preventDefault()
    if (gravando) {
      setErro('Pare a gravação antes de enviar.')
      return
    }
    if (!texto.trim() && !arquivo) {
      setErro('Escreva o relato, grave um áudio ou escolha uma foto.')
      return
    }
    setErro('')
    onSubmit(texto.trim(), arquivo)
  }

  function escolherFoto(file: File | null) {
    setErro('')
    setArquivo(file)
  }

  function escolherAudio(file: File | null) {
    if (foto.current) foto.current.value = ''
    setErro('')
    setArquivo(file)
  }

  return (
    <form onSubmit={enviar} noValidate>
      <h2 className="mb-3 text-lg">Relato para a fazenda</h2>
      <Field label="Texto">
        <Area value={texto} onChange={setTexto} />
      </Field>
      <Field label="Foto ou áudio">
        <GravadorAudio
          arquivo={audio}
          onArquivo={escolherAudio}
          onGravando={setGravando}
          desabilitado={enviando}
        >
          <input
            ref={foto}
            className="block w-full text-base text-tinta file:mr-3 file:min-h-12 file:rounded-controle file:border file:border-linha file:bg-orvalho file:px-3"
            type="file"
            accept="image/*"
            disabled={enviando || gravando}
            onChange={(event) => {
              const file = event.target.files?.[0] ?? null
              if (file && !file.type.startsWith('image/')) {
                event.target.value = ''
                setErro('Escolha uma foto.')
                return
              }
              escolherFoto(file)
            }}
          />
        </GravadorAudio>
      </Field>
      {erro && <p className="mb-3 text-base text-perigo">{erro}</p>}
      <Acoes enviando={enviando} bloqueado={gravando} rotulo="Enviar relato" onCancelar={onCancelar} />
    </form>
  )
}

function Acoes({
  enviando,
  bloqueado = false,
  rotulo,
  onCancelar,
}: {
  enviando: boolean
  bloqueado?: boolean
  rotulo: string
  onCancelar: () => void
}) {
  return (
    <div className="flex flex-wrap gap-2">
      <Button type="submit" disabled={enviando || bloqueado}>
        {enviando ? 'Salvando…' : rotulo}
      </Button>
      <Button type="button" variant="quiet" onClick={onCancelar} disabled={enviando}>
        Cancelar
      </Button>
    </div>
  )
}

function Area({ value, onChange }: { value: string; onChange: (valor: string) => void }) {
  return (
    <textarea
      className="min-h-24 w-full rounded-controle border border-linha bg-orvalho px-3 py-2 text-base text-tinta"
      value={value}
      maxLength={1000}
      onChange={(event) => onChange(event.target.value)}
    />
  )
}

function nome(talhoes: Talhao[], id: string): string {
  return talhoes.find((item) => item.id === id)?.nome ?? 'talhão'
}

function numeroOuNulo(texto: string): number | null {
  const limpo = texto.trim().replace(',', '.')
  if (!limpo) return null
  const numero = Number(limpo)
  return Number.isFinite(numero) ? numero : null
}

function hoje(): string {
  const agora = new Date()
  const local = new Date(agora.getTime() - agora.getTimezoneOffset() * 60_000)
  return local.toISOString().slice(0, 10)
}
