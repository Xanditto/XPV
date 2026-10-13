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
