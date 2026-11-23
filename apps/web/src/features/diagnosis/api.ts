import type { EnvioMensagem, MensagemCampo, Ocorrencia, OcorrenciaRequest } from '@/features/activities/types/index.ts'
import { api } from '@/shared/lib/api.ts'

export function listarOcorrencias(talhaoId: string) {
  return api<Ocorrencia[]>(`/api/talhoes/${talhaoId}/ocorrencias`)
}

export function criarOcorrencia(talhaoId: string, body: OcorrenciaRequest) {
  return api<Ocorrencia>(`/api/talhoes/${talhaoId}/ocorrencias`, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function listarMensagens() {
  return api<MensagemCampo[]>('/api/ingestao/mensagens')
}

export function enviarMensagem(envio: EnvioMensagem) {
  const body = new FormData()
  if (envio.texto) body.set('texto', envio.texto)
  if (envio.arquivo) body.set('arquivo', envio.arquivo)
  return api<MensagemCampo>('/api/ingestao/mensagens', { method: 'POST', body })
}
