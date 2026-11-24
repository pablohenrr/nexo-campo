import { useState, type FormEvent } from 'react'
import { useMutation } from '@tanstack/react-query'
import { trocarSenha } from '@/features/profile/api.ts'
import { ApiError } from '@/shared/lib/api.ts'
import { Button } from '@/shared/components/ui/button.tsx'
import { Field, TextInput } from '@/shared/components/ui/field.tsx'

const classeDoCartao = 'rounded-controle border border-linha bg-orvalho p-6'

export function PasswordForm({ contaGoogle }: { contaGoogle: boolean }) {
  if (contaGoogle) {
    return (
      <section aria-labelledby="perfil-senha" className={classeDoCartao}>
        <h2 id="perfil-senha" className="mb-4 text-2xl tracking-tight">Senha</h2>
        <p className="text-base text-nevoa">
          Você entra com o Google, então não existe uma senha do NEXO Campo para trocar. A senha é a da
          sua conta Google.
        </p>
      </section>
    )
  }
  return <PasswordChange />
}

type Erros = { atual?: string; nova?: string; confirmacao?: string; geral?: string }

function PasswordChange() {
  const [atual, setAtual] = useState('')
  const [nova, setNova] = useState('')
  const [confirmacao, setConfirmacao] = useState('')
  const [erros, setErros] = useState<Erros>({})
  const [trocada, setTrocada] = useState(false)

  const trocar = useMutation({
    mutationFn: trocarSenha,
    onSuccess: () => {
      setAtual('')
      setNova('')
      setConfirmacao('')
      setTrocada(true)
    },
    onError: (error) => {
      const mensagem = error instanceof ApiError ? error.message : 'Não foi possível trocar a senha.'
      setErros(/senha atual/i.test(mensagem) ? { atual: mensagem } : { geral: mensagem })
    },
  })

  function editar(aplicar: () => void) {
    aplicar()
    setTrocada(false)
  }

  function onSubmit(event: FormEvent) {
    event.preventDefault()
    setTrocada(false)
    const novos: Erros = {}
    if (!atual) novos.atual = 'Informe a senha atual.'
    if (nova.length < 8) novos.nova = 'A nova senha precisa ter pelo menos 8 caracteres.'
    else if (nova === atual) novos.nova = 'A nova senha precisa ser diferente da atual.'
    if (!novos.nova && confirmacao !== nova) novos.confirmacao = 'As duas senhas novas não são iguais.'
    setErros(novos)
    if (Object.keys(novos).length) return
    trocar.mutate({ senhaAtual: atual, novaSenha: nova })
  }

  return (
    <form onSubmit={onSubmit} noValidate aria-labelledby="perfil-senha" className={classeDoCartao}>
      <h2 id="perfil-senha" className="mb-6 text-2xl tracking-tight">Senha</h2>
      <Field label="Senha atual" error={erros.atual}>
        <TextInput
          type="password"
          name="senhaAtual"
          autoComplete="current-password"
          value={atual}
          aria-invalid={Boolean(erros.atual)}
          onChange={(event) => editar(() => setAtual(event.target.value))}
        />
      </Field>
      <Field label="Nova senha" hint="Pelo menos 8 caracteres." error={erros.nova}>
        <TextInput
          type="password"
          name="novaSenha"
          autoComplete="new-password"
          value={nova}
          aria-invalid={Boolean(erros.nova)}
          onChange={(event) => editar(() => setNova(event.target.value))}
        />
      </Field>
      <Field label="Repita a nova senha" error={erros.confirmacao}>
        <TextInput
          type="password"
          name="confirmacaoSenha"
          autoComplete="new-password"
          value={confirmacao}
          aria-invalid={Boolean(erros.confirmacao)}
          onChange={(event) => editar(() => setConfirmacao(event.target.value))}
        />
      </Field>
      {erros.geral && <p role="alert" className="mb-4 text-base text-perigo">{erros.geral}</p>}
      {trocada && (
        <p role="status" className="mb-4 text-base text-sadio">
          Senha trocada. Use a nova senha no próximo acesso.
        </p>
      )}
      <Button type="submit" disabled={trocar.isPending}>
        {trocar.isPending ? 'Trocando…' : 'Trocar senha'}
      </Button>
    </form>
  )
}
