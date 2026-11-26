import { BrowserRouter, Navigate, Route, Routes } from 'react-router'
import { LoginPage } from '@/features/auth/components/login-page.tsx'
import { RegisterPage } from '@/features/auth/components/register-page.tsx'
import { ProfilePage } from '@/features/auth/components/profile-page.tsx'
import { DashboardPage } from '@/features/dashboard/components/dashboard-page.tsx'
import { AppShell } from '@/shared/components/layout/app-shell.tsx'
import { AreaPlaceholder } from '@/shared/components/layout/area-placeholder.tsx'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/entrar" element={<LoginPage />} />
        <Route path="/cadastrar" element={<RegisterPage />} />
        <Route element={<AppShell />}>
          <Route index element={<DashboardPage />} />
          <Route path="atividades" element={<AreaPlaceholder nome="Atividades" />} />
          <Route path="alertas" element={<AreaPlaceholder nome="Alertas" />} />
          <Route path="evidencias" element={<AreaPlaceholder nome="Evidências" />} />
          <Route path="mensagens" element={<AreaPlaceholder nome="Mensagens" />} />
          <Route path="perfil" element={<ProfilePage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
