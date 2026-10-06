import { useEffect, useMemo, useState } from 'react'
import { listarContas, listarJogos, sincronizarJogos } from '../lib/api'
import { formatarHoras } from '../lib/formato'
import LayoutApp from '../components/LayoutApp'
import { IconeConquista100, IconeSteam } from '../components/Icones'

function porcentagemConquistas(jogo) {
  if (jogo.conquistasTotais === 0) return -1
  return jogo.conquistasObtidas / jogo.conquistasTotais
}

const ORDENACOES = {
  horas: {
    rotulo: 'Horas jogadas',
    comparar: (a, b) => b.horasJogadas - a.horasJogadas,
  },
  conquistas: {
    rotulo: 'Porcentagem de conquistas',
    comparar: (a, b) => porcentagemConquistas(b) - porcentagemConquistas(a),
  },
  alfabetica: {
    rotulo: 'Ordem alfabética',
    comparar: (a, b) => a.nome.localeCompare(b.nome, 'pt-BR'),
  },
}

export default function BibliotecaPage() {
  const [conta, setConta] = useState(null)
  const [jogos, setJogos] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [ordenacao, setOrdenacao] = useState('horas')
  const [jogoSelecionadoAppId, setJogoSelecionadoAppId] = useState(null)

  useEffect(() => {
    listarContas()
      .then(async (contas) => {
        const contaSteam = contas.find((c) => c.plataforma === 'Steam')
        setConta(contaSteam || null)
        if (!contaSteam || contaSteam.bibliotecaPublica !== true) {
          return []
        }
        const jogosAtuais = await listarJogos(contaSteam.id)
        // Se a biblioteca já deveria ter jogos mas ainda não sincronizamos
        // nenhum, sincroniza automaticamente uma vez (sem precisar de botão).
        if (jogosAtuais.length === 0) {
          return sincronizarJogos(contaSteam.id)
        }
        return jogosAtuais
      })
      .then((jogosCarregados) => {
        setJogos(jogosCarregados)
        if (jogosCarregados.length > 0) {
          setJogoSelecionadoAppId(jogosCarregados[0].appId)
        }
      })
      .catch((err) => setErro(err.message || 'Não foi possível carregar a biblioteca.'))
      .finally(() => setCarregando(false))
  }, [])

  const jogosOrdenados = useMemo(
    () => [...jogos].sort(ORDENACOES[ordenacao].comparar),
    [jogos, ordenacao],
  )

  const jogoSelecionado = jogos.find((jogo) => jogo.appId === jogoSelecionadoAppId) || null

  return (
    <LayoutApp>
      <div className="cabecalho-biblioteca">
        <h1 className="titulo-pagina">Biblioteca de Jogos</h1>
        {jogos.length > 0 && (
          <label className="campo-ordenacao">
            <span>Ordenar por</span>
            <select value={ordenacao} onChange={(e) => setOrdenacao(e.target.value)}>
              {Object.entries(ORDENACOES).map(([chave, { rotulo }]) => (
                <option key={chave} value={chave}>
                  {rotulo}
                </option>
              ))}
            </select>
          </label>
        )}
      </div>

      {carregando ? (
        <p className="mensagem">Carregando...</p>
      ) : !conta ? (
        <p className="mensagem">
          Você ainda não vinculou uma conta Steam. Vincule uma em "Contas vinculadas" para ver sua biblioteca.
        </p>
      ) : conta.bibliotecaPublica !== true ? (
        <p className="mensagem mensagem-erro">
          Sua biblioteca de jogos Steam está privada, então não conseguimos exibi-la aqui. Torne-a
          pública em "Detalhes do jogo", nas configurações de privacidade da Steam.
        </p>
      ) : (
        <>
          <p className="subtitulo-pagina">
            {jogos.length > 0 ? `${jogos.length} jogos.` : 'Seus jogos da Steam.'}
          </p>

          {erro && <p className="mensagem mensagem-erro">{erro}</p>}

          {jogosOrdenados.length === 0 ? (
            <p className="mensagem">Nenhum jogo encontrado.</p>
          ) : (
            <div className="biblioteca-layout">
              <div className="lista-jogos">
                {jogosOrdenados.map((jogo) => {
                  const completo = jogo.conquistasTotais > 0 && jogo.conquistasObtidas === jogo.conquistasTotais
                  return (
                    <button
                      key={jogo.appId}
                      type="button"
                      className={`linha-jogo ${jogo.appId === jogoSelecionadoAppId ? 'linha-jogo-selecionada' : ''}`}
                      onClick={() => setJogoSelecionadoAppId(jogo.appId)}
                    >
                      <img
                        src={jogo.imagem || 'https://placehold.co/32x32?text=%20'}
                        alt={jogo.nome}
                        className="icone-jogo"
                      />
                      <span className="nome-jogo">
                        <IconeSteam />
                        {jogo.nome}
                      </span>
                      {jogo.conquistasTotais > 0 && (
                        <span className={`conquistas-jogo ${completo ? 'conquistas-completo' : 'conquistas-incompleto'}`}>
                          {completo && <IconeConquista100 />}
                          {jogo.conquistasObtidas}/{jogo.conquistasTotais}
                        </span>
                      )}
                      <span className="horas-jogo">{formatarHoras(jogo.horasJogadas)}</span>
                    </button>
                  )
                })}
              </div>

              <div className="painel-detalhes-jogo">
                {jogoSelecionado ? (
                  <DetalhesJogo jogo={jogoSelecionado} />
                ) : (
                  <p className="mensagem">Selecione um jogo para ver os detalhes.</p>
                )}
              </div>
            </div>
          )}
        </>
      )}

      {erro && !conta && <p className="mensagem mensagem-erro">{erro}</p>}
    </LayoutApp>
  )
}

function DetalhesJogo({ jogo }) {
  const completo = jogo.conquistasTotais > 0 && jogo.conquistasObtidas === jogo.conquistasTotais

  return (
    <div className="detalhes-jogo">
      <img
        src={jogo.imagem || 'https://placehold.co/160x160?text=%20'}
        alt={jogo.nome}
        className="detalhes-jogo-capa"
      />
      <h2 className="detalhes-jogo-nome">
        <IconeSteam />
        {jogo.nome}
      </h2>
      <div className="detalhes-jogo-estatisticas">
        <div className="detalhes-jogo-estatistica">
          <span className="detalhes-jogo-estatistica-rotulo">Horas jogadas</span>
          <span className="detalhes-jogo-estatistica-valor detalhes-jogo-estatistica-valor-horas">
            {formatarHoras(jogo.horasJogadas)}
          </span>
        </div>
        {jogo.conquistasTotais > 0 && (
          <div className="detalhes-jogo-estatistica">
            <span className="detalhes-jogo-estatistica-rotulo">Conquistas</span>
            <span
              className={`detalhes-jogo-estatistica-valor ${completo ? 'conquistas-completo' : 'conquistas-incompleto'}`}
            >
              {completo && <IconeConquista100 />}
              {jogo.conquistasObtidas}/{jogo.conquistasTotais}
            </span>
          </div>
        )}
      </div>
    </div>
  )
}
