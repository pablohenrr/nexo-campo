import { useQuery } from '@tanstack/react-query'
import { buscarPerfil } from '@/features/profile/api.ts'
import type { Perfil } from '@/features/profile/types.ts'
import { FarmsSummary } from '@/features/profile/components/farms-summary.tsx'
import { PasswordForm } from '@/features/profile/components/password-form.tsx'
import { PersonalDataForm } from '@/features/profile/components/personal-data-form.tsx'
import { ApiError } from '@/shared/lib/api.ts'
import { inicialDoNome, mesEAno } from '@/shared/lib/format.ts'
import { Button } from '@/shared/components/ui/button.tsx'

export function ProfilePage() {
  const perfil = useQuery({ queryKey: ['perfil'], queryFn: buscarPerfil })

  return (
    <section className="h-full overflow-y-auto bg-claridade px-6 py-10 lg:px-14">
      <div className="mx-auto flex max-w-2xl flex-col gap-8">
        {perfil.isPending ? (
          <p className="text-base text-nevoa">Carregando perfil…</p>
        ) : perfil.isError ? (
          <div className="rounded-controle border border-linha bg-orvalho p-6">
            <p role="alert" className="mb-4 text-base text-perigo">
              {perfil.error instanceof ApiError ? perfil.error.message : 'Não foi possível carregar o perfil.'}
            </p>
            <Button onClick={() => perfil.refetch()}>Tentar novamente</Button>
          </div>
        ) : (
          <>
            <Cabecalho perfil={perfil.data} />
            <PersonalDataForm key={perfil.data.id} perfil={perfil.data} />
            <PasswordForm contaGoogle={perfil.data.contaGoogle} />
            <FarmsSummary />
          </>
        )}
      </div>
    </section>
  )
}

function Cabecalho({ perfil }: { perfil: Perfil }) {
  const desde = mesEAno(perfil.criadoEm)

  return (
    <header className="flex items-center gap-5">
      <span
        aria-hidden="true"
        className="flex size-16 shrink-0 items-center justify-center rounded-full bg-chuva text-2xl text-cerca"
      >
        {inicialDoNome(perfil.nome)}
      </span>
      <div className="min-w-0">
        <p className="text-base text-nevoa">Seu perfil</p>
        <h1 className="break-words text-4xl leading-tight tracking-tight">{perfil.nome}</h1>
        {desde && <p className="mt-1 text-base text-nevoa">No NEXO Campo desde {desde}</p>}
      </div>
    </header>
  )
}
