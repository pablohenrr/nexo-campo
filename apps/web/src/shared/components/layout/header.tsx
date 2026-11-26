import { Menu } from 'lucide-react'
import { NavLink } from 'react-router'
import { WeatherAlertBell } from '@/features/weather/components/weather-alert-bell.tsx'
import { AccountMenu } from '@/shared/components/layout/account-menu.tsx'
import { areas } from '@/shared/components/layout/sidebar.tsx'
import { cn } from '@/shared/lib/cn.ts'
import type { Sessao } from '@/shared/types/auth.ts'

export function Header({
  sessao,
  menuAberto,
  onMenu,
}: {
  sessao: Sessao
  menuAberto: boolean
  onMenu: () => void
}) {
  return (
    <header className="z-40 flex h-16 shrink-0 items-center gap-4 border-b border-linha bg-claridade px-4 lg:gap-8 lg:px-8">
      <button
        type="button"
        className="flex size-12 items-center justify-center rounded-full border border-linha lg:hidden"
        aria-expanded={menuAberto}
        aria-label={menuAberto ? 'Fechar menu' : 'Abrir menu'}
        onClick={onMenu}
      >
        <Menu aria-hidden="true" size={20} />
      </button>
      <p className="text-xl font-semibold tracking-tight">NEXO</p>
      <nav aria-label="Áreas" className="hidden min-w-0 flex-1 lg:block">
        <ul className="flex items-center gap-1">
          {areas.map((item) => (
            <li key={item.to}>
              <NavLink
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  cn(
                    'flex min-h-12 items-center rounded-full px-4 text-base font-medium',
                    isActive ? 'bg-chuva text-cerca' : 'text-nevoa',
                  )
                }
              >
                {item.label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
      <div className="ml-auto flex items-center gap-2">
        <WeatherAlertBell />
        <AccountMenu sessao={sessao} />
      </div>
    </header>
  )
}
