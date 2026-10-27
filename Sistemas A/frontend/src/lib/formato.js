export function formatarHoras(horas) {
  if (horas < 1) return `${Math.round(horas * 60)} min`
  return `${horas.toFixed(1)} h`
}

/** Silhueta usada quando o usuário não tem foto de perfil (BASE_URL cobre o prefixo /XPV/ do GitHub Pages). */
export const AVATAR_PADRAO = `${import.meta.env.BASE_URL}avatar-padrao.svg`

/** "5 / 5" para notas inteiras e "4.5 / 5" para meias estrelas. */
export function formatarNota(nota) {
  return `${Number.isInteger(nota) ? nota : nota.toFixed(1)} / 5`
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
