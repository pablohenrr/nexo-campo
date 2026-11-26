import type { AlertasClimaticos, PreferenciaAlerta } from '@/features/weather/types.ts'
import { api } from '@/shared/lib/api.ts'

export const alertasClimaticosKey = ['alertas-climaticos'] as const

export function listarAlertasClimaticos() {
  return api<AlertasClimaticos>('/api/alertas-climaticos')
}

export function marcarAlertaLido(id: string) {
  return api<void>(`/api/alertas-climaticos/${id}/leitura`, { method: 'POST' })
}

export function lerPreferenciaAlerta(fazendaId: string) {
  return api<PreferenciaAlerta>(`/api/fazendas/${fazendaId}/alertas-climaticos`)
}

export function salvarPreferenciaAlerta(fazendaId: string, body: Omit<PreferenciaAlerta, 'fazendaId'>) {
  return api<PreferenciaAlerta>(`/api/fazendas/${fazendaId}/alertas-climaticos`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}
