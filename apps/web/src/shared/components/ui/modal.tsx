import { useEffect, useRef, type ReactNode } from 'react'

export function Modal({
  open,
  title,
  onClose,
  children,
}: {
  open: boolean
  title: string
  onClose: () => void
  children: ReactNode
}) {
  const ref = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const dialog = ref.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])

  return (
    <dialog
      ref={ref}
      aria-labelledby="modal-titulo"
      className="t-resize"
      onClose={onClose}
    >
      <h2 id="modal-titulo" className="mb-4 text-xl">
        {title}
      </h2>
      {children}
    </dialog>
  )
}
