import { useEffect, useReducer, useState } from 'react'
import { useMutation, useQueries, useQuery, useQueryClient } from '@tanstack/react-query'
import { useSearchParams } from 'react-router'
import { criarAtividade, listarAtividades } from '@/features/activities/api.ts'
import type { AtividadeRequest, OcorrenciaRequest } from '@/features/activities/types/index.ts'
import { criarOcorrencia, enviarMensagem, listarMensagens, listarOcorrencias } from '@/features/diagnosis/api.ts'
import { listarFazendas } from '@/features/farms/api.ts'
import { CreateFarmModal } from '@/features/farms/components/create-farm-modal.tsx'
import type { Fazenda, TalhaoRequest } from '@/features/farms/types/index.ts'
import { criarTalhao, listarTalhoes } from '@/features/plots/api.ts'
import { estadoDoTalhao } from '@/features/plots/status.ts'
import { buildPolygonWkt } from '@/features/maps/wkt.ts'
import { areaDaFazenda, montarFeatures } from '@/features/dashboard/geo.ts'
import { estadoInicial, reduzir } from '@/features/dashboard/state.ts'
import { montarLinha } from '@/features/dashboard/timeline.ts'
import { FarmHeader } from '@/features/dashboard/components/farm-header.tsx'
import { MapPane } from '@/features/dashboard/components/map-pane.tsx'
import { RegisterSheet } from '@/features/dashboard/components/register-sheet.tsx'
import { Timeline } from '@/features/dashboard/components/timeline.tsx'
import { ApiError } from '@/shared/lib/api.ts'
import { hectares } from '@/shared/lib/format.ts'
import { cn } from '@/shared/lib/cn.ts'
import { Button } from '@/shared/components/ui/button.tsx'

export function DashboardPage() {
  const queryClient = useQueryClient()
  const [params, setParams] = useSearchParams()
  const pedida = params.get('fazenda')
  const wide = useWide()
  const [state, dispatch] = useReducer(reduzir, estadoInicial)
  const [novaFazenda, setNovaFazenda] = useState(false)
  const [erroAcao, setErroAcao] = useState('')
  const [painelAberto, setPainelAberto] = useState(false)
  const fazendas = useQuery({ queryKey: ['fazendas'], queryFn: listarFazendas })

  useEffect(() => {
    if (!fazendas.data?.length) return
    if (pedida && fazendas.data.some((item) => item.id === pedida)) return
    setParams({ fazenda: fazendas.data[0].id }, { replace: true })
  }, [fazendas.data, pedida, setParams])

  const fazenda = fazendas.data?.find((item) => item.id === pedida) ?? null
  const talhoes = useQuery({
    queryKey: ['talhoes', fazenda?.id],
    queryFn: () => listarTalhoes(fazenda!.id),
    enabled: Boolean(fazenda),
  })
  const lista = talhoes.data ?? []
  const ocorrencias = useQueries({
    queries: lista.map((talhao) => ({
      queryKey: ['ocorrencias', talhao.id],
      queryFn: () => listarOcorrencias(talhao.id),
    })),
  })
  const atividades = useQueries({
    queries: lista.map((talhao) => ({
      queryKey: ['atividades', talhao.id],
      queryFn: () => listarAtividades(talhao.id),
    })),
  })
  const mensagens = useQuery({
    queryKey: ['mensagens'],
    queryFn: listarMensagens,
    enabled: Boolean(fazenda),
  })

  const criar = useMutation({
    mutationFn: (body: TalhaoRequest) => {
      if (!fazenda) throw new Error('Escolha a fazenda antes de marcar o talhão.')
      return criarTalhao(fazenda.id, body)
    },
    onSuccess: async (talhao) => {
      await queryClient.invalidateQueries({ queryKey: ['talhoes', fazenda?.id] })
      setErroAcao('')
      revelar(talhao.id)
      dispatch({ type: 'salvo', talhaoId: talhao.id })
      if (!wide) dispatch({ type: 'ver-feed' })
    },
    onError: (error) => {
      setErroAcao(error instanceof ApiError ? error.message : 'Não foi possível criar o talhão.')
    },
  })

  const atividade = useMutation({
    mutationFn: ({ talhaoId, body }: { talhaoId: string; body: AtividadeRequest }) => criarAtividade(talhaoId, body),
    onSuccess: async (_item, { talhaoId }) => {
      await queryClient.invalidateQueries({ queryKey: ['atividades', talhaoId] })
      setErroAcao('')
      dispatch({ type: 'salvo' })
    },
    onError: (error) => {
      setErroAcao(error instanceof ApiError ? error.message : 'Não foi possível salvar a atividade.')
    },
  })

  const ocorrencia = useMutation({
    mutationFn: ({ talhaoId, body }: { talhaoId: string; body: OcorrenciaRequest }) => criarOcorrencia(talhaoId, body),
    onSuccess: async (_item, { talhaoId }) => {
      await queryClient.invalidateQueries({ queryKey: ['ocorrencias', talhaoId] })
      setErroAcao('')
      dispatch({ type: 'salvo' })
    },
    onError: (error) => {
      setErroAcao(error instanceof ApiError ? error.message : 'Não foi possível salvar a ocorrência.')
    },
  })

  const relato = useMutation({
    mutationFn: (envio: { texto: string; arquivo: File | null }) =>
      enviarMensagem({ texto: envio.texto || null, arquivo: envio.arquivo }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['mensagens'] })
      await queryClient.invalidateQueries({ queryKey: ['atividades'] })
      await queryClient.invalidateQueries({ queryKey: ['ocorrencias'] })
      setErroAcao('')
      dispatch({ type: 'salvo' })
    },
    onError: (error) => {
      setErroAcao(error instanceof ApiError ? error.message : 'Não foi possível enviar o relato.')
    },
  })

  function revelar(talhaoId: string) {
    dispatch({ type: 'selecionar', talhaoId })
    setPainelAberto(false)
    requestAnimationFrame(() => setPainelAberto(true))
  }

  function cancelar() {
    setErroAcao('')
    dispatch({ type: 'cancelar' })
  }

  if (fazendas.isPending) return <p className="p-6 text-lg">Carregando fazendas…</p>

  if (fazendas.isError) {
    const mensagem = fazendas.error instanceof ApiError ? fazendas.error.message : 'Não foi possível carregar as fazendas.'
    return (
      <div className="p-6">
        <p className="text-lg">{mensagem}</p>
        <Button className="mt-4" onClick={() => fazendas.refetch()}>
          Tentar de novo
        </Button>
      </div>
    )
  }

  if (!fazendas.data.length) {
    return (
      <div className="p-6">
        <h1 className="text-4xl">Fazendas</h1>
        <p className="mt-4 text-lg text-tinta">Nenhuma fazenda cadastrada.</p>
        <Button className="mt-4" onClick={() => setNovaFazenda(true)}>
          Criar fazenda
        </Button>
        <CreateFarmModal
          open={novaFazenda}
          onClose={() => setNovaFazenda(false)}
          onCreated={(criada) => {
            setNovaFazenda(false)
            setParams({ fazenda: criada.id })
          }}
        />
      </div>
    )
  }

  if (!fazenda) return <p className="p-6 text-lg">Carregando fazendas…</p>

  const estados = lista.map((talhao, index) =>
    estadoDoTalhao(talhao, ocorrencias[index]?.data ?? [], atividades[index]?.data ?? []),
  )
  const features = montarFeatures(fazenda.nome, lista, estados)
  const fitKey = `${fazenda.id}:${features.map((item) => item.properties.talhaoId).join(',')}`
  const interacao = state.interacao
  const desenhando = interacao.etapa === 'desenhando'
  const nomeando = interacao.etapa === 'nomeando'
  const pontos = interacao.etapa === 'desenhando' || interacao.etapa === 'nomeando' ? interacao.pontos : []
  const vista = wide ? 'split' : state.vista
  const mapaVisivel = wide || state.vista === 'mapa'
  const escolhido = lista.find((item) => item.id === state.talhaoId) ?? null
  const eventos = montarLinha(
    lista,
    atividades.map((query) => query.data ?? []),
    ocorrencias.map((query) => query.data ?? []),
    mensagens.data ?? [],
    state.talhaoId,
  )
  const carregandoLinha = talhoes.isPending || mensagens.isPending || atividades.some((query) => query.isPending) || ocorrencias.some((query) => query.isPending)
  const erroLinha = mensagemDe(talhoes.error) || mensagemDe(mensagens.error) || mensagemDe(atividades.find((query) => query.error)?.error) || mensagemDe(ocorrencias.find((query) => query.error)?.error)
  const alertas = ocorrencias.reduce(
    (total, query) => total + (query.data?.filter((item) => item.situacao === 'ABERTA').length ?? 0),
    0,
  )
  const ativos = lista.filter((item) => item.situacao === 'ATIVO').length
  const enviando = atividade.isPending || ocorrencia.isPending || relato.isPending
  const vazio = state.talhaoId
    ? 'Nada registrado neste talhão.'
    : lista.length === 0
      ? 'Esta fazenda ainda não tem talhão. Desenhe o primeiro no mapa.'
      : 'Nada registrado ainda nesta fazenda.'

  function fecharPoligono() {
    if (pontos.length < 3) {
      setErroAcao('Marque pelo menos três cantos.')
      return
    }
    setErroAcao('')
    dispatch({ type: 'fechar-poligono' })
  }

  function salvarTalhao(body: Omit<TalhaoRequest, 'limiteWkt'>) {
    try {
      criar.mutate({ ...body, limiteWkt: buildPolygonWkt(pontos) })
    } catch (error) {
      setErroAcao(error instanceof Error ? error.message : 'Marque pelo menos três cantos.')
    }
  }

  return (
    <div className="flex h-full min-h-0 flex-col">
      <div className="bg-cerca px-5 py-6 lg:mx-6 lg:mt-6 lg:rounded-controle">
        <FarmHeader
          fazenda={fazenda}
          fazendas={fazendas.data}
          area={areaDaFazenda(fazenda.areaTotalHectares, lista)}
          ativos={ativos}
          alertas={alertas}
          desenhando={desenhando || nomeando}
          onFazenda={(id) => {
            setErroAcao('')
            dispatch({ type: 'trocar-fazenda' })
            setParams({ fazenda: id })
          }}
          onAdicionar={() => setNovaFazenda(true)}
          onRegistrar={() => {
            setErroAcao('')
            if (!wide) dispatch({ type: 'ver-feed' })
            dispatch({ type: 'abrir-registro' })
          }}
          onDesenhar={() => {
            setErroAcao('')
            if (!wide) dispatch({ type: 'ver-mapa' })
            dispatch({ type: 'iniciar-desenho' })
          }}
        />
      </div>
      <div className="flex gap-2 p-3 lg:hidden">
        <Aba ativa={state.vista === 'feed'} onClick={() => dispatch({ type: 'ver-feed' })}>
          Registros
        </Aba>
        <Aba ativa={state.vista === 'mapa'} onClick={() => dispatch({ type: 'ver-mapa' })}>
          Mapa
        </Aba>
      </div>
      <div className="grid min-h-0 flex-1 lg:grid-cols-[minmax(0,2fr)_minmax(0,3fr)] lg:gap-6 lg:p-6">
        <section
          aria-label="Feed da fazenda"
          data-vista={wide ? undefined : state.vista}
          className={cn(
            't-vista min-h-0 overflow-auto bg-cerca px-5 py-6 [scrollbar-color:#3f4e3c_#e7efd8] lg:rounded-controle',
            vista === 'mapa' && 'hidden',
          )}
        >
          {escolhido && (
            <div className="t-panel-reveal mt-4" data-open={painelAberto ? 'true' : 'false'}>
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h2 className="text-lg">{escolhido.nome}</h2>
                  <p className="text-tinta">
                    {escolhido.cultura ?? 'Sem cultura'} · {hectares(escolhido.areaHectares)}
                  </p>
                </div>
                <Button variant="quiet" onClick={() => dispatch({ type: 'limpar' })}>
                  Fazenda inteira
                </Button>
              </div>
            </div>
          )}
          {(interacao.etapa === 'escolhendo' || interacao.etapa === 'escolhendo-talhao' || interacao.etapa === 'preenchendo') && (
            <RegisterSheet
              etapa={interacao.etapa}
              preenchimento={interacao.etapa === 'preenchendo' ? interacao.registro : null}
              talhaoId={interacao.etapa === 'preenchendo' ? interacao.talhaoId : state.talhaoId}
              talhoes={lista}
              enviando={enviando}
              erro={erroAcao}
              onEscolher={(registro) => {
                setErroAcao('')
                dispatch({ type: 'escolher-registro', registro })
              }}
              onTalhao={(talhaoId) => {
                setErroAcao('')
                revelar(talhaoId)
                dispatch({ type: 'escolher-talhao', talhaoId })
              }}
              onAtividade={(body) => {
                if (interacao.etapa !== 'preenchendo' || !interacao.talhaoId) return
                atividade.mutate({ talhaoId: interacao.talhaoId, body })
              }}
              onOcorrencia={(body) => {
                if (interacao.etapa !== 'preenchendo' || !interacao.talhaoId) return
                ocorrencia.mutate({ talhaoId: interacao.talhaoId, body })
              }}
              onMensagem={(texto, arquivo) => relato.mutate({ texto, arquivo })}
              onCancelar={cancelar}
            />
          )}
          <Timeline
            eventos={eventos}
            talhaoId={state.talhaoId}
            carregando={carregandoLinha}
            erro={erroLinha}
            vazio={vazio}
            onSelecionar={(talhaoId) => {
              revelar(talhaoId)
              if (!wide) dispatch({ type: 'ver-feed' })
            }}
          />
        </section>
        <div
          data-vista={wide ? undefined : state.vista}
          className={cn('t-vista min-h-0 lg:overflow-hidden lg:rounded-controle', vista === 'feed' && 'hidden')}
        >
          <MapPane
            features={features}
            selectedId={state.talhaoId}
            modo={state.modo}
            onModo={(modo) => dispatch({ type: 'modo', modo })}
            drawing={desenhando}
            draft={pontos}
            naming={nomeando}
            onSelect={(id) => {
              revelar(id)
              if (!wide) dispatch({ type: 'ver-feed' })
            }}
            onAddVertex={(longitude, latitude) => {
              setErroAcao('')
              dispatch({ type: 'vertice', longitude, latitude })
            }}
            onUndo={() => dispatch({ type: 'desfazer-vertice' })}
            onFinish={fecharPoligono}
            onCancel={cancelar}
            onSave={salvarTalhao}
            saving={criar.isPending}
            erro={erroAcao}
            center={
              fazenda.latitude != null && fazenda.longitude != null
                ? { latitude: fazenda.latitude, longitude: fazenda.longitude }
                : null
            }
            fitKey={fitKey}
            visivel={mapaVisivel}
          />
        </div>
      </div>
      <CreateFarmModal
        open={novaFazenda}
        onClose={() => setNovaFazenda(false)}
        onCreated={(criada: Fazenda) => {
          setNovaFazenda(false)
          setParams({ fazenda: criada.id })
        }}
      />
    </div>
  )
}

function Aba({ ativa, onClick, children }: { ativa: boolean; onClick: () => void; children: string }) {
  return (
    <button
      type="button"
      aria-pressed={ativa}
      className={cn('min-h-12 flex-1 rounded-full text-base font-medium', ativa ? 'bg-chuva text-cerca' : 'text-nevoa')}
      onClick={onClick}
    >
      {children}
    </button>
  )
}

function mensagemDe(error: unknown): string {
  if (!error) return ''
  return error instanceof ApiError ? error.message : 'Não foi possível carregar a linha do tempo.'
}

function useWide() {
  const [wide, setWide] = useState(() => window.matchMedia('(min-width: 1024px)').matches)
  useEffect(() => {
    const media = window.matchMedia('(min-width: 1024px)')
    const atualizar = () => setWide(media.matches)
    media.addEventListener('change', atualizar)
    return () => media.removeEventListener('change', atualizar)
  }, [])
  return wide
}
