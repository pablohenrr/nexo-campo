// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { api } from '@/shared/lib/api.ts'
import { ProfilePage } from '@/features/auth/components/profile-page.tsx'

vi.mock('@/shared/lib/api.ts', () => ({
  ApiError: class ApiError extends Error {},
  api: vi.fn(),
}))

const mockApi = vi.mocked(api)

afterEach(() => {
  cleanup()
  vi.clearAllMocks()
})

describe('ProfilePage', () => {
  it('keeps preferences form hidden when loading fails, then allows retry', async () => {
    mockApi
      .mockRejectedValueOnce(new Error('API indisponível'))
      .mockResolvedValueOnce({ telefone: null, notificacoesSmsAtivas: false })

    render(<ProfilePage />)

    expect(await screen.findByRole('alert')).not.toBeNull()
    expect(screen.queryByRole('button', { name: 'Salvar alterações' })).toBeNull()
    expect(mockApi).toHaveBeenCalledTimes(1)

    fireEvent.click(screen.getByRole('button', { name: 'Tentar novamente' }))

    expect(await screen.findByRole('button', { name: 'Salvar alterações' })).not.toBeNull()
    expect(mockApi).toHaveBeenCalledTimes(2)
    expect(mockApi).toHaveBeenNthCalledWith(1, '/api/usuarios/me/contato')
    expect(mockApi).toHaveBeenNthCalledWith(2, '/api/usuarios/me/contato')
  })
})
