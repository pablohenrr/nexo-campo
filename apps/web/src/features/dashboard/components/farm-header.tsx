import { Plus } from 'lucide-react'
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
  onRegistrar: () => void
  onDesenhar: () => void
  desenhando: boolean
}) {
  const lugar = [fazenda.municipio, fazenda.estado].filter(Boolean).join(', ')
  const areaTexto = hectares(area)

  return (
    <header>
      <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
        <div className="min-w-0">
          <h1 key={fazenda.id} className="t-assenta text-4xl leading-tight tracking-tight">
            {fazenda.nome}
          </h1>
          {lugar && <p className="mt-2 text-base text-nevoa">{lugar}</p>}
        </div>
        <div className="flex flex-wrap gap-3">
          <Button onClick={onRegistrar}>Registrar</Button>
          <Button variant="quiet" onClick={onDesenhar} disabled={desenhando}>
            Desenhar talhão
          </Button>
        </div>
      </div>
      <dl className="mt-8 grid grid-cols-3 gap-6">
        <Metrica rotulo="Área" valor={areaTexto} chave={`${fazenda.id}-area-${areaTexto}`} />
        <Metrica rotulo="Talhões" valor={String(ativos)} chave={`${fazenda.id}-talhoes-${ativos}`} />
        <Metrica rotulo="Alertas" valor={String(alertas)} chave={`${fazenda.id}-alertas-${alertas}`} />
      </dl>
      <div className="mt-8 flex flex-col gap-3">
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
        <Button variant="quiet" className="w-full gap-2" onClick={onAdicionar}>
          <Plus aria-hidden="true" size={18} />
          Adicionar fazenda
        </Button>
      </div>
      <div className="t-horizonte mt-8" aria-hidden="true" />
    </header>
  )
}

function Metrica({ rotulo, valor, chave }: { rotulo: string; valor: string; chave: string }) {
  return (
    <div>
      <dt className="text-base text-nevoa">{rotulo}</dt>
      <dd className="mt-2 text-3xl leading-none">
        <MetricDigits key={chave} value={valor} />
      </dd>
    </div>
  )
}
