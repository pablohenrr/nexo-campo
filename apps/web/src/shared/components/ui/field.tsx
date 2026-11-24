import type { InputHTMLAttributes, ReactNode } from 'react'

export function Field({
  label,
  hint,
  error,
  children,
}: {
  label: string
  hint?: string
  error?: string
  children: ReactNode
}) {
  return (
    <label className="mb-6 block text-base text-tinta">
      {label}
      <span className="mt-2 block">{children}</span>
      {hint && <span className="mt-2 block font-normal text-nevoa">{hint}</span>}
      {error && <span className="mt-2 block text-perigo">{error}</span>}
    </label>
  )
}

export function TextInput(props: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      {...props}
      className="h-12 w-full rounded-controle border border-linha bg-orvalho px-3 text-base text-tinta"
    />
  )
}
