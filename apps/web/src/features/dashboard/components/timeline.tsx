import type { EventoLinha } from '@/features/dashboard/timeline.ts'
import { dataCurta, rotuloAtividade, rotuloCategoria } from '@/shared/lib/format.ts'
import { cn } from '@/shared/lib/cn.ts'

const situacoes: Record<string, string> = {
  REGISTRADA: 'Registrada',
  SEM_TALHAO: 'Sem talhão',
  IGNORADA: 'Sem registro de campo',
  FALHOU: 'A leitura falhou',
}

const faltas: Record<string, string> = {
  nomeTalhao: 'talhão',
  data: 'data',
  insumos: 'insumos',
  custo: 'custo',
  descricao: 'descrição',
}

export function Timeline({
  eventos,
  talhaoId,
  carregando,
  erro,
  vazio,
  onSelecionar,
}: {
  eventos: EventoLinha[]
  talhaoId: string | null
  carregando: boolean
  erro: string
  vazio: string
  onSelecionar: (talhaoId: string) => void
}) {
  if (carregando) return <p className="py-6 text-tinta">Carregando a linha do tempo…</p>
  if (erro) return <p className="py-6 text-perigo">{erro}</p>
  if (eventos.length === 0) return <p className="py-6 text-tinta">{vazio}</p>

  return (
    <ol className="mt-2 border-l border-linha">
      {eventos.map((evento) => (
        <li key={evento.id} className="relative pl-4">
          <span aria-hidden="true" className="absolute top-5 -left-[5px] size-2 rounded-full bg-sol" />
          {evento.kind === 'mensagem' ? (
            <MensagemItem
              evento={evento}
              ativo={talhaoId != null && evento.mensagem.talhaoId === talhaoId}
              onSelecionar={onSelecionar}
            />
          ) : (
            <RegistroItem evento={evento} ativo={talhaoId === talhaoDo(evento)} onSelecionar={onSelecionar} />
          )}
        </li>
      ))}
    </ol>
  )
}

function RegistroItem({
  evento,
  ativo,
  onSelecionar,
}: {
  evento: Extract<EventoLinha, { kind: 'atividade' | 'ocorrencia' }>
  ativo: boolean
  onSelecionar: (talhaoId: string) => void
}) {
  const talhaoId = talhaoDo(evento)
  const titulo = evento.kind === 'atividade' ? rotuloAtividade(evento.atividade.tipo) : rotuloCategoria(evento.ocorrencia.categoria)
  const detalhe = evento.kind === 'atividade' ? evento.atividade.descricao : evento.ocorrencia.descricao
  const quando = evento.kind === 'atividade' ? evento.atividade.dataAtividade : evento.ocorrencia.dataObservacao
  const extra = evento.kind === 'ocorrencia' ? severidade(evento.ocorrencia.severidade) : insumos(evento.atividade.insumos, evento.atividade.custo)

  return (
    <button
      type="button"
      className={cn('flex w-full flex-col gap-1 py-3 text-left', ativo && 'bg-claridade')}
      onClick={() => onSelecionar(talhaoId)}
    >
      <span className="text-base text-tinta">
        {dataCurta(quando)} · {evento.nomeTalhao}
      </span>
      <span className="text-base font-medium">{titulo}</span>
      {detalhe && <span className="text-tinta">{detalhe}</span>}
      {extra && <span className="text-base text-tinta">{extra}</span>}
    </button>
  )
}

function MensagemItem({
  evento,
  ativo,
  onSelecionar,
}: {
  evento: Extract<EventoLinha, { kind: 'mensagem' }>
  ativo: boolean
  onSelecionar: (talhaoId: string) => void
}) {
  const mensagem = evento.mensagem
  const leitura = mensagem.interpretacao
  const resposta = mensagem.resposta || leitura?.resumo || 'A mensagem chegou sem uma leitura.'
  const origem = mensagem.transcricao || mensagem.texto
  const faltantes = (leitura?.camposFaltantes ?? []).map((campo) => faltas[campo] ?? campo)
  const confianca = leitura ? Math.round(leitura.confianca * 100) : null

  const corpo = (
    <>
      <span className="text-base text-tinta">
        {dataCurta(mensagem.criadaEm)} · {mensagem.canal}
        {mensagem.nomeTalhao ? ` · ${mensagem.nomeTalhao}` : ''}
      </span>
      <span className="mt-1 block text-base font-medium">{resposta}</span>
      {origem && <span className="mt-2 block text-tinta">{origem}</span>}
      <span className="mt-2 block text-base text-tinta">
        {situacoes[mensagem.situacao]}
        {confianca != null ? ` · ${confianca}% de confiança` : ''}
      </span>
      {faltantes.length > 0 && (
        <span className="mt-1 block text-base text-tinta">Falta completar: {faltantes.join(', ')}.</span>
      )}
    </>
  )

  if (!mensagem.talhaoId) {
    return <article className={cn('my-2 border border-linha px-3 py-3', ativo && 'bg-claridade')}>{corpo}</article>
  }

  return (
    <button
      type="button"
      className={cn('my-2 w-full border border-linha px-3 py-3 text-left', ativo && 'bg-claridade')}
      onClick={() => onSelecionar(mensagem.talhaoId!)}
    >
      {corpo}
    </button>
  )
}

function talhaoDo(evento: Extract<EventoLinha, { kind: 'atividade' | 'ocorrencia' }>): string {
  return evento.kind === 'atividade' ? evento.atividade.talhaoId : evento.ocorrencia.talhaoId
}

function severidade(valor: string): string {
  if (valor === 'ALTA') return 'Severidade alta'
  if (valor === 'MEDIA') return 'Severidade média'
  return 'Severidade baixa'
}

function insumos(texto: string | null, custo: number | null): string {
  const partes = [texto, custo == null ? null : moeda(custo)].filter(Boolean)
  return partes.join(' · ')
}

function moeda(valor: number): string {
  return valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' })
}
