export type Perfil = {
  id: string
  nome: string
  email: string
  perfil: string
  /** Formato internacional, como `+5511987654321`. */
  telefone: string | null
  notificacoesSmsAtivas: boolean
  /** Quem entra pelo Google não tem senha do NEXO Campo para trocar. */
  contaGoogle: boolean
  criadoEm: string
}

export type PerfilRequest = {
  nome: string
  telefone: string | null
  notificacoesSmsAtivas: boolean
}

export type TrocaDeSenhaRequest = {
  senhaAtual: string
  novaSenha: string
}
