import type { SVGProps } from 'react'

/** Cana-de-açúcar: três colmos com nós e folhas. O pacote de ícones não tem um desenho próprio. */
export function Cana(props: SVGProps<SVGSVGElement> & { size?: number; strokeWidth?: number }) {
  const { size = 24, strokeWidth = 2, ...resto } = props
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={strokeWidth}
      strokeLinecap="round"
      strokeLinejoin="round"
      {...resto}
    >
      <path d="M7 22V9" />
      <path d="M12 22V5" />
      <path d="M17 22V10" />
      <path d="M6 16h2M6 12h2M11 17h2M11 12h2M11 8h2M16 17h2M16 13h2" />
      <path d="M12 5c-1-2-3-3-5-3 1 2 3 3 5 3z" />
      <path d="M12 5c1-2 3-3 5-3-1 2-3 3-5 3z" />
    </svg>
  )
}
