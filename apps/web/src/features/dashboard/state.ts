import type { ModoMapa } from '@/features/maps/components/field-map.tsx'

export type Vista = 'feed' | 'mapa'
export type Registro = 'atividade' | 'ocorrencia' | 'mensagem'

export type Interacao =
  | { etapa: 'observando' }
  | { etapa: 'desenhando'; pontos: Array<[number, number]> }
  | { etapa: 'nomeando'; pontos: Array<[number, number]> }
  | { etapa: 'escolhendo' }
  | { etapa: 'escolhendo-talhao'; registro: 'atividade' | 'ocorrencia' }
  | { etapa: 'preenchendo'; registro: Registro; talhaoId: string | null }

export type DashboardState = {
  talhaoId: string | null
  vista: Vista
  modo: ModoMapa
  interacao: Interacao
  revelacao: number
}

export type DashboardEvento =
  | { type: 'selecionar'; talhaoId: string }
  | { type: 'limpar' }
  | { type: 'ver-feed' }
  | { type: 'ver-mapa' }
  | { type: 'modo'; modo: ModoMapa }
  | { type: 'iniciar-desenho' }
  | { type: 'vertice'; longitude: number; latitude: number }
  | { type: 'desfazer-vertice' }
  | { type: 'fechar-poligono' }
  | { type: 'abrir-registro' }
  | { type: 'escolher-registro'; registro: Registro | 'talhao' }
  | { type: 'escolher-talhao'; talhaoId: string }
  | { type: 'salvo'; talhaoId?: string }
  | { type: 'cancelar' }
  | { type: 'trocar-fazenda' }

export const estadoInicial: DashboardState = {
  talhaoId: null,
  vista: 'feed',
  modo: 'mapa',
  interacao: { etapa: 'observando' },
  revelacao: 0,
}

export function reduzir(state: DashboardState, evento: DashboardEvento): DashboardState {
  switch (evento.type) {
    case 'selecionar':
      return { ...state, talhaoId: evento.talhaoId, revelacao: state.revelacao + 1 }
    case 'limpar':
      return { ...state, talhaoId: null }
    case 'ver-feed':
      return { ...state, vista: 'feed' }
    case 'ver-mapa':
      return { ...state, vista: 'mapa' }
    case 'modo':
      return { ...state, modo: evento.modo }
    case 'iniciar-desenho':
      return { ...state, vista: 'mapa', interacao: { etapa: 'desenhando', pontos: [] } }
    case 'vertice':
      if (state.interacao.etapa !== 'desenhando') return state
      return {
        ...state,
        interacao: {
          etapa: 'desenhando',
          pontos: [...state.interacao.pontos, [evento.longitude, evento.latitude]],
        },
      }
    case 'desfazer-vertice':
      if (state.interacao.etapa !== 'desenhando') return state
      return {
        ...state,
        interacao: { etapa: 'desenhando', pontos: state.interacao.pontos.slice(0, -1) },
      }
    case 'fechar-poligono':
      if (state.interacao.etapa !== 'desenhando' || state.interacao.pontos.length < 3) return state
      return { ...state, interacao: { etapa: 'nomeando', pontos: state.interacao.pontos } }
    case 'abrir-registro':
      return { ...state, interacao: { etapa: 'escolhendo' } }
    case 'escolher-registro':
      if (evento.registro === 'talhao') {
        return { ...state, vista: 'mapa', interacao: { etapa: 'desenhando', pontos: [] } }
      }
      if (evento.registro === 'mensagem') {
        return { ...state, interacao: { etapa: 'preenchendo', registro: 'mensagem', talhaoId: state.talhaoId } }
      }
      if (!state.talhaoId) {
        return { ...state, interacao: { etapa: 'escolhendo-talhao', registro: evento.registro } }
      }
      return {
        ...state,
        interacao: { etapa: 'preenchendo', registro: evento.registro, talhaoId: state.talhaoId },
      }
    case 'escolher-talhao':
      if (state.interacao.etapa !== 'escolhendo-talhao') return state
      return {
        ...state,
        talhaoId: evento.talhaoId,
        revelacao: state.revelacao + 1,
        interacao: { etapa: 'preenchendo', registro: state.interacao.registro, talhaoId: evento.talhaoId },
      }
    case 'salvo':
      return {
        ...state,
        talhaoId: evento.talhaoId ?? state.talhaoId,
        revelacao: evento.talhaoId ? state.revelacao + 1 : state.revelacao,
        interacao: { etapa: 'observando' },
      }
    case 'cancelar':
      return { ...state, interacao: { etapa: 'observando' } }
    case 'trocar-fazenda':
      return { ...estadoInicial, modo: state.modo }
    default:
      return state
  }
}
