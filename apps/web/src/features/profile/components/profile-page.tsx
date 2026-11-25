import { useQuery } from '@tanstack/react-query'
import { buscarPerfil } from '@/features/profile/api.ts'
import type { Perfil } from '@/features/profile/types.ts'
import { FarmsSummary } from '@/features/profile/components/farms-summary.tsx'
import { PasswordForm } from '@/features/profile/components/password-form.tsx'
import { PersonalDataForm } from '@/features/profile/components/personal-data-form.tsx'
import { painel } from '@/features/profile/estilo.ts'
import { ApiError } from '@/shared/lib/api.ts'
import { inicialDoNome, mesEAno } from '@/shared/lib/format.ts'
import { Button } from '@/shared/components/ui/button.tsx'

export function ProfilePage() {
  const perfil = useQuery({ queryKey: ['perfil'], queryFn: buscarPerfil })

  if (perfil.isPending) return <p className="p-6 text-lg">Carregando perfil…</p>

  if (perfil.isError) {
    const mensagem = perfil.error instanceof ApiError ? perfil.error.message : 'Não foi possível carregar o perfil.'
    return (
      <div className="p-6">
        <p role="alert" className="text-lg">{mensagem}</p>
        <Button className="mt-4" onClick={() => perfil.refetch()}>
          Tentar de novo
        </Button>
      </div>
    )
  }

  return (
    <div className="h-full overflow-y-auto lg:p-6">
      <div className="mx-auto flex max-w-6xl flex-col gap-3 lg:gap-6">
        <Cabecalho perfil={perfil.data} />
        <div className="grid gap-3 lg:grid-cols-2 lg:items-start lg:gap-6">
          <PersonalDataForm key={perfil.data.id} perfil={perfil.data} />
          <PasswordForm contaGoogle={perfil.data.contaGoogle} />
        </div>
        <FarmsSummary />
      </div>
    </div>
  )
}

function Cabecalho({ perfil }: { perfil: Perfil }) {
  const desde = mesEAno(perfil.criadoEm)

  return (
    <header className={painel}>
      <div className="flex items-center gap-5">
        <span
          aria-hidden="true"
          className="flex size-16 shrink-0 items-center justify-center rounded-full bg-chuva text-2xl text-cerca"
        >
          {inicialDoNome(perfil.nome)}
        </span>
        <div className="min-w-0">
          {/* A chave refaz a animação quando o nome muda, como no título da fazenda. */}
          <h1 key={perfil.nome} className="t-assenta break-words text-4xl leading-tight tracking-tight">
            {perfil.nome}
          </h1>
          {desde && <p className="mt-2 text-base text-nevoa">No NEXO Campo desde {desde}</p>}
        </div>
      </div>
      <div className="t-horizonte mt-8" aria-hidden="true" />
    </header>
  )
}
