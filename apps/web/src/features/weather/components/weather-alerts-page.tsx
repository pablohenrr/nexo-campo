import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { listarFazendas } from '@/features/farms/api.ts'
import {
  alertasClimaticosKey,
  listarAlertasClimaticos,
  lerPreferenciaAlerta,
  marcarAlertaLido,
  salvarPreferenciaAlerta,
} from '@/features/weather/api.ts'
import {
  categoriasAlerta,
  frequenciasAlerta,
  nomeCategoria,
  nomeFonte,
  type CategoriaAlerta,
  type FrequenciaAlerta,
} from '@/features/weather/types.ts'
import { Button } from '@/shared/components/ui/button.tsx'

export function WeatherAlertsPage() {
  const queryClient = useQueryClient()
  const alertas = useQuery({
    queryKey: alertasClimaticosKey,
    queryFn: listarAlertasClimaticos,
    refetchInterval: 60_000,
  })
  const leitura = useMutation({
    mutationFn: marcarAlertaLido,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: alertasClimaticosKey }),
  })

  return (
    <section className="h-full overflow-auto bg-claridade px-6 py-8 lg:px-14">
      <h1 className="text-4xl leading-tight lg:text-6xl">Alertas</h1>
      <p className="mt-3 max-w-2xl text-lg text-nevoa">
        O aviso fica registrado mesmo com o sistema fechado. O sino conta cada evento, não cada fazenda.
      </p>

      {alertas.isError && <p className="mt-8 text-perigo">Não foi possível carregar os alertas.</p>}
      {alertas.data && alertas.data.eventos.length === 0 && (
        <p className="mt-8 text-lg text-nevoa">Nenhum alerta por enquanto. Fazenda sem ponto no mapa fica de fora.</p>
      )}

      <ul className="mt-8 flex max-w-3xl flex-col gap-4">
        {alertas.data?.eventos.map((evento) => (
          <li key={evento.id} className="rounded-controle border border-linha bg-cerca p-5">
            <p className="text-sm font-medium tracking-wide text-alerta uppercase">
              {nomeCategoria(evento.categoria)} · {nomeFonte(evento.fonte)}
              {evento.lido ? ' · visto' : ' · não lido'}
            </p>
            <p className="mt-2 text-xl leading-snug">{evento.frase}</p>
            <p className="mt-2 text-base text-nevoa">{evento.fazendas.map((fazenda) => fazenda.nome).join(', ')}</p>
            {!evento.lido && (
              <Button className="mt-4" disabled={leitura.isPending} onClick={() => leitura.mutate(evento.id)}>
                Marcar como visto
              </Button>
            )}
          </li>
        ))}
      </ul>

      <PreferenciaDaFazenda />

      <p className="mt-10 max-w-2xl text-base text-nevoa">
        Avisos oficiais do INMET. Previsão das próximas 24 h por Open-Meteo.com, licença CC BY 4.0.
      </p>
    </section>
  )
}

function PreferenciaDaFazenda() {
  const fazendas = useQuery({ queryKey: ['fazendas'], queryFn: listarFazendas })
  const [fazendaId, setFazendaId] = useState('')
  const selecionada = fazendaId || fazendas.data?.[0]?.id || ''
  const preferencia = useQuery({
    queryKey: ['preferencia-alerta', selecionada],
    queryFn: () => lerPreferenciaAlerta(selecionada),
    enabled: Boolean(selecionada),
  })
  const [raioKm, setRaioKm] = useState(30)
  const [categorias, setCategorias] = useState<CategoriaAlerta[]>(['CHUVA', 'VENTO', 'TEMPESTADE'])
  const [frequencia, setFrequencia] = useState<FrequenciaAlerta>('POR_EVENTO')
  const [sincronizada, setSincronizada] = useState('')

  if (preferencia.data && sincronizada !== preferencia.data.fazendaId + preferencia.dataUpdatedAt) {
    setRaioKm(preferencia.data.raioKm)
    setCategorias(preferencia.data.categorias)
    setFrequencia(preferencia.data.frequencia)
    setSincronizada(preferencia.data.fazendaId + preferencia.dataUpdatedAt)
  }

  const salvar = useMutation({
    mutationFn: () => salvarPreferenciaAlerta(selecionada, { raioKm, categorias, frequencia }),
    onSuccess: (salva) => {
      setRaioKm(salva.raioKm)
      setCategorias(salva.categorias)
      setFrequencia(salva.frequencia)
    },
  })

  if (!fazendas.data?.length) return null

  function alternar(categoria: CategoriaAlerta) {
    setCategorias((atual) =>
      atual.includes(categoria) ? atual.filter((item) => item !== categoria) : [...atual, categoria],
    )
  }

  return (
    <form
      className="mt-12 max-w-3xl rounded-controle border border-linha bg-cerca p-5"
      onSubmit={(event) => {
        event.preventDefault()
        salvar.mutate()
      }}
    >
      <h2 className="text-2xl">Preferência da fazenda</h2>
      <p className="mt-2 text-base text-nevoa">Raio padrão de 30 km, no máximo 80. Tornado só entra se o texto do INMET citar.</p>
      <label className="mt-5 block text-base" htmlFor="fazenda-alerta">
        Fazenda
      </label>
      <select
        id="fazenda-alerta"
        className="mt-2 min-h-12 w-full rounded-controle border border-linha bg-orvalho px-3"
        value={selecionada}
        onChange={(event) => setFazendaId(event.target.value)}
      >
        {fazendas.data.map((fazenda) => (
          <option key={fazenda.id} value={fazenda.id}>
            {fazenda.nome}
          </option>
        ))}
      </select>
      <label className="mt-5 block text-base" htmlFor="raio-alerta">
        Raio em km
      </label>
      <input
        id="raio-alerta"
        className="mt-2 min-h-12 w-full rounded-controle border border-linha bg-orvalho px-3"
        type="number"
        min={1}
        max={80}
        value={raioKm}
        onChange={(event) => setRaioKm(Number(event.target.value))}
      />
      <fieldset className="mt-5">
        <legend className="text-base">Categorias</legend>
        <div className="mt-3 flex flex-wrap gap-3">
          {categoriasAlerta.map((categoria) => (
            <label key={categoria.id} className="flex min-h-12 items-center gap-2 rounded-full border border-linha px-4">
              <input
                type="checkbox"
                checked={categorias.includes(categoria.id)}
                onChange={() => alternar(categoria.id)}
              />
              {categoria.label}
            </label>
          ))}
        </div>
      </fieldset>
      <label className="mt-5 block text-base" htmlFor="frequencia-alerta">
        Frequência
      </label>
      <select
        id="frequencia-alerta"
        className="mt-2 min-h-12 w-full rounded-controle border border-linha bg-orvalho px-3"
        value={frequencia}
        onChange={(event) => setFrequencia(event.target.value as FrequenciaAlerta)}
      >
        {frequenciasAlerta.map((item) => (
          <option key={item.id} value={item.id}>
            {item.label}
          </option>
        ))}
      </select>
      <Button className="mt-6" type="submit" disabled={salvar.isPending || raioKm < 1 || raioKm > 80}>
        Salvar preferência
      </Button>
      {salvar.isError && <p className="mt-3 text-perigo">Não foi possível salvar a preferência.</p>}
    </form>
  )
}
