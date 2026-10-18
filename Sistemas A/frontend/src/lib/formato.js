export function formatarHoras(horas) {
  if (horas < 1) return `${Math.round(horas * 60)} min`
  return `${horas.toFixed(1)} h`
}

/** "120.5 h" ou, se o jogo tiver conquistas, "120.5 h • 45/80 conquistas". */
export function formatarProgresso(jogo) {
  const horas = formatarHoras(jogo.horasJogadas)
  if (!jogo.conquistasTotais) return horas
  return `${horas} • ${jogo.conquistasObtidas}/${jogo.conquistasTotais} conquistas`
}
