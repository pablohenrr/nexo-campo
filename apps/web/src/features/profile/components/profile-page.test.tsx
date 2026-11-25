// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { definirSessao, sessaoAtual } from '@/features/auth/session.ts'
import { listarFazendas } from '@/features/farms/api.ts'
import { listarTalhoes } from '@/features/plots/api.ts'
import { buscarPerfil, salvarPerfil, trocarSenha } from '@/features/profile/api.ts'
import type { Perfil } from '@/features/profile/types.ts'
import { ProfilePage } from '@/features/profile/components/profile-page.tsx'
import { ApiError } from '@/shared/lib/api.ts'

vi.mock('@/features/profile/api.ts', () => ({
  buscarPerfil: vi.fn(),
  salvarPerfil: vi.fn(),
  trocarSenha: vi.fn(),
}))
vi.mock('@/features/farms/api.ts', () => ({ listarFazendas: vi.fn() }))
vi.mock('@/features/plots/api.ts', () => ({ listarTalhoes: vi.fn() }))

const perfil: Perfil = {
  id: 'u1',
  nome: 'Maria da Silva',
  email: 'maria@example.com',
  perfil: 'PRODUTOR',
  telefone: '+5511987654321',
  notificacoesSmsAtivas: true,
  contaGoogle: false,
  criadoEm: '2026-11-20T10:00:00Z',
}

function renderizar() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <ProfilePage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

beforeEach(() => {
  vi.mocked(listarFazendas).mockResolvedValue([])
  definirSessao({ token: 't', usuarioId: 'u1', nome: 'Maria da Silva', email: 'maria@example.com', perfil: 'PRODUTOR' })
})

afterEach(() => {
  cleanup()
  vi.clearAllMocks()
  definirSessao(null)
})

describe('ProfilePage', () => {
  it('shows the saved data and keeps the account menu name in sync after saving', async () => {
    vi.mocked(buscarPerfil).mockResolvedValue(perfil)
    vi.mocked(salvarPerfil).mockResolvedValue({ ...perfil, nome: 'Maria Souza' })
    renderizar()

    const nome = await screen.findByRole('textbox', { name: /^Nome/ })
    expect((screen.getByRole('textbox', { name: /^Telefone/ }) as HTMLInputElement).value).toBe('(11) 98765-4321')
    expect(screen.getByText('maria@example.com')).not.toBeNull()
    expect(screen.getByText('No NEXO Campo desde novembro de 2026')).not.toBeNull()
    const salvar = screen.getByRole('button', { name: 'Salvar alterações' }) as HTMLButtonElement
    expect(salvar.disabled).toBe(true)

    fireEvent.change(nome, { target: { value: '  Maria Souza ' } })
    expect(salvar.disabled).toBe(false)
    fireEvent.click(salvar)

    expect(await screen.findByRole('status')).toHaveProperty('textContent', 'Alterações salvas.')
    expect(vi.mocked(salvarPerfil).mock.calls[0][0]).toEqual({
      nome: 'Maria Souza',
      telefone: '(11) 98765-4321',
      notificacoesSmsAtivas: true,
    })
    expect(sessaoAtual()?.nome).toBe('Maria Souza')
    expect(screen.getByRole('heading', { level: 1 }).textContent).toBe('Maria Souza')
  })

  it('shows phone errors from the API next to the phone field', async () => {
    vi.mocked(buscarPerfil).mockResolvedValue(perfil)
    vi.mocked(salvarPerfil).mockRejectedValue(new ApiError(400, 'Este telefone já está cadastrado em outra conta'))
    renderizar()

    fireEvent.change(await screen.findByRole('textbox', { name: /^Telefone/ }), { target: { value: '(21) 99999-0000' } })
    fireEvent.click(screen.getByRole('button', { name: 'Salvar alterações' }))

    const campo = await screen.findByRole('textbox', { name: /já está cadastrado em outra conta/ })
    expect(campo.getAttribute('name')).toBe('telefone')
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('asks for a phone before turning SMS on', async () => {
    vi.mocked(buscarPerfil).mockResolvedValue({ ...perfil, telefone: null, notificacoesSmsAtivas: false })
    renderizar()

    fireEvent.click(await screen.findByRole('checkbox', { name: 'Quero receber notificações por SMS.' }))
    fireEvent.click(screen.getByRole('button', { name: 'Salvar alterações' }))

    expect(screen.getByText('Informe o telefone para receber as notificações por SMS.')).not.toBeNull()
    expect(salvarPerfil).not.toHaveBeenCalled()
  })

  it('checks the new password confirmation before calling the API', async () => {
    vi.mocked(buscarPerfil).mockResolvedValue(perfil)
    vi.mocked(trocarSenha).mockResolvedValue(undefined)
    renderizar()

    fireEvent.change(await screen.findByLabelText(/^Senha atual/), { target: { value: 'senha-antiga-123' } })
    fireEvent.change(screen.getByLabelText(/^Nova senha/), { target: { value: 'senha-nova-456' } })
    fireEvent.change(screen.getByLabelText(/^Repita a nova senha/), { target: { value: 'senha-nova-457' } })
    fireEvent.click(screen.getByRole('button', { name: 'Trocar senha' }))

    expect(screen.getByText('As duas senhas novas não são iguais.')).not.toBeNull()
    expect(trocarSenha).not.toHaveBeenCalled()

    fireEvent.change(screen.getByLabelText(/^Repita a nova senha/), { target: { value: 'senha-nova-456' } })
    fireEvent.click(screen.getByRole('button', { name: 'Trocar senha' }))

    expect(await screen.findByText('Senha trocada. Use a nova senha no próximo acesso.')).not.toBeNull()
    expect(vi.mocked(trocarSenha).mock.calls[0][0]).toEqual({ senhaAtual: 'senha-antiga-123', novaSenha: 'senha-nova-456' })
    expect((screen.getByLabelText(/^Senha atual/) as HTMLInputElement).value).toBe('')
  })

  it('shows a wrong current password next to that field', async () => {
    vi.mocked(buscarPerfil).mockResolvedValue(perfil)
    vi.mocked(trocarSenha).mockRejectedValue(new ApiError(400, 'A senha atual não confere'))
    renderizar()

    fireEvent.change(await screen.findByLabelText(/^Senha atual/), { target: { value: 'chute' } })
    fireEvent.change(screen.getByLabelText(/^Nova senha/), { target: { value: 'senha-nova-456' } })
    fireEvent.change(screen.getByLabelText(/^Repita a nova senha/), { target: { value: 'senha-nova-456' } })
    fireEvent.click(screen.getByRole('button', { name: 'Trocar senha' }))

    expect((await screen.findByLabelText(/A senha atual não confere/)).getAttribute('name')).toBe('senhaAtual')
  })

  it('tells Google accounts there is no password to change', async () => {
    vi.mocked(buscarPerfil).mockResolvedValue({ ...perfil, contaGoogle: true })
    renderizar()

    expect(await screen.findByText(/Você entra com o Google/)).not.toBeNull()
    expect(screen.getByText('Vem da sua conta Google e não pode ser trocado aqui.')).not.toBeNull()
    expect(screen.queryByRole('button', { name: 'Trocar senha' })).toBeNull()
  })

  it('summarizes the farms with a link to each one on the map', async () => {
    vi.mocked(buscarPerfil).mockResolvedValue(perfil)
    vi.mocked(listarFazendas).mockResolvedValue([{
      id: 'f1', nome: 'Fazenda Boa Esperança', descricao: null, municipio: 'Cascavel', estado: 'PR',
      areaTotalHectares: 120, latitude: null, longitude: null,
    }])
    vi.mocked(listarTalhoes).mockResolvedValue([
      { id: 't1', fazendaId: 'f1', nome: 'Talhão 01', cultura: 'Milho', areaHectares: 40, situacao: 'ATIVO', limiteWkt: '' },
      { id: 't2', fazendaId: 'f1', nome: 'Talhão 02', cultura: 'Soja', areaHectares: 80, situacao: 'ATIVO', limiteWkt: '' },
    ])
    renderizar()

    expect(await screen.findByText('Fazenda Boa Esperança')).not.toBeNull()
    await waitFor(() => expect(screen.getByText('Cascavel, PR · 120 ha · 2 talhões')).not.toBeNull())
    expect(screen.getByRole('link', { name: 'Abrir Fazenda Boa Esperança no mapa' }).getAttribute('href'))
      .toBe('/?fazenda=f1')
  })

  it('offers to register a farm when there is none', async () => {
    vi.mocked(buscarPerfil).mockResolvedValue(perfil)
    renderizar()

    expect(await screen.findByText('Você ainda não cadastrou nenhuma fazenda.')).not.toBeNull()
    expect(screen.getByRole('link', { name: 'Cadastrar fazenda' }).getAttribute('href')).toBe('/')
  })

  it('lets the user retry when the profile does not load', async () => {
    vi.mocked(buscarPerfil)
      .mockRejectedValueOnce(new ApiError(0, 'A API não respondeu. Confira se ela está no ar.'))
      .mockResolvedValueOnce(perfil)
    renderizar()

    expect((await screen.findByRole('alert')).textContent).toBe('A API não respondeu. Confira se ela está no ar.')
    expect(screen.queryByRole('button', { name: 'Salvar alterações' })).toBeNull()

    fireEvent.click(screen.getByRole('button', { name: 'Tentar de novo' }))

    expect(await screen.findByRole('button', { name: 'Salvar alterações' })).not.toBeNull()
    expect(buscarPerfil).toHaveBeenCalledTimes(2)
  })
})
