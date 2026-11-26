import { useQuery } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import { alertasClimaticosKey, listarAlertasClimaticos } from '@/features/weather/api.ts'
import { nomeCategoria, nomeFonte, type AlertaClimatico } from '@/features/weather/types.ts'
import { Button } from '@/shared/components/ui/button.tsx'

export function WeatherAlertPopup() {
  const conhecidos = useRef<Set<string> | null>(null)
  const [aberto, setAberto] = useState<AlertaClimatico | null>(null)
  const dialogRef = useRef<HTMLDialogElement>(null)
  const alertas = useQuery({
    queryKey: alertasClimaticosKey,
    queryFn: listarAlertasClimaticos,
    refetchInterval: 60_000,
  })

  useEffect(() => {
    const eventos = alertas.data?.eventos
    if (!eventos) return
    const naoLidos = eventos.filter((evento) => !evento.lido)
    if (conhecidos.current === null) {
      conhecidos.current = new Set(naoLidos.map((evento) => evento.id))
      return
    }
    const novo = naoLidos.find((evento) => !conhecidos.current?.has(evento.id))
    for (const evento of naoLidos) conhecidos.current.add(evento.id)
    if (novo) setAberto(novo)
  }, [alertas.data])

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return
    if (aberto && !dialog.open) dialog.showModal()
    if (!aberto && dialog.open) dialog.close()
  }, [aberto])

  return (
    <dialog ref={dialogRef} aria-labelledby="alerta-climatico-titulo" onClose={() => setAberto(null)}>
      {aberto && (
        <div className="flex flex-col gap-4">
          <p className="text-sm font-medium tracking-wide text-alerta uppercase">
            {nomeCategoria(aberto.categoria)} · {nomeFonte(aberto.fonte)}
          </p>
          <h2 id="alerta-climatico-titulo" className="text-2xl leading-tight">
            {aberto.frase}
          </h2>
          <p className="text-base text-nevoa">{aberto.fazendas.map((fazenda) => fazenda.nome).join(', ')}</p>
          <div className="flex flex-wrap gap-3">
            <Link to="/alertas" onClick={() => setAberto(null)}>
              <Button>Ver aviso</Button>
            </Link>
            <Button variant="quiet" onClick={() => setAberto(null)}>
              Agora não
            </Button>
          </div>
        </div>
      )}
    </dialog>
  )
}
