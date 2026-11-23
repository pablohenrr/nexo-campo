import type { MensagemCampo, Ocorrencia } from '@/features/activities/types/index.ts'
import type { EstadoTalhao } from '@/features/plots/status.ts'
import type { Talhao } from '@/features/plots/types.ts'

export type AlertaVista = {
  chave: string
  estado: EstadoTalhao
  titulo: string
  detalhe: string
  quando: string
}

function estadoDaSeveridade(severidade: string, categoria: string): EstadoTalhao {
  if (severidade === 'ALTA' && (categoria === 'PRAGA' || categoria === 'DOENCA')) return 'INFESTACAO_ALTA'
  if (severidade === 'BAIXA') return 'ALERTA_BAIXO'
  return 'ALERTA_MEDIO'
}

export function ultimoAlerta(
  talhoes: Talhao[],
  ocorrencias: Ocorrencia[][],
  mensagens: MensagemCampo[],
): AlertaVista | null {
  const abertas = talhoes.flatMap((talhao, index) =>
    (ocorrencias[index] ?? [])
      .filter((item) => item.situacao === 'ABERTA')
      .map((item) => ({ item, talhao })),
  )
  abertas.sort((a, b) => b.item.criadaEm.localeCompare(a.item.criadaEm))
  const primeira = abertas[0]
  if (primeira) {
    return {
      chave: primeira.item.id,
      estado: estadoDaSeveridade(primeira.item.severidade, primeira.item.categoria),
      titulo: primeira.talhao.nome,
      detalhe: primeira.item.descricao,
      quando: primeira.item.dataObservacao,
    }
  }

  const ids = new Set(talhoes.map((item) => item.id))
  const nomes = new Set(talhoes.map((item) => item.nome.toLocaleLowerCase('pt-BR')))
  const mensagensDaFazenda = mensagens
    .filter((item) => item.tipo === 'OCORRENCIA')
    .filter((item) => {
      if (item.talhaoId && ids.has(item.talhaoId)) return true
      return Boolean(item.nomeTalhao && nomes.has(item.nomeTalhao.toLocaleLowerCase('pt-BR')))
    })
    .sort((a, b) => b.criadaEm.localeCompare(a.criadaEm))

  const mensagem = mensagensDaFazenda[0]
  if (!mensagem) return null
  const ocorrencia = mensagem.interpretacao?.ocorrencia
  return {
    chave: mensagem.id,
    estado: ocorrencia ? estadoDaSeveridade(ocorrencia.severidade, ocorrencia.categoria) : 'ALERTA_MEDIO',
    titulo: mensagem.nomeTalhao ?? 'Talhão não identificado',
    detalhe: mensagem.interpretacao?.resumo || mensagem.resposta || 'Alerta registrado pela mensagem.',
    quando: mensagem.criadaEm,
  }
}
