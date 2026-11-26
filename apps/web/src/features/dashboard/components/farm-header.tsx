import { PencilLine, Plus } from 'lucide-react'
import type { Fazenda } from '@/features/farms/types/index.ts'
import { hectares } from '@/shared/lib/format.ts'
import { MetricDigits } from '@/shared/components/ui/metric-digits.tsx'
import { Button } from '@/shared/components/ui/button.tsx'

export function FarmHeader({
  fazenda,
  fazendas,
  area,
  ativos,
  alertas,
  onFazenda,
  onAdicionar,
  onEditar,
  onRegistrar,
  onDesenhar,
  desenhando,
}: {
  fazenda: Fazenda
  fazendas: Fazenda[]
  area: number | null
  ativos: number
  alertas: number
  onFazenda: (id: string) => void
  onAdicionar: () => void
  onEditar: () => void
  onRegistrar: () => void
  onDesenhar: () => void
  desenhando: boolean
}) {
  const lugar = [fazenda.municipio, fazenda.estado].filter(Boolean).join(', ')
  const areaTexto = hectares(area)

  return (
    <header>
      <div className="flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between lg:flex-col lg:items-stretch">
        <div className="min-w-0">
          <h1 key={fazenda.id} className="t-assenta text-4xl leading-tight tracking-tight lg:text-3xl">
            {fazenda.nome}
          </h1>
          {lugar && <p className="mt-2 text-base text-nevoa">{lugar}</p>}
        </div>
        <div className="flex flex-wrap gap-3 lg:[&>button]:flex-1">
          <Button className="whitespace-nowrap px-4" onClick={onRegistrar}>Registrar no talhão</Button>
          <Button variant="quiet" onClick={onDesenhar} disabled={desenhando}>
            Desenhar talhão
          </Button>
        </div>
      </div>
      <dl className="mt-6 grid grid-cols-3 gap-4">
        <Metrica rotulo="Área" valor={areaTexto} chave={`${fazenda.id}-area-${areaTexto}`} />
        <Metrica rotulo="Talhões" valor={String(ativos)} chave={`${fazenda.id}-talhoes-${ativos}`} />
        <Metrica rotulo="Alertas" valor={String(alertas)} chave={`${fazenda.id}-alertas-${alertas}`} />
      </dl>
      <div className="mt-6 flex flex-col gap-3">
        {fazendas.length > 1 && (
          <label className="block text-base text-tinta">
            Fazenda
            <select
              className="mt-2 h-12 w-full rounded-controle border border-linha bg-orvalho px-3 text-base text-tinta"
              value={fazenda.id}
              onChange={(event) => onFazenda(event.target.value)}
            >
              {fazendas.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.nome}
                </option>
              ))}
            </select>
          </label>
        )}
        <div className="grid grid-cols-2 gap-3">
          <Button variant="quiet" className="w-full gap-2 whitespace-nowrap px-3" onClick={onEditar}>
            <PencilLine aria-hidden="true" size={18} />
            Editar área
          </Button>
          <Button variant="quiet" className="w-full gap-2 whitespace-nowrap px-3" onClick={onAdicionar}>
            <Plus aria-hidden="true" size={18} />
            Nova fazenda
          </Button>
        </div>
      </div>
      <div className="t-horizonte mt-6" aria-hidden="true" />
    </header>
  )
}

function Metrica({ rotulo, valor, chave }: { rotulo: string; valor: string; chave: string }) {
  return (
    <div>
      <dt className="text-base text-nevoa">{rotulo}</dt>
      <dd className="mt-2 text-3xl leading-none lg:text-2xl">
        <MetricDigits key={chave} value={valor} />
      </dd>
    </div>
  )
}
