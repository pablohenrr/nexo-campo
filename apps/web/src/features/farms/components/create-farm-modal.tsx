import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Search, Trash2, Undo2 } from 'lucide-react'
import { atualizarFazenda, criarFazenda } from '@/features/farms/api.ts'
import { buscaDisponivel, buscarEndereco, type ResultadoEndereco } from '@/features/farms/geocoding.ts'
import { FarmOutlineMap, type Destino } from '@/features/farms/components/farm-outline-map.tsx'
import type { Coordenada, Fazenda, FazendaRequest } from '@/features/farms/types/index.ts'
import { ApiError } from '@/shared/lib/api.ts'
import { Button } from '@/shared/components/ui/button.tsx'
import { Field, TextInput } from '@/shared/components/ui/field.tsx'
import { Modal } from '@/shared/components/ui/modal.tsx'

const minimoPinos = 3

/**
 * Cadastro e edição da fazenda. O contorno marcado com pinos é obrigatório: o backend calcula
 * o centro e a área a partir dele. A busca de endereço só move o mapa.
 */
export function CreateFarmModal({
  open,
  onClose,
  onCreated,
  fazenda,
}: {
  open: boolean
  onClose: () => void
  onCreated: (fazenda: Fazenda) => void
  /** Quando informada, a tela edita esta fazenda em vez de criar uma nova. */
  fazenda?: Fazenda
}) {
  return (
    <Modal
      open={open}
      title={fazenda ? 'Editar fazenda' : 'Nova fazenda'}
      className="!w-[min(64rem,calc(100%-2rem))] max-h-[calc(100dvh-2rem)]"
      onClose={onClose}
    >
      {/* Remonta a cada abertura para começar do estado salvo. */}
      {open && <Formulario key={fazenda?.id ?? 'nova'} fazenda={fazenda} onClose={onClose} onSaved={onCreated} />}
    </Modal>
  )
}

function Formulario({
  fazenda,
  onClose,
  onSaved,
}: {
  fazenda?: Fazenda
  onClose: () => void
  onSaved: (fazenda: Fazenda) => void
}) {
  const queryClient = useQueryClient()
  const [nome, setNome] = useState(fazenda?.nome ?? '')
  const [descricao, setDescricao] = useState(fazenda?.descricao ?? '')
  const [municipio, setMunicipio] = useState(fazenda?.municipio ?? '')
  const [estado, setEstado] = useState(fazenda?.estado ?? '')
  const [pinos, setPinos] = useState<Coordenada[]>(fazenda?.contorno ?? [])
  const [erro, setErro] = useState('')
  const [destino, setDestino] = useState<Destino | null>(() =>
    !fazenda?.contorno?.length && fazenda?.latitude != null && fazenda.longitude != null
      ? { latitude: fazenda.latitude, longitude: fazenda.longitude, limites: null }
      : null,
  )

  const salvar = useMutation({
    mutationFn: (body: FazendaRequest) => (fazenda ? atualizarFazenda(fazenda.id, body) : criarFazenda(body)),
    onSuccess: async (salva) => {
      await queryClient.invalidateQueries({ queryKey: ['fazendas'] })
      onSaved(salva)
    },
    onError: (error) => {
      setErro(error instanceof ApiError ? error.message : 'Não foi possível salvar a fazenda.')
    },
  })
  const salvando = salvar.isPending

  function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (salvando) return
    setErro('')
    if (!nome.trim()) {
      setErro('Dê um nome para a fazenda.')
      return
    }
    const uf = estado.trim().toLocaleUpperCase('pt-BR')
    if (uf && uf.length !== 2) {
      setErro('O estado usa duas letras, como PR.')
      return
    }
    if (pinos.length < minimoPinos) {
      setErro(`Marque pelo menos ${minimoPinos} cantos da fazenda no mapa.`)
      return
    }
    salvar.mutate({
      nome: nome.trim(),
      descricao: descricao.trim() || null,
      municipio: municipio.trim() || null,
      estado: uf || null,
      contorno: pinos,
    })
  }

  const faltam = Math.max(0, minimoPinos - pinos.length)

  return (
    <form onSubmit={onSubmit} noValidate className="grid gap-6 lg:grid-cols-[18rem_1fr]">
      <div>
        <Field label="Nome">
          <TextInput value={nome} disabled={salvando} onChange={(event) => setNome(event.target.value)} />
        </Field>
        <Field label="Município">
          <TextInput value={municipio} disabled={salvando} onChange={(event) => setMunicipio(event.target.value)} />
        </Field>
        <Field label="Estado">
          <TextInput value={estado} maxLength={2} disabled={salvando} onChange={(event) => setEstado(event.target.value)} />
        </Field>
        <Field label="Descrição">
          <TextInput value={descricao} disabled={salvando} onChange={(event) => setDescricao(event.target.value)} />
        </Field>
      </div>

      <div className="flex min-w-0 flex-col gap-3">
        {buscaDisponivel && <BuscaEndereco onEscolher={setDestino} desabilitado={salvando} />}
        <p className="text-base text-tinta">
          Clique no mapa para marcar cada canto da fazenda, na ordem do contorno. Arraste um pino para corrigir.
        </p>
        <div className="h-[22rem] lg:h-[28rem]">
          <FarmOutlineMap
            pinos={pinos}
            destino={destino}
            desabilitado={salvando}
            onAdicionar={(pino) => setPinos((atual) => [...atual, pino])}
            onMover={(indice, pino) => setPinos((atual) => atual.map((item, i) => (i === indice ? pino : item)))}
          />
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <span className="mr-auto text-base text-nevoa" aria-live="polite">
            {pinos.length === 0
              ? 'Nenhum canto marcado.'
              : faltam > 0
                ? `${pinos.length} ${pinos.length === 1 ? 'canto marcado' : 'cantos marcados'}. Faltam ${faltam}.`
                : `${pinos.length} cantos marcados. Contorno fechado.`}
          </span>
          <Button
            variant="quiet"
            className="gap-2"
            disabled={salvando || pinos.length === 0}
            onClick={() => setPinos((atual) => atual.slice(0, -1))}
          >
            <Undo2 aria-hidden="true" size={18} />
            Desfazer
          </Button>
          <Button variant="quiet" className="gap-2" disabled={salvando || pinos.length === 0} onClick={() => setPinos([])}>
            <Trash2 aria-hidden="true" size={18} />
            Limpar
          </Button>
        </div>

        {erro && (
          <p role="alert" className="text-base text-perigo">
            {erro}
          </p>
        )}
        <div className="flex gap-2">
          <Button type="submit" disabled={salvando || pinos.length < minimoPinos}>
            {salvando ? 'Salvando…' : fazenda ? 'Salvar alterações' : 'Cadastrar fazenda'}
          </Button>
          <Button variant="quiet" onClick={onClose} disabled={salvando}>
            Cancelar
          </Button>
        </div>
      </div>
    </form>
  )
}

type EstadoBusca =
  | { fase: 'parada' }
  | { fase: 'buscando' }
  | { fase: 'resultados'; itens: ResultadoEndereco[] }
  | { fase: 'falhou' }

function BuscaEndereco({ onEscolher, desabilitado }: { onEscolher: (destino: Destino) => void; desabilitado: boolean }) {
  const [texto, setTexto] = useState('')
  const [busca, setBusca] = useState<EstadoBusca>({ fase: 'parada' })

  async function buscar() {
    if (!texto.trim() || busca.fase === 'buscando') return
    setBusca({ fase: 'buscando' })
    try {
      setBusca({ fase: 'resultados', itens: await buscarEndereco(texto, AbortSignal.timeout(8000)) })
    } catch {
      setBusca({ fase: 'falhou' })
    }
  }

  return (
    <div>
      <div className="flex gap-2">
        <div className="flex-1">
          <TextInput
            aria-label="Buscar endereço (opcional)"
            placeholder="Buscar endereço (opcional)"
            value={texto}
            disabled={desabilitado}
            onChange={(event) => setTexto(event.target.value)}
            onKeyDown={(event) => {
              // Enter busca o endereço em vez de enviar o cadastro.
              if (event.key === 'Enter') {
                event.preventDefault()
                void buscar()
              }
            }}
          />
        </div>
        <Button
          variant="quiet"
          className="gap-2"
          disabled={desabilitado || !texto.trim() || busca.fase === 'buscando'}
          onClick={() => void buscar()}
        >
          <Search aria-hidden="true" size={18} />
          {busca.fase === 'buscando' ? 'Buscando…' : 'Buscar'}
        </Button>
      </div>
      {busca.fase === 'falhou' && (
        <p className="mt-2 text-base text-perigo">A busca falhou. Tente de novo ou navegue pelo mapa.</p>
      )}
      {busca.fase === 'resultados' && busca.itens.length === 0 && (
        <p className="mt-2 text-base text-nevoa">Nenhum endereço encontrado. Navegue pelo mapa até a fazenda.</p>
      )}
      {busca.fase === 'resultados' && busca.itens.length > 0 && (
        <ul className="mt-2 divide-y divide-linha rounded-controle border border-linha">
          {busca.itens.map((item) => (
            <li key={item.id}>
              <button
                type="button"
                className="w-full px-3 py-2 text-left text-base text-tinta hover:bg-orvalho"
                onClick={() => {
                  onEscolher({ longitude: item.longitude, latitude: item.latitude, limites: item.limites })
                  setBusca({ fase: 'parada' })
                }}
              >
                {item.nome}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
