import { formatarHoras } from '../lib/formato'
import { IconeConquista100, IconeSteam } from './Icones'

export const MINIMO_PERFECCIONISTA = 2
export const MAXIMO_PERFECCIONISTA = 6
export const MINIMO_CONQUISTAS_ESPECIFICAS = 1
export const MAXIMO_CONQUISTAS_ESPECIFICAS = 20
export const MAXIMO_TAMANHO_TEXTO = 500

export const TIPOS_DESTAQUE = {
  HORAS_PLATAFORMA: 'Horas totais na Steam',
  JOGO_FAVORITO: 'Jogo favorito',
  PERFECCIONISTA: 'Perfeccionista (jogos platinados)',
  MAIS_JOGADOS: 'Mais jogados (top 3)',
  TEMPO_POR_PLATAFORMA: 'Tempo por plataforma (top 3)',
  IMAGEM_PERSONALIZADA: 'Imagem personalizada',
  CAIXA_TEXTO: 'Caixa de texto',
  CONQUISTAS_ESPECIFICAS: 'Conquistas específicas',
}

/** Quantos destaques de cada tipo podem coexistir no perfil (a maioria permite só 1). */
export const LIMITE_POR_TIPO = {
  HORAS_PLATAFORMA: 1,
  JOGO_FAVORITO: 1,
  MAIS_JOGADOS: 1,
  TEMPO_POR_PLATAFORMA: 1,
  PERFECCIONISTA: 2,
  CONQUISTAS_ESPECIFICAS: 2,
  IMAGEM_PERSONALIZADA: 3,
  CAIXA_TEXTO: 3,
}

export function jogoEstaPlatinado(jogo) {
  return jogo.conquistasTotais > 0 && jogo.conquistasObtidas === jogo.conquistasTotais
}

/**
 * Card de um destaque. Passe onRemover/onEditar para exibir os botões de
 * remover/editar (modo edição); omita ambos para exibição somente leitura.
 */
export function CardDestaque({ destaque, onRemover, onEditar }) {
  return (
    <div className="cartao-destaque">
      <div className="cabecalho-cartao-destaque">
        <strong>{TIPOS_DESTAQUE[destaque.tipo]}</strong>
        <div className="acoes-cartao-destaque">
          {onEditar && (
            <button type="button" className="botao-editar-destaque" onClick={onEditar}>
              Editar
            </button>
          )}
          {onRemover && (
            <button type="button" className="botao-remover-destaque" onClick={onRemover} aria-label="Remover destaque">
              ×
            </button>
          )}
        </div>
      </div>
      <ConteudoDestaque destaque={destaque} />
    </div>
  )
}

function ConteudoDestaque({ destaque }) {
  if (destaque.tipo === 'HORAS_PLATAFORMA') {
    return (
      <div className="conteudo-destaque-horas">
        <IconeSteam />
        <span className="numero-destaque">{formatarHoras(destaque.horasTotais ?? 0)}</span>
      </div>
    )
  }

  if (destaque.tipo === 'TEMPO_POR_PLATAFORMA') {
    return (
      <ol className="lista-destaque-jogos lista-ranking">
        {(destaque.temposPorPlataforma ?? []).map((tempo, i) => (
          <li key={tempo.plataforma} className="linha-destaque-jogo">
            <span className="posicao-ranking">#{i + 1}</span>
            <span className="nome-jogo-destaque">{tempo.plataforma}</span>
            <span className="horas-jogo">{formatarHoras(tempo.horasTotais)}</span>
          </li>
        ))}
      </ol>
    )
  }

  if (destaque.tipo === 'IMAGEM_PERSONALIZADA') {
    return (
      <div className="conteudo-destaque-imagem">
        <img src={destaque.imagem} alt="Imagem do destaque" className="imagem-destaque-grande" />
      </div>
    )
  }

  if (destaque.tipo === 'CAIXA_TEXTO') {
    return <p className="conteudo-destaque-texto">{destaque.texto}</p>
  }

  if (destaque.tipo === 'CONQUISTAS_ESPECIFICAS') {
    return (
      <div className="lista-destaque-jogos">
        {(destaque.conquistas ?? []).map((conquista, i) => (
          <div key={`${conquista.appId}-${i}`} className="linha-destaque-jogo">
            <img
              src={conquista.conquistaIcone || 'https://placehold.co/32x32?text=%20'}
              alt={conquista.conquistaNome}
              className="icone-jogo"
            />
            <span className="nome-jogo-destaque">
              {conquista.conquistaNome}
              <span className="jogo-da-conquista"> — {conquista.jogoNome}</span>
            </span>
          </div>
        ))}
      </div>
    )
  }

  if (destaque.tipo === 'MAIS_JOGADOS') {
    return (
      <ol className="lista-destaque-jogos lista-ranking">
        {(destaque.jogos ?? []).map((jogo, i) => (
          <li key={jogo.appId} className="linha-destaque-jogo">
            <span className="posicao-ranking">#{i + 1}</span>
            <img src={jogo.imagem || 'https://placehold.co/32x32?text=%20'} alt={jogo.nome} className="icone-jogo" />
            <span className="nome-jogo-destaque">{jogo.nome}</span>
            <span className="horas-jogo">{formatarHoras(jogo.horasJogadas)}</span>
          </li>
        ))}
      </ol>
    )
  }

  // JOGO_FAVORITO e PERFECCIONISTA
  return (
    <div className="lista-destaque-jogos">
      {(destaque.jogos ?? []).map((jogo) => {
        const completo = jogo.conquistasTotais > 0 && jogo.conquistasObtidas === jogo.conquistasTotais
        return (
          <div key={jogo.appId} className="linha-destaque-jogo">
            <img src={jogo.imagem || 'https://placehold.co/32x32?text=%20'} alt={jogo.nome} className="icone-jogo" />
            <span className="nome-jogo-destaque">{jogo.nome}</span>
            {jogo.conquistasTotais > 0 && (
              <span className={`conquistas-jogo ${completo ? 'conquistas-completo' : 'conquistas-incompleto'}`}>
                {completo && <IconeConquista100 />}
                {jogo.conquistasObtidas}/{jogo.conquistasTotais}
              </span>
            )}
            <span className="horas-jogo">{formatarHoras(jogo.horasJogadas)}</span>
          </div>
        )
      })}
    </div>
  )
}
