import { useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { criarFazenda } from '@/features/farms/api.ts'
import type { Fazenda } from '@/features/farms/types/index.ts'
import { ApiError } from '@/shared/lib/api.ts'
import { Button } from '@/shared/components/ui/button.tsx'
import { Field, TextInput } from '@/shared/components/ui/field.tsx'
import { Modal } from '@/shared/components/ui/modal.tsx'

export function CreateFarmModal({
  open,
  onClose,
  onCreated,
}: {
  open: boolean
  onClose: () => void
  onCreated: (fazenda: Fazenda) => void
}) {
  const queryClient = useQueryClient()
  const [nome, setNome] = useState('')
  const [descricao, setDescricao] = useState('')
  const [municipio, setMunicipio] = useState('')
  const [estado, setEstado] = useState('')
  const [area, setArea] = useState('')
  const [latitude, setLatitude] = useState('')
  const [longitude, setLongitude] = useState('')
  const [erro, setErro] = useState('')

  const mutation = useMutation({
    mutationFn: criarFazenda,
    onSuccess: async (fazenda) => {
      await queryClient.invalidateQueries({ queryKey: ['fazendas'] })
      limpar()
      onCreated(fazenda)
    },
    onError: (error) => {
      setErro(error instanceof ApiError ? error.message : 'Não foi possível criar a fazenda.')
    },
  })

  function limpar() {
    setNome('')
    setDescricao('')
    setMunicipio('')
    setEstado('')
    setArea('')
    setLatitude('')
    setLongitude('')
    setErro('')
  }

  function onSubmit(event: FormEvent) {
    event.preventDefault()
    setErro('')
    if (!nome.trim()) {
      setErro('Dê um nome para a fazenda.')
      return
    }
    const uf = estado.trim().toLocaleUpperCase('pt-BR')
    if (uf && uf.length !== 2) {
      setErro('O estado usa duas letras, como PR.')
      return
    }
    const areaTotal = lerNumero(area)
    const lat = lerNumero(latitude)
    const lng = lerNumero(longitude)
    if (Number.isNaN(areaTotal) || Number.isNaN(lat) || Number.isNaN(lng)) {
      setErro('Use números nas áreas e nas coordenadas.')
      return
    }
    mutation.mutate({
      nome: nome.trim(),
      descricao: descricao.trim() || null,
      municipio: municipio.trim() || null,
      estado: uf || null,
      areaTotalHectares: areaTotal,
      latitude: lat,
      longitude: lng,
    })
  }

  return (
    <Modal
      open={open}
      title="Nova fazenda"
      onClose={() => {
        if (!mutation.isPending) onClose()
      }}
    >
      <form onSubmit={onSubmit} noValidate>
        <Field label="Nome" error={!nome.trim() ? erro : undefined}>
          <TextInput value={nome} onChange={(event) => setNome(event.target.value)} />
        </Field>
        <Field label="Descrição">
          <TextInput value={descricao} onChange={(event) => setDescricao(event.target.value)} />
        </Field>
        <Field label="Município">
          <TextInput value={municipio} onChange={(event) => setMunicipio(event.target.value)} />
        </Field>
        <Field label="Estado">
          <TextInput value={estado} maxLength={2} onChange={(event) => setEstado(event.target.value)} />
        </Field>
        <Field label="Área em hectares">
          <TextInput inputMode="decimal" value={area} onChange={(event) => setArea(event.target.value)} />
        </Field>
        <Field label="Latitude">
          <TextInput inputMode="decimal" value={latitude} onChange={(event) => setLatitude(event.target.value)} />
        </Field>
        <Field label="Longitude">
          <TextInput inputMode="decimal" value={longitude} onChange={(event) => setLongitude(event.target.value)} />
        </Field>
        {erro && nome.trim() && <p className="mb-4 text-base text-perigo">{erro}</p>}
        <div className="flex gap-2">
          <Button type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? 'Salvando…' : 'Criar fazenda'}
          </Button>
          <Button type="button" variant="quiet" onClick={onClose} disabled={mutation.isPending}>
            Cancelar
          </Button>
        </div>
      </form>
    </Modal>
  )
}

function lerNumero(value: string): number | null {
  const texto = value.trim().replace(',', '.')
  if (!texto) return null
  const numero = Number(texto)
  return Number.isFinite(numero) ? numero : Number.NaN
}
