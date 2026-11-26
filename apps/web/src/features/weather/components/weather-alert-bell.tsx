import { useQuery } from '@tanstack/react-query'
import { Bell } from 'lucide-react'
import { Link } from 'react-router'
import { alertasClimaticosKey, listarAlertasClimaticos } from '@/features/weather/api.ts'

export function WeatherAlertBell() {
  const alertas = useQuery({
    queryKey: alertasClimaticosKey,
    queryFn: listarAlertasClimaticos,
    refetchInterval: 60_000,
  })
  const naoLidos = alertas.data?.naoLidos ?? 0
  const rotulo = naoLidos === 1 ? '1 alerta não lido' : `${naoLidos} alertas não lidos`

  return (
    <Link
      to="/alertas"
      className="relative flex size-12 items-center justify-center rounded-full border border-linha text-tinta"
      aria-label={naoLidos > 0 ? `Alertas, ${rotulo}` : 'Alertas'}
    >
      <Bell aria-hidden="true" size={20} />
      {naoLidos > 0 && (
        <span className="absolute -top-1 -right-1 flex min-h-6 min-w-6 items-center justify-center rounded-full bg-perigo px-1 text-sm font-semibold text-cerca">
          {naoLidos > 9 ? '9+' : naoLidos}
        </span>
      )}
    </Link>
  )
}
