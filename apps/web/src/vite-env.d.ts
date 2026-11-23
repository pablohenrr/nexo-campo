interface ImportMetaEnv {
  readonly VITE_MAPTILER_KEY?: string
  readonly VITE_API_ORIGIN?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
