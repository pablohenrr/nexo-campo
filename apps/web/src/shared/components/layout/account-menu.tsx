import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import { definirSessao } from '@/features/auth/session.ts'
import { inicialDoNome } from '@/shared/lib/format.ts'
import type { Sessao } from '@/shared/types/auth.ts'

export function AccountMenu({ sessao }: { sessao: Sessao }) {
  const [aberto, setAberto] = useState(false)
  const raiz = useRef<HTMLDivElement>(null)
  const inicial = inicialDoNome(sessao.nome)

  useEffect(() => {
    if (!aberto) return
    function fechar(event: MouseEvent) {
      if (!raiz.current?.contains(event.target as Node)) setAberto(false)
    }
    document.addEventListener('mousedown', fechar)
    return () => document.removeEventListener('mousedown', fechar)
  }, [aberto])

  return (
    <div className="relative" ref={raiz}>
      <button
        type="button"
        className="flex size-12 items-center justify-center rounded-full bg-chuva text-base text-cerca"
        aria-haspopup="menu"
        aria-expanded={aberto}
        aria-label={`Conta de ${sessao.nome}`}
        onClick={() => setAberto((valor) => !valor)}
      >
        {inicial}
      </button>
      {aberto && (
        <div role="menu" className="absolute right-0 z-20 mt-3 w-72 rounded-controle border border-linha bg-cerca p-4 text-tinta">
          <p className="text-base">{sessao.nome}</p>
          <p className="mt-1 mb-4 text-base text-nevoa">{sessao.email}</p>
          <Link role="menuitem" className="flex min-h-12 items-center" to="/perfil" onClick={() => setAberto(false)}>
            Perfil
          </Link>
          <button
            role="menuitem"
            type="button"
            className="flex min-h-12 w-full items-center text-left"
            onClick={() => definirSessao(null)}
          >
            Sair
          </button>
        </div>
      )}
    </div>
  )
}
