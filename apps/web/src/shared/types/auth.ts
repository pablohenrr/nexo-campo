export type Sessao = {
  token: string
  usuarioId: string
  nome: string
  email: string
  perfil: string
}

export type ApiErrorBody = {
  dataHora: string
  status: number
  erro: string
  mensagem: string
  caminho: string
  detalhes: string[]
}
