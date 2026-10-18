export function formatarHoras(horas) {
  if (horas < 1) return `${Math.round(horas * 60)} min`
  return `${horas.toFixed(1)} h`
}

/** Classe CSS do selo de nota do Metacritic, nas mesmas cores do site oficial. */
export function corMetacritic(nota) {
  if (nota >= 75) return 'detalhes-jogo-metacritic-alta'
  if (nota >= 50) return 'detalhes-jogo-metacritic-media'
  return 'detalhes-jogo-metacritic-baixa'
}

/** "120.5 h" ou, se o jogo tiver conquistas, "120.5 h • 45/80 conquistas". */
export function formatarProgresso(jogo) {
  const horas = formatarHoras(jogo.horasJogadas)
  if (!jogo.conquistasTotais) return horas
  return `${horas} • ${jogo.conquistasObtidas}/${jogo.conquistasTotais} conquistas`
}
