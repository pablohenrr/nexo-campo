import { useState, type FormEvent } from 'react'
import { Link, Navigate } from 'react-router'
import { useSyncExternalStore } from 'react'
import { AuthSplit } from '@/features/auth/components/auth-split.tsx'
import { GoogleButton } from '@/features/auth/components/google-button.tsx'
import { entrar } from '@/features/auth/api.ts'
import { definirSessao, ouvirSessao, sessaoAtual } from '@/features/auth/session.ts'
import { ApiError } from '@/shared/lib/api.ts'
import { Button } from '@/shared/components/ui/button.tsx'
import { Field, TextInput } from '@/shared/components/ui/field.tsx'

export function LoginPage() {
  const sessao = useSyncExternalStore(ouvirSessao, sessaoAtual, () => null)
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)

  if (sessao) return <Navigate to="/" replace />

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setErro('')
    if (!email.trim() || !senha) {
      setErro('Informe o e-mail e a senha.')
      return
    }
    setEnviando(true)
    try {
      definirSessao(await entrar(email.trim(), senha))
    } catch (error) {
      setErro(error instanceof ApiError ? error.message : 'Não foi possível entrar.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <AuthSplit titulo="Entrar">
      <GoogleButton rotulo="Fazer login com o Google" onErro={setErro} />
      <p className="my-8 text-center text-base text-nevoa">ou</p>
      <form onSubmit={onSubmit} noValidate>
        <Field label="E-mail">
          <TextInput
            type="email"
            name="email"
            autoComplete="username"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </Field>
        <Field label="Senha">
          <TextInput
            type="password"
            name="senha"
            autoComplete="current-password"
            value={senha}
            onChange={(event) => setSenha(event.target.value)}
          />
        </Field>
        {erro && <p className="mb-4 text-base text-perigo">{erro}</p>}
        <Button type="submit" className="w-full" disabled={enviando}>
          {enviando ? 'Entrando…' : 'Entrar na fazenda'}
        </Button>
      </form>
      <p className="mt-8 text-base text-nevoa">
        Ainda não tem conta?{' '}
        <Link to="/cadastrar" className="text-chuva underline">Criar conta</Link>
      </p>
      <p className="mt-4 text-base text-nevoa">Conta de demonstração: demo@nexocampo.local / password</p>
    </AuthSplit>
  )
}
