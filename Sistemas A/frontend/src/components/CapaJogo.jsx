import { useEffect, useState } from 'react'

// A Steam guarda imagens de capa em alta resolucao no CDN, indexadas só pelo
// appId (sem precisar de chamada de API) - bem melhores que o icone de
// 32x32 que a sincronização da biblioteca guarda. Nem todo jogo
// (principalmente os mais antigos/obscuros) tem a capa vertical, então cai
// pro header e depois pro icone pequeno se a imagem de maior qualidade não
// existir. Funciona em qualquer tamanho exibido (lista pequena ou painel de
// detalhes) - uma imagem maior escalada pra baixo sempre fica mais nítida
// que uma pequena escalada pra cima.
function fontesDeCapa(jogo) {
  return [
    `https://cdn.akamai.steamstatic.com/steam/apps/${jogo.appId}/library_600x900.jpg`,
    `https://cdn.akamai.steamstatic.com/steam/apps/${jogo.appId}/header.jpg`,
    jogo.imagem || 'https://placehold.co/300x450?text=%20',
  ]
}

export default function CapaJogo({ jogo, className }) {
  const fontes = fontesDeCapa(jogo)
  const [indice, setIndice] = useState(0)

  useEffect(() => setIndice(0), [jogo.appId])

  return (
    <img
      src={fontes[indice]}
      alt={jogo.nome}
      className={className}
      onError={() => setIndice((atual) => Math.min(atual + 1, fontes.length - 1))}
    />
  )
}
