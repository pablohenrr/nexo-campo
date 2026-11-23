import { useState, type FormEvent } from 'react'
import { Button } from '@/shared/components/ui/button.tsx'
import { Field, TextInput } from '@/shared/components/ui/field.tsx'
import { Modal } from '@/shared/components/ui/modal.tsx'

export type RascunhoTalhao = {
  nome: string
  cultura: string | null
  areaHectares: number | null
}

export function CreatePlotModal({
  open,
  onClose,
  onDraw,
}: {
  open: boolean
  onClose: () => void
  onDraw: (rascunho: RascunhoTalhao) => void
}) {
  const [nome, setNome] = useState('')
  const [cultura, setCultura] = useState('')
  const [area, setArea] = useState('')
  const [erro, setErro] = useState('')

  function onSubmit(event: FormEvent) {
    event.preventDefault()
    setErro('')
    if (!nome.trim()) {
      setErro('Dê um nome para o talhão.')
      return
    }
    const texto = area.trim().replace(',', '.')
    const numero = texto ? Number(texto) : null
    if (numero != null && !Number.isFinite(numero)) {
      setErro('A área precisa ser um número.')
      return
    }
    onDraw({
      nome: nome.trim(),
      cultura: cultura.trim() || null,
      areaHectares: numero,
    })
    setNome('')
    setCultura('')
    setArea('')
  }

  return (
    <Modal open={open} title="Novo talhão" onClose={onClose}>
      <form onSubmit={onSubmit} noValidate>
        <Field label="Nome" error={!nome.trim() ? erro : undefined}>
          <TextInput value={nome} onChange={(event) => setNome(event.target.value)} />
        </Field>
        <Field label="Cultura">
          <TextInput value={cultura} onChange={(event) => setCultura(event.target.value)} />
        </Field>
        <Field label="Área em hectares">
          <TextInput inputMode="decimal" value={area} onChange={(event) => setArea(event.target.value)} />
        </Field>
        {erro && nome.trim() && <p className="mb-4 text-base text-perigo">{erro}</p>}
        <div className="flex gap-2">
          <Button type="submit">Marcar no mapa</Button>
          <Button type="button" variant="quiet" onClick={onClose}>
            Cancelar
          </Button>
        </div>
      </form>
    </Modal>
  )
}
