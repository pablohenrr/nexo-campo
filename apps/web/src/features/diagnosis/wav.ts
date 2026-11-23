const taxaDestino = 16_000

/** Converte a gravação do navegador para WAV PCM, o formato que a API aceita. */
export async function audioParaWav(blob: Blob): Promise<File> {
  if (blob.size === 0) throw new Error('vazio')
  const contexto = new AudioContext()
  try {
    await contexto.resume()
    const decodificado = await contexto.decodeAudioData(await blob.arrayBuffer())
    const amostras = Math.max(1, Math.ceil(decodificado.duration * taxaDestino))
    const offline = new OfflineAudioContext(1, amostras, taxaDestino)
    const fonte = offline.createBufferSource()
    fonte.buffer = decodificado
    fonte.connect(offline.destination)
    fonte.start()
    const renderizado = await offline.startRendering()
    const canal = renderizado.getChannelData(0)
    return new File([codificarWav(canal, taxaDestino)], 'relato.wav', { type: 'audio/wav' })
  } finally {
    await contexto.close()
  }
}

export function codificarWav(canal: Float32Array, taxa: number): Blob {
  const dados = canal.length * 2
  const arquivo = new ArrayBuffer(44 + dados)
  const vista = new DataView(arquivo)
  escrever(vista, 0, 'RIFF')
  vista.setUint32(4, 36 + dados, true)
  escrever(vista, 8, 'WAVE')
  escrever(vista, 12, 'fmt ')
  vista.setUint32(16, 16, true)
  vista.setUint16(20, 1, true)
  vista.setUint16(22, 1, true)
  vista.setUint32(24, taxa, true)
  vista.setUint32(28, taxa * 2, true)
  vista.setUint16(32, 2, true)
  vista.setUint16(34, 16, true)
  escrever(vista, 36, 'data')
  vista.setUint32(40, dados, true)
  let offset = 44
  for (const amostra of canal) {
    const limitada = Math.max(-1, Math.min(1, amostra))
    vista.setInt16(offset, limitada < 0 ? limitada * 0x8000 : limitada * 0x7fff, true)
    offset += 2
  }
  return new Blob([arquivo], { type: 'audio/wav' })
}

function escrever(vista: DataView, offset: number, texto: string) {
  for (let indice = 0; indice < texto.length; indice += 1) {
    vista.setUint8(offset + indice, texto.charCodeAt(indice))
  }
}
