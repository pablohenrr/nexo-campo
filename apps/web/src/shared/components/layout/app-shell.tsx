import { useState } from 'react'
import { Navigate, Outlet } from 'react-router'
import { useSyncExternalStore } from 'react'
import { ouvirSessao, sessaoAtual } from '@/features/auth/session.ts'
import { WeatherAlertPopup } from '@/features/weather/components/weather-alert-popup.tsx'
import { Header } from '@/shared/components/layout/header.tsx'
import { Sidebar } from '@/shared/components/layout/sidebar.tsx'

export function AppShell() {
  const sessao = useSyncExternalStore(ouvirSessao, sessaoAtual, () => null)
  const [menuAberto, setMenuAberto] = useState(false)

  if (!sessao) return <Navigate to="/entrar" replace />

  return (
    <div className="flex h-dvh flex-col bg-claridade">
      <a href="#conteudo" className="sr-only focus:not-sr-only focus:absolute focus:z-50 focus:bg-cerca focus:p-3">
        Ir para o conteúdo
      </a>
      <Header sessao={sessao} menuAberto={menuAberto} onMenu={() => setMenuAberto((valor) => !valor)} />
      <Sidebar aberto={menuAberto} onNavigate={() => setMenuAberto(false)} />
      <main id="conteudo" className="min-h-0 flex-1">
        <Outlet />
      </main>
      <WeatherAlertPopup />
    </div>
  )
}
