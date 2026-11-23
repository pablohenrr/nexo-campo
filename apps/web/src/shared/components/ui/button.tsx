import type { ButtonHTMLAttributes } from 'react'
import { cn } from '@/shared/lib/cn.ts'

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'solid' | 'quiet'
}

export function Button({ variant = 'solid', className, type = 'button', ...props }: ButtonProps) {
  return (
    <button
      type={type}
      className={cn(
        'inline-flex min-h-12 items-center justify-center rounded-full border px-5 text-base font-medium transition-transform duration-150 enabled:hover:-translate-y-px enabled:active:translate-y-0 disabled:opacity-50',
        variant === 'solid'
          ? 'border-chuva bg-chuva text-cerca'
          : 'border-linha bg-transparent text-tinta',
        className,
      )}
      {...props}
    />
  )
}
