import type { Atividade, MensagemCampo, Ocorrencia } from '@/features/activities/types/index.ts'
import type { Talhao } from '@/features/farms/types/index.ts'

export type EventoLinha =
  | { id: string; kind: 'mensagem'; em: string; mensagem: MensagemCampo }
  | { id: string; kind: 'atividade'; em: string; atividade: Atividade; nomeTalhao: string }
  | { id: string; kind: 'ocorrencia'; em: string; ocorrencia: Ocorrencia; nomeTalhao: string }

export function montarLinha(
  talhoes: Talhao[],
  atividades: Atividade[][],
  ocorrencias: Ocorrencia[][],
  mensagens: MensagemCampo[],
  talhaoId: string | null,
): EventoLinha[] {
  const eventos: EventoLinha[] = []

  talhoes.forEach((talhao, index) => {
    if (talhaoId && talhao.id !== talhaoId) return
    for (const atividade of atividades[index] ?? []) {
      eventos.push({
        id: `atividade-${atividade.id}`,
        kind: 'atividade',
        em: `${atividade.dataAtividade}T12:00:00`,
        atividade,
        nomeTalhao: talhao.nome,
      })
    }
    for (const ocorrencia of ocorrencias[index] ?? []) {
      eventos.push({
        id: `ocorrencia-${ocorrencia.id}`,
        kind: 'ocorrencia',
        em: ocorrencia.criadaEm,
        ocorrencia,
        nomeTalhao: talhao.nome,
      })
    }
  })

  for (const mensagem of mensagens) {
    const doTalhao = talhaoId != null && mensagem.talhaoId === talhaoId
    const daFazenda = talhaoId == null
    if (!doTalhao && !daFazenda) continue
    eventos.push({
      id: `mensagem-${mensagem.id}`,
      kind: 'mensagem',
      em: mensagem.criadaEm,
      mensagem,
    })
  }

  eventos.sort((a, b) => b.em.localeCompare(a.em))
  return eventos
}
