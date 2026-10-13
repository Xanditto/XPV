import { useMemo, useState } from 'react'

/** Filtra uma lista de jogos pelo nome digitado, usado em todo seletor de jogo com busca. */
export function useFiltroJogos(jogos) {
  const [filtro, setFiltro] = useState('')
  const filtrados = useMemo(
    () => jogos.filter((jogo) => jogo.nome.toLowerCase().includes(filtro.toLowerCase())),
    [jogos, filtro],
  )
  return [filtro, setFiltro, filtrados]
}
