import type { ReactNode } from 'react'

export function AuthSplit({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <main className="flex min-h-dvh flex-col bg-claridade text-tinta">
      <div className="mx-auto flex w-full max-w-5xl flex-1 flex-col justify-center px-6 py-12 lg:px-10">
        <p className="text-base font-semibold tracking-tight">NEXO</p>
        <div className="mt-10 grid items-end gap-12 lg:grid-cols-2 lg:gap-16">
          <h1 className="t-assenta max-w-md text-4xl leading-tight tracking-tight lg:text-6xl">
            A fazenda e os talhões no mesmo lugar.
          </h1>
          <section>
            <h2 className="mb-8 text-3xl">{titulo}</h2>
            {children}
          </section>
        </div>
        <div className="t-horizonte mt-12" aria-hidden="true" />
      </div>
    </main>
  )
}
