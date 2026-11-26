import { useEffect, useState, type FormEvent } from 'react'
import { ApiError, api } from '@/shared/lib/api.ts'
import { Button } from '@/shared/components/ui/button.tsx'
import { Field, TextInput } from '@/shared/components/ui/field.tsx'

type ContactPreferences = {
  telefone: string | null
  notificacoesSmsAtivas: boolean
}

export function ProfilePage() {
  const [telefone, setTelefone] = useState('')
  const [notificacoesSmsAtivas, setNotificacoesSmsAtivas] = useState(false)
  const [estadoPerfil, setEstadoPerfil] = useState<'carregando' | 'carregado' | 'falha'>('carregando')
  const [tentativaCarregamento, setTentativaCarregamento] = useState(0)
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState('')
  const [sucesso, setSucesso] = useState('')

  useEffect(() => {
    let ativo = true
    api<ContactPreferences>('/api/usuarios/me/contato')
      .then((contato) => {
        if (!ativo) return
        setTelefone(contato.telefone ?? '')
        setNotificacoesSmsAtivas(contato.notificacoesSmsAtivas)
        setEstadoPerfil('carregado')
      })
      .catch((error: unknown) => {
        if (!ativo) return
        setErro(error instanceof ApiError ? error.message : 'Não foi possível carregar o telefone.')
        setEstadoPerfil('falha')
      })

    return () => { ativo = false }
  }, [tentativaCarregamento])

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setErro('')
    setSucesso('')
    setSalvando(true)
    try {
      const contato = await api<ContactPreferences>('/api/usuarios/me/contato', {
        method: 'PUT',
        body: JSON.stringify({
          telefone: telefone.trim() || null,
          notificacoesSmsAtivas,
        }),
      })
      setTelefone(contato.telefone ?? '')
      setNotificacoesSmsAtivas(contato.notificacoesSmsAtivas)
      setSucesso('Preferências de contato salvas.')
    } catch (error) {
      setErro(error instanceof ApiError ? error.message : 'Não foi possível salvar as preferências.')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <section className="h-full overflow-y-auto bg-claridade px-6 py-10 lg:px-14">
      <div className="mx-auto max-w-2xl">
        <h1 className="text-4xl leading-tight lg:text-5xl">Telefone e notificações</h1>
        <p className="mt-4 text-base text-nevoa">
          Atualize seu telefone e escolha se quer receber notificações por SMS.
        </p>
        {estadoPerfil === 'carregando' ? (
          <p className="mt-8 text-base text-nevoa">Carregando perfil…</p>
        ) : estadoPerfil === 'falha' ? (
          <div className="mt-8 rounded-controle border border-linha bg-orvalho p-6">
            <p role="alert" className="mb-4 text-base text-perigo">{erro}</p>
            <Button
              type="button"
              onClick={() => {
                setErro('')
                setEstadoPerfil('carregando')
                setTentativaCarregamento((tentativa) => tentativa + 1)
              }}
            >
              Tentar novamente
            </Button>
          </div>
        ) : (
          <form onSubmit={onSubmit} className="mt-8 rounded-controle border border-linha bg-orvalho p-6">
            <Field label="Telefone (opcional)">
              <TextInput
                type="tel"
                name="telefone"
                autoComplete="tel"
                placeholder="(11) 99999-9999"
                value={telefone}
                onChange={(event) => setTelefone(event.target.value)}
              />
            </Field>
            <label className="mb-6 flex items-start gap-3 text-base text-tinta">
              <input
                type="checkbox"
                name="notificacoesSmsAtivas"
                checked={notificacoesSmsAtivas}
                onChange={(event) => setNotificacoesSmsAtivas(event.target.checked)}
                className="mt-1 size-4 accent-chuva"
              />
              <span>Quero receber notificações por SMS.</span>
            </label>
            {erro && <p role="alert" className="mb-4 text-base text-perigo">{erro}</p>}
            {sucesso && <p role="status" className="mb-4 text-base text-chuva">{sucesso}</p>}
            <Button type="submit" disabled={salvando} className="w-full sm:w-auto">
              {salvando ? 'Salvando…' : 'Salvar alterações'}
            </Button>
          </form>
        )}
      </div>
    </section>
  )
}
