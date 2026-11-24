import { useQueries, useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { listarFazendas } from '@/features/farms/api.ts'
import type { Fazenda } from '@/features/farms/types/index.ts'
import { listarTalhoes } from '@/features/plots/api.ts'
import { hectares } from '@/shared/lib/format.ts'

const classeDoLink =
  'inline-flex min-h-12 shrink-0 items-center rounded-full border border-linha px-5 text-base font-medium text-tinta'

/** Resumo das fazendas no perfil. A edição fica no mapa, então cada uma leva para lá. */
export function FarmsSummary() {
  // Mesmas chaves do painel, para aproveitar o que já foi carregado lá.
  const fazendas = useQuery({ queryKey: ['fazendas'], queryFn: listarFazendas })
  const lista = fazendas.data ?? []
  const talhoes = useQueries({
    queries: lista.map((fazenda) => ({
      queryKey: ['talhoes', fazenda.id],
      queryFn: () => listarTalhoes(fazenda.id),
    })),
  })

  return (
    <section aria-labelledby="perfil-fazendas" className="rounded-controle border border-linha bg-orvalho p-6">
      <h2 id="perfil-fazendas" className="mb-2 text-2xl tracking-tight">Suas fazendas</h2>
      {fazendas.isPending ? (
        <p className="text-base text-nevoa">Carregando fazendas…</p>
      ) : fazendas.isError ? (
        <p role="alert" className="text-base text-perigo">Não foi possível carregar as fazendas.</p>
      ) : lista.length === 0 ? (
        <div className="flex flex-col items-start gap-4">
          <p className="text-base text-nevoa">Você ainda não cadastrou nenhuma fazenda.</p>
          <Link to="/" className={classeDoLink}>Cadastrar fazenda</Link>
        </div>
      ) : (
        <ul className="divide-y divide-linha">
          {lista.map((fazenda, indice) => (
            <li key={fazenda.id} className="flex flex-wrap items-center justify-between gap-4 py-4">
              <div className="min-w-0">
                <p className="text-lg text-tinta">{fazenda.nome}</p>
                <p className="mt-1 text-base text-nevoa">{detalhes(fazenda, talhoes[indice]?.data?.length)}</p>
              </div>
              <Link to={`/?fazenda=${fazenda.id}`} className={classeDoLink} aria-label={`Abrir ${fazenda.nome} no mapa`}>
                Abrir no mapa
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}

function detalhes(fazenda: Fazenda, quantidadeDeTalhoes: number | undefined): string {
  const lugar = [fazenda.municipio, fazenda.estado].filter(Boolean).join(', ')
  const area = fazenda.areaTotalHectares == null ? '' : hectares(fazenda.areaTotalHectares)
  const talhoes =
    quantidadeDeTalhoes == null ? '' : quantidadeDeTalhoes === 1 ? '1 talhão' : `${quantidadeDeTalhoes} talhões`
  return [lugar, area, talhoes].filter(Boolean).join(' · ') || 'Sem detalhes cadastrados'
}
