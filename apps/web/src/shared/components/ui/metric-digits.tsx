export function MetricDigits({ value }: { value: string }) {
  const chars = [...value]
  return (
    <span className="t-number-pop-in is-animating" aria-label={value}>
      {chars.map((char, index) => {
        const fromEnd = chars.length - 1 - index
        const stagger = fromEnd === 0 ? '2' : fromEnd === 1 ? '1' : undefined
        return (
          <span key={`${index}-${char}`} className="t-digit" data-stagger={stagger} aria-hidden="true">
            {char}
          </span>
        )
      })}
    </span>
  )
}
