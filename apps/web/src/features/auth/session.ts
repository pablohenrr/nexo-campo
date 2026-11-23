import type { Sessao } from '@/shared/types/auth.ts'

const chave = 'nexo.sessao'
const ouvintes = new Set<() => void>()

function ler(): Sessao | null {
  try {
    const bruto = sessionStorage.getItem(chave)
    if (!bruto) return null
    const valor = JSON.parse(bruto) as Sessao
    if (!valor.token || !valor.nome) return null
    return valor
  } catch {
    return null
  }
}

let atual = ler()

export function sessaoAtual(): Sessao | null {
  return atual
}

export function definirSessao(sessao: Sessao | null) {
  atual = sessao
  if (sessao) sessionStorage.setItem(chave, JSON.stringify(sessao))
  else sessionStorage.removeItem(chave)
  ouvintes.forEach((ouvir) => ouvir())
}

export function ouvirSessao(ouvir: () => void) {
  ouvintes.add(ouvir)
  return () => ouvintes.delete(ouvir)
}
