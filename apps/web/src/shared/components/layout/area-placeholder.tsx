export function AreaPlaceholder({ nome }: { nome: string }) {
  return (
    <section className="flex h-full flex-col justify-center bg-claridade px-6 py-16 lg:px-14">
      <h1 className="max-w-xl text-5xl leading-tight lg:text-7xl">{nome}</h1>
      <p className="mt-6 max-w-md text-lg text-nevoa">Esta parte chega na próxima tela.</p>
    </section>
  )
}
