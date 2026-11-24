import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { definirSessao, sessaoAtual } from '@/features/auth/session.ts'
import { salvarPerfil } from '@/features/profile/api.ts'
import type { Perfil } from '@/features/profile/types.ts'
import { ApiError } from '@/shared/lib/api.ts'
import { telefoneLegivel } from '@/shared/lib/format.ts'
import { Button } from '@/shared/components/ui/button.tsx'
import { Field, TextInput } from '@/shared/components/ui/field.tsx'

type Erros = { nome?: string; telefone?: string; geral?: string }

export function PersonalDataForm({ perfil }: { perfil: Perfil }) {
  const queryClient = useQueryClient()
  const [nome, setNome] = useState(perfil.nome)
  const [telefone, setTelefone] = useState(telefoneLegivel(perfil.telefone))
  const [sms, setSms] = useState(perfil.notificacoesSmsAtivas)
  const [erros, setErros] = useState<Erros>({})
  const [salvo, setSalvo] = useState(false)

  const alterado =
    nome.trim() !== perfil.nome ||
    telefone.trim() !== telefoneLegivel(perfil.telefone) ||
    sms !== perfil.notificacoesSmsAtivas

  const salvar = useMutation({
    mutationFn: salvarPerfil,
    onSuccess: (atualizado) => {
      queryClient.setQueryData(['perfil'], atualizado)
      preencher(atualizado)
      atualizarNomeDaSessao(atualizado.nome)
      setSalvo(true)
    },
    onError: (error) => {
      const mensagem = error instanceof ApiError ? error.message : 'Não foi possível salvar o perfil.'
      // Os erros de telefone da API citam o telefone ou o celular; mostramos junto do campo.
      setErros(/telefone|celular/i.test(mensagem) ? { telefone: mensagem } : { geral: mensagem })
    },
  })

  function preencher(origem: Perfil) {
    setNome(origem.nome)
    setTelefone(telefoneLegivel(origem.telefone))
    setSms(origem.notificacoesSmsAtivas)
    setErros({})
  }

  function editar(aplicar: () => void) {
    aplicar()
    setSalvo(false)
  }

  function onSubmit(event: FormEvent) {
    event.preventDefault()
    setSalvo(false)
    const novos: Erros = {}
    if (!nome.trim()) novos.nome = 'Informe seu nome.'
    if (sms && !telefone.trim()) novos.telefone = 'Informe o celular para receber os avisos por SMS.'
    setErros(novos)
    if (novos.nome || novos.telefone) return
    salvar.mutate({ nome: nome.trim(), telefone: telefone.trim() || null, notificacoesSmsAtivas: sms })
  }

  return (
    <form
      onSubmit={onSubmit}
      noValidate
      aria-labelledby="perfil-dados"
      className="rounded-controle border border-linha bg-orvalho p-6"
    >
      <h2 id="perfil-dados" className="mb-6 text-2xl tracking-tight">Dados pessoais</h2>
      <Field label="Nome" error={erros.nome}>
        <TextInput
          name="nome"
          autoComplete="name"
          maxLength={120}
          value={nome}
          aria-invalid={Boolean(erros.nome)}
          onChange={(event) => editar(() => setNome(event.target.value))}
        />
      </Field>
      <div className="mb-6 text-base text-tinta">
        <p>E-mail</p>
        <p className="mt-2 flex min-h-12 items-center break-all rounded-controle border border-linha bg-cerca px-3">
          {perfil.email}
        </p>
        <p className="mt-2 text-nevoa">
          {perfil.contaGoogle
            ? 'Vem da sua conta Google e não pode ser trocado aqui.'
            : 'É com ele que você entra no NEXO Campo e não pode ser trocado aqui.'}
        </p>
      </div>
      <Field
        label="Celular"
        hint="Use o número do seu WhatsApp. É por ele que reconhecemos as mensagens que você manda e é ele que recebe os avisos por SMS."
        error={erros.telefone}
      >
        <TextInput
          type="tel"
          name="telefone"
          autoComplete="tel"
          inputMode="tel"
          placeholder="(11) 99999-9999"
          value={telefone}
          aria-invalid={Boolean(erros.telefone)}
          onChange={(event) => editar(() => setTelefone(event.target.value))}
        />
      </Field>
      <label className="mb-6 flex items-start gap-3 text-base text-tinta">
        <input
          type="checkbox"
          name="notificacoesSmsAtivas"
          checked={sms}
          onChange={(event) => editar(() => setSms(event.target.checked))}
          className="mt-1 size-4 accent-chuva"
        />
        <span>Quero receber avisos por SMS.</span>
      </label>
      {erros.geral && <p role="alert" className="mb-4 text-base text-perigo">{erros.geral}</p>}
      {salvo && !alterado && <p role="status" className="mb-4 text-base text-sadio">Alterações salvas.</p>}
      <div className="flex flex-wrap gap-3">
        <Button type="submit" disabled={!alterado || salvar.isPending}>
          {salvar.isPending ? 'Salvando…' : 'Salvar alterações'}
        </Button>
        {alterado && !salvar.isPending && (
          <Button variant="quiet" onClick={() => preencher(perfil)}>
            Desfazer
          </Button>
        )}
      </div>
    </form>
  )
}

/** O nome também aparece no menu da conta, que lê a sessão. */
function atualizarNomeDaSessao(nome: string) {
  const sessao = sessaoAtual()
  if (sessao && sessao.nome !== nome) definirSessao({ ...sessao, nome })
}
