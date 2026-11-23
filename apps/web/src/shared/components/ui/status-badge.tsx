import type { EstadoTalhao } from '@/features/plots/status.ts'
import { rotuloEstado } from '@/features/plots/status.ts'
import { cn } from '@/shared/lib/cn.ts'

const tom: Record<EstadoTalhao, string> = {
  INFESTACAO_ALTA: 'border-perigo text-perigo',
  ALERTA_MEDIO: 'border-alerta text-alerta',
  ALERTA_BAIXO: 'border-sadio text-sadio',
  COLHEITA: 'border-alerta text-alerta',
  MONITORAMENTO: 'border-tinta text-tinta',
  INATIVO: 'border-nevoa text-nevoa',
}

export function StatusBadge({ estado, pop = false }: { estado: EstadoTalhao; pop?: boolean }) {
  const label = rotuloEstado[estado]
  const pill = (
    <span className={cn('inline-flex min-h-8 items-center rounded-controle border bg-cerca px-3 text-base', tom[estado])}>
      {label}
    </span>
  )

  if (!pop) return pill

  return (
    <span className="t-notification-badge t-badge-inline" data-open="true">
      <span className="t-badge-dot">{pill}</span>
    </span>
  )
}
