import { Bug, Image, LandPlot, MessageSquare, Sprout } from 'lucide-react'
import { NavLink } from 'react-router'
import { cn } from '@/shared/lib/cn.ts'

export const areas = [
  { to: '/', label: 'Fazenda', icon: LandPlot, end: true },
  { to: '/atividades', label: 'Atividades', icon: Sprout, end: false },
  { to: '/alertas', label: 'Alertas', icon: Bug, end: false },
  { to: '/evidencias', label: 'Evidências', icon: Image, end: false },
  { to: '/mensagens', label: 'Mensagens', icon: MessageSquare, end: false },
]

export function Sidebar({ aberto, onNavigate }: { aberto: boolean; onNavigate: () => void }) {
  if (!aberto) return null

  return (
    <nav
      aria-label="Áreas"
      className="fixed inset-x-0 top-16 bottom-0 z-30 flex flex-col gap-2 bg-claridade px-6 py-8 lg:hidden"
    >
      {areas.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.end}
          onClick={onNavigate}
          className={({ isActive }) =>
            cn(
              'flex min-h-12 items-center gap-3 rounded-full px-4 text-xl',
              isActive ? 'bg-chuva text-cerca' : 'text-nevoa',
            )
          }
        >
          <item.icon aria-hidden="true" size={22} />
          {item.label}
        </NavLink>
      ))}
    </nav>
  )
}
