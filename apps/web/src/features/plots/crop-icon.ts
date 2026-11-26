import { createElement, type ComponentType, type SVGProps } from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { Cana } from '@/features/plots/cana-icon.tsx'
import { Apple, Bean, Beef, Carrot, Citrus, Coffee, Egg, Fish, Grape, Milk, Sprout, Trees, Wheat } from 'lucide-react'

type Icone = ComponentType<SVGProps<SVGSVGElement> & { size?: number; strokeWidth?: number }>

/** Palavras do campo "Cultura" e o ícone de cada uma. A primeira que aparecer no texto vence. */
const regras: Array<[RegExp, Icone]> = [
  [/\b(gado|boi|bois|vaca|bovin|pecu[aá]ria|pasto|pastagem|nelore|corte)/, Beef],
  [/\b(leite|leiteir)/, Milk],
  [/\bcana/, Cana],
  [/\b(soja|feij[aã]o|amendoim|gr[aã]o)/, Bean],
  [/\b(milho|trigo|arroz|sorgo|aveia|cevada)/, Wheat],
  [/\bcaf[eé]/, Coffee],
  [/\b(laranja|lim[aã]o|citr|tangerina)/, Citrus],
  [/\b(uva|vinh)/, Grape],
  [/\b(ma[cç][aã]|fruta|pomar|manga|banana|goiaba)/, Apple],
  [/\b(cenoura|horta|hortali|mandioca|batata|legume)/, Carrot],
  [/\b(eucalipto|pinus|madeira|floresta|mata|reflorest)/, Trees],
  [/\b(frango|galinha|ave|ovos?|granja)/, Egg],
  [/\b(peixe|tilápia|tilapia|piscicult|tanque)/, Fish],
]

export function iconeDaCultura(cultura: string | null | undefined): Icone {
  const texto = (cultura ?? '').toLocaleLowerCase('pt-BR')
  return regras.find(([regra]) => regra.test(texto))?.[1] ?? Sprout
}

/** SVG pronto para marcadores do MapLibre, que são elementos DOM fora do React. */
export function svgDaCultura(cultura: string | null | undefined, size = 22): string {
  const Componente = iconeDaCultura(cultura)
  return renderToStaticMarkup(createElement(Componente, { size, strokeWidth: 2.2, 'aria-hidden': true }))
}
