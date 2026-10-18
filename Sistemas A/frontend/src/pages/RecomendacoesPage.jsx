import { useEffect, useState } from 'react'
import { buscarRecomendacoes, listarContas } from '../lib/api'
import { corMetacritic } from '../lib/formato'
import { mensagemDeErroDeConexao } from '../lib/erros'
import LayoutApp from '../components/LayoutApp'
import CapaJogo from '../components/CapaJogo'

export default function RecomendacoesPage() {
  const [temSteam, setTemSteam] = useState(true)
  const [deUmaChance, setDeUmaChance] = useState([])
  const [recomendacoes, setRecomendacoes] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  useEffect(() => {
    listarContas()
      .then(async (contas) => {
        const contaSteam = contas.find((c) => c.plataforma === 'Steam')
        if (!contaSteam) {
          setTemSteam(false)
          return
        }
        const dados = await buscarRecomendacoes()
        setDeUmaChance(dados.deUmaChance)
        setRecomendacoes(dados.recomendacoes)
      })
      .catch((err) => setErro(mensagemDeErroDeConexao(err, 'Não foi possível carregar as recomendações.')))
      .finally(() => setCarregando(false))
  }, [])

  return (
    <LayoutApp>
      <h1 className="titulo-pagina">Recomendação de Jogos</h1>
      <p className="subtitulo-pagina">
        Baseado nos jogos que você mais jogou - pode levar alguns segundos na primeira vez.
      </p>

      {erro && <p className="mensagem mensagem-erro">{erro}</p>}

      {carregando ? (
        <p className="mensagem">Carregando recomendações...</p>
      ) : !temSteam ? (
        <p className="mensagem">
          Vincule uma conta Steam em "Contas vinculadas" para receber recomendações baseadas na sua biblioteca.
        </p>
      ) : (
        <>
          {deUmaChance.length > 0 && (
            <>
              <h2 className="subtitulo-secao">Dê uma chance</h2>
              <p className="subtitulo-pagina">Jogos que já estão na sua biblioteca, mas você ainda não jogou.</p>
              <div className="grade-recomendacoes">
                {deUmaChance.map((jogo) => (
                  <div key={jogo.appId} className="cartao-recomendacao">
                    <CapaJogo jogo={jogo} className="capa-recomendacao" />
                    <span className="nome-recomendacao">{jogo.nome}</span>
                  </div>
                ))}
              </div>
            </>
          )}

          <h2 className="subtitulo-secao">Recomendações</h2>
          <p className="subtitulo-pagina">Jogos que combinam com o que você mais joga.</p>
          {recomendacoes.length === 0 ? (
            <p className="mensagem">Não encontramos recomendações agora. Tente novamente mais tarde.</p>
          ) : (
            <div className="grade-recomendacoes">
              {recomendacoes.map((jogo) => (
                <a
                  key={jogo.appId}
                  href={`https://store.steampowered.com/app/${jogo.appId}`}
                  target="_blank"
                  rel="noreferrer"
                  className="cartao-recomendacao"
                >
                  <div className="capa-recomendacao-wrapper">
                    <CapaJogo jogo={jogo} className="capa-recomendacao" />
                    {jogo.notaMetacritic != null && (
                      <span className={`detalhes-jogo-metacritic selo-metacritic-recomendacao ${corMetacritic(jogo.notaMetacritic)}`}>
                        {jogo.notaMetacritic}
                      </span>
                    )}
                  </div>
                  <span className="nome-recomendacao">{jogo.nome}</span>
                  <span className="preco-recomendacao">{jogo.gratuito ? 'Grátis' : jogo.precoFormatado}</span>

                  <div className="dica-recomendacao">
                    <div className="dica-recomendacao-cabecalho">
                      <strong>{jogo.nome}</strong>
                      {jogo.notaMetacritic != null && (
                        <span className={`detalhes-jogo-metacritic ${corMetacritic(jogo.notaMetacritic)}`}>
                          {jogo.notaMetacritic}
                        </span>
                      )}
                    </div>
                    {jogo.sinopse && <p className="dica-recomendacao-sinopse">{jogo.sinopse}</p>}
                  </div>
                </a>
              ))}
            </div>
          )}
        </>
      )}
    </LayoutApp>
  )
}
