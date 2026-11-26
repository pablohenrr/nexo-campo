import { useEffect, useRef, type ReactNode } from 'react'
import { cn } from '@/shared/lib/cn.ts'

export function Modal({
  open,
  title,
  onClose,
  children,
  className,
}: {
  open: boolean
  title: string
  onClose: () => void
  children: ReactNode
  className?: string
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
      className={cn('t-resize', className)}
      onClose={onClose}
    >
      <h2 id="modal-titulo" className="mb-4 text-xl">
        {title}
      </h2>
      {children}
    </dialog>
  )
}
