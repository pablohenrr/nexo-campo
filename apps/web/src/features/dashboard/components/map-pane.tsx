import { useState, type FormEvent } from 'react'
import type { SituacaoTalhao, TalhaoRequest } from '@/features/farms/types/index.ts'
import { FieldMap, type ModoMapa } from '@/features/maps/components/field-map.tsx'
import type { PlotFeature } from '@/features/maps/mountPlotMap.ts'
import { Button } from '@/shared/components/ui/button.tsx'
import { Field, TextInput } from '@/shared/components/ui/field.tsx'

export function MapPane({
  features,
  selectedId,
  modo,
  onModo,
  drawing,
  draft,
  naming,
  onSelect,
  onAddVertex,
  onUndo,
  onFinish,
  onCancel,
  onSave,
  saving,
  erro,
  center,
  fitKey,
  visivel,
}: {
  features: PlotFeature[]
  selectedId: string | null
  modo: ModoMapa
  onModo: (modo: ModoMapa) => void
  drawing: boolean
  draft: Array<[number, number]>
  naming: boolean
  onSelect: (id: string) => void
  onAddVertex: (longitude: number, latitude: number) => void
  onUndo: () => void
  onFinish: () => void
  onCancel: () => void
  onSave: (body: Omit<TalhaoRequest, 'limiteWkt'>) => void
  saving: boolean
  erro: string
  center: { latitude: number; longitude: number } | null
  fitKey: string
  visivel: boolean
}) {
  return (
    <div className="relative h-full min-h-80">
      <FieldMap
        features={features}
        selectedId={selectedId}
        modo={modo}
        onModo={onModo}
        drawing={drawing || naming}
        draft={draft}
        onSelect={onSelect}
        onAddVertex={onAddVertex}
        center={center}
        fitKey={fitKey}
        visivel={visivel}
      />
      {drawing && (
        <div className="absolute inset-x-3 bottom-3 z-10 rounded-controle border border-linha bg-cerca p-4">
          <p>
            Toque nos cantos do talhão. {draft.length} {draft.length === 1 ? 'canto' : 'cantos'}.
          </p>
          {erro && <p className="mt-2 text-base text-perigo">{erro}</p>}
          <div className="mt-3 flex flex-wrap gap-2">
            <Button onClick={onFinish} disabled={draft.length < 3}>
              Fechar talhão
            </Button>
            <Button variant="quiet" onClick={onUndo} disabled={draft.length === 0}>
              Desfazer canto
            </Button>
            <Button variant="quiet" onClick={onCancel}>
              Cancelar
            </Button>
          </div>
        </div>
      )}
      {naming && (
        <PlotForm cantos={draft.length} saving={saving} erro={erro} onSave={onSave} onCancel={onCancel} />
      )}
    </div>
  )
}

function PlotForm({
  cantos,
  saving,
  erro,
  onSave,
  onCancel,
}: {
  cantos: number
  saving: boolean
  erro: string
  onSave: (body: Omit<TalhaoRequest, 'limiteWkt'>) => void
  onCancel: () => void
}) {
  const [nome, setNome] = useState('')
  const [cultura, setCultura] = useState('')
  const [area, setArea] = useState('')
  const [situacao, setSituacao] = useState<SituacaoTalhao>('ATIVO')
  const [local, setLocal] = useState('')

  function enviar(event: FormEvent) {
    event.preventDefault()
    if (!nome.trim()) {
      setLocal('Dê um nome para o talhão.')
      return
    }
    const texto = area.trim().replace(',', '.')
    const numero = texto ? Number(texto) : null
    if (numero != null && !Number.isFinite(numero)) {
      setLocal('A área precisa ser um número.')
      return
    }
    setLocal('')
    onSave({
      nome: nome.trim(),
      cultura: cultura.trim() || null,
      areaHectares: numero,
      situacao,
    })
  }

  return (
    <form onSubmit={enviar} className="absolute inset-x-3 bottom-3 z-10 rounded-controle border border-linha bg-cerca p-4" noValidate>
      <h2 className="mb-3 text-lg">Talhão com {cantos} cantos</h2>
      <Field label="Nome" error={!nome.trim() ? local : undefined}>
        <TextInput value={nome} onChange={(event) => setNome(event.target.value)} />
      </Field>
      <Field label="Cultura">
        <TextInput value={cultura} onChange={(event) => setCultura(event.target.value)} />
      </Field>
      <Field label="Área em hectares">
        <TextInput inputMode="decimal" value={area} onChange={(event) => setArea(event.target.value)} />
      </Field>
      <Field label="Situação">
        <select
          className="h-12 w-full rounded-controle border border-linha bg-orvalho px-3 text-base text-tinta"
          value={situacao}
          onChange={(event) => setSituacao(event.target.value as SituacaoTalhao)}
        >
          <option value="ATIVO">Ativo</option>
          <option value="INATIVO">Inativo</option>
        </select>
      </Field>
      {(local && nome.trim()) || erro ? <p className="mb-4 text-base text-perigo">{erro || local}</p> : null}
      <div className="flex flex-wrap gap-2">
        <Button type="submit" disabled={saving}>
          {saving ? 'Salvando…' : 'Salvar talhão'}
        </Button>
        <Button type="button" variant="quiet" onClick={onCancel} disabled={saving}>
          Cancelar
        </Button>
      </div>
    </form>
  )
}
