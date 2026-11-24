import { useState, type FormEvent } from 'react'
import { Link, Navigate } from 'react-router'
import { useSyncExternalStore } from 'react'
import { GoogleButton } from '@/features/auth/components/google-button.tsx'
import { definirSessao, ouvirSessao, sessaoAtual } from '@/features/auth/session.ts'
import { ApiError, api } from '@/shared/lib/api.ts'
import type { Sessao } from '@/shared/types/auth.ts'
import { Button } from '@/shared/components/ui/button.tsx'
import { Field, TextInput } from '@/shared/components/ui/field.tsx'
import { AuthSplit } from '@/features/auth/components/auth-split.tsx'

export function RegisterPage() {
  const sessao = useSyncExternalStore(ouvirSessao, sessaoAtual, () => null)
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [telefone, setTelefone] = useState('')
  const [notificacoesSmsAtivas, setNotificacoesSmsAtivas] = useState(false)
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)

  if (sessao) return <Navigate to="/" replace />

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setErro('')
    if (!nome.trim() || !email.trim() || senha.length < 8) {
      setErro('Informe o nome, o e-mail e uma senha com pelo menos 8 caracteres.')
      return
    }
    setEnviando(true)
    try {
      definirSessao(await api<Sessao>('/api/autenticacao/cadastro', {
        method: 'POST',
        body: JSON.stringify({
          nome: nome.trim(),
          email: email.trim(),
          senha,
          telefone: telefone.trim() || null,
          notificacoesSmsAtivas,
        }),
      }))
    } catch (error) {
      setErro(error instanceof ApiError ? error.message : 'Não foi possível criar a conta.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <AuthSplit titulo="Criar conta">
      <GoogleButton rotulo="Inscrever-se com o Google" onErro={setErro} />
      <p className="my-8 text-center text-base text-nevoa">ou</p>
      <form onSubmit={onSubmit} noValidate>
        <Field label="Nome">
          <TextInput
            name="nome"
            autoComplete="name"
            value={nome}
            onChange={(event) => setNome(event.target.value)}
          />
        </Field>
        <Field label="E-mail">
          <TextInput
            type="email"
            name="email"
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </Field>
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
          <span>Quero receber notificações por SMS. Posso alterar essa preferência no meu perfil.</span>
        </label>
        <Field label="Senha">
          <TextInput
            type="password"
            name="senha"
            autoComplete="new-password"
            value={senha}
            onChange={(event) => setSenha(event.target.value)}
          />
        </Field>
        {erro && <p className="mb-4 text-base text-perigo">{erro}</p>}
        <Button type="submit" className="w-full" disabled={enviando}>
          {enviando ? 'Criando conta…' : 'Criar conta'}
        </Button>
      </form>
      <p className="mt-8 text-base text-nevoa">
        Já tem conta?{' '}
        <Link to="/entrar" className="text-chuva underline">Entrar</Link>
      </p>
    </AuthSplit>
  )
}
