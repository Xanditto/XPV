import { useEffect, useMemo, useState } from 'react'
import { buscarDetalhesLojaDoJogo, listarContas, listarJogos, sincronizarJogos } from '../lib/api'
import { corMetacritic, formatarHoras } from '../lib/formato'
import { mensagemDeErroDeConexao } from '../lib/erros'
import { useFiltroJogos } from '../lib/useFiltroJogos'
import LayoutApp from '../components/LayoutApp'
import CapaJogo from '../components/CapaJogo'
import { IconeConquista100, IconeSteam } from '../components/Icones'

function porcentagemConquistas(jogo) {
  if (jogo.conquistasTotais === 0) return -1
  return jogo.conquistasObtidas / jogo.conquistasTotais
}

function dataOuMinimo(dataIso) {
  return dataIso ? new Date(dataIso).getTime() : -Infinity
}

// Toda comparação aqui é "crescente" (a menor/mais antigo primeiro) - a
// direção escolhida pela pessoa (crescente/decrescente) inverte o resultado
// por fora, então não precisa duplicar a lógica de ordenação pros dois lados.
const ORDENACOES = {
  horas: {
    rotulo: 'Horas jogadas',
    comparar: (a, b) => a.horasJogadas - b.horasJogadas,
    direcaoPadrao: 'desc',
  },
  conquistas: {
    rotulo: 'Porcentagem de conquistas',
    comparar: (a, b) => porcentagemConquistas(a) - porcentagemConquistas(b),
    direcaoPadrao: 'desc',
  },
  ultimoAcesso: {
    rotulo: 'Última vez jogado',
    comparar: (a, b) => dataOuMinimo(a.ultimoAcesso) - dataOuMinimo(b.ultimoAcesso),
    direcaoPadrao: 'desc',
  },
  alfabetica: {
    rotulo: 'Ordem alfabética',
    comparar: (a, b) => a.nome.localeCompare(b.nome, 'pt-BR'),
    direcaoPadrao: 'asc',
  },
}

function formatarData(dataIso) {
  return new Date(dataIso).toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' })
}

const ROTULOS_SISTEMA = {
  horasWindows: 'Windows',
  horasMac: 'Mac',
  horasLinux: 'Linux',
  horasDeck: 'Steam Deck',
}

export default function BibliotecaPage() {
  const [conta, setConta] = useState(null)
  const [jogos, setJogos] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [ordenacao, setOrdenacao] = useState('horas')
  const [direcao, setDirecao] = useState(ORDENACOES.horas.direcaoPadrao)
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
      .catch((err) => setErro(mensagemDeErroDeConexao(err, 'Não foi possível carregar a biblioteca.')))
      .finally(() => setCarregando(false))
  }, [])

  const [filtroJogo, setFiltroJogo, jogosFiltrados] = useFiltroJogos(jogos)

  const jogosOrdenados = useMemo(() => {
    const ordenados = [...jogosFiltrados].sort(ORDENACOES[ordenacao].comparar)
    return direcao === 'asc' ? ordenados : ordenados.reverse()
  }, [jogosFiltrados, ordenacao, direcao])

  function handleTrocarOrdenacao(chave) {
    setOrdenacao(chave)
    setDirecao(ORDENACOES[chave].direcaoPadrao)
  }

  const jogoSelecionado = jogos.find((jogo) => jogo.appId === jogoSelecionadoAppId) || null

  return (
    <LayoutApp>
      <div className="cabecalho-biblioteca">
        <h1 className="titulo-pagina">Biblioteca de Jogos</h1>
        {jogos.length > 0 && (
          <div className="controles-biblioteca">
            <input
              type="text"
              className="campo-busca-biblioteca"
              value={filtroJogo}
              onChange={(e) => setFiltroJogo(e.target.value)}
              placeholder="Buscar jogo pelo nome..."
            />
            <label className="campo-ordenacao">
              <span>Ordenar por</span>
              <select value={ordenacao} onChange={(e) => handleTrocarOrdenacao(e.target.value)}>
                {Object.entries(ORDENACOES).map(([chave, { rotulo }]) => (
                  <option key={chave} value={chave}>
                    {rotulo}
                  </option>
                ))}
              </select>
            </label>
            <button
              type="button"
              className="botao-direcao-ordenacao"
              onClick={() => setDirecao((atual) => (atual === 'asc' ? 'desc' : 'asc'))}
              title={direcao === 'asc' ? 'Ordem crescente' : 'Ordem decrescente'}
              aria-label={direcao === 'asc' ? 'Ordem crescente' : 'Ordem decrescente'}
            >
              {direcao === 'asc' ? '↑' : '↓'}
            </button>
          </div>
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
                      <CapaJogo jogo={jogo} className="icone-jogo" />
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
                  <DetalhesJogo jogo={jogoSelecionado} contaId={conta.id} />
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

function DetalhesJogo({ jogo, contaId }) {
  const completo = jogo.conquistasTotais > 0 && jogo.conquistasObtidas === jogo.conquistasTotais
  const [detalhesLoja, setDetalhesLoja] = useState(null)
  const [carregandoLoja, setCarregandoLoja] = useState(true)
  const [erroLoja, setErroLoja] = useState('')

  useEffect(() => {
    setDetalhesLoja(null)
    setErroLoja('')
    setCarregandoLoja(true)
    buscarDetalhesLojaDoJogo(contaId, jogo.appId)
      .then(setDetalhesLoja)
      .catch((err) => setErroLoja(mensagemDeErroDeConexao(err, 'Não foi possível carregar os detalhes da loja.')))
      .finally(() => setCarregandoLoja(false))
  }, [contaId, jogo.appId])

  const horasPorSistema = Object.entries(ROTULOS_SISTEMA).filter(([campo]) => jogo[campo] > 0)

  return (
    <div className="detalhes-jogo-wrapper">
      <div className="detalhes-jogo">
        <CapaJogo jogo={jogo} className="detalhes-jogo-capa" />
        <div className="detalhes-jogo-info">
          <div className="detalhes-jogo-cabecalho">
            <h2 className="detalhes-jogo-nome">
              <IconeSteam />
              {jogo.nome}
            </h2>
            {detalhesLoja?.notaMetacritic != null && (
              <span className={`detalhes-jogo-metacritic ${corMetacritic(detalhesLoja.notaMetacritic)}`}>
                {detalhesLoja.notaMetacritic}
              </span>
            )}
          </div>
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
            {jogo.ultimoAcesso && (
              <div className="detalhes-jogo-estatistica">
                <span className="detalhes-jogo-estatistica-rotulo">Última vez jogado</span>
                <span className="detalhes-jogo-estatistica-valor">{formatarData(jogo.ultimoAcesso)}</span>
              </div>
            )}
          </div>

          {horasPorSistema.length > 1 && (
            <div className="detalhes-jogo-horas-sistema">
              {horasPorSistema.map(([campo, rotulo]) => (
                <span key={campo} className="detalhes-jogo-chip">
                  {rotulo}: {formatarHoras(jogo[campo])}
                </span>
              ))}
            </div>
          )}

          {detalhesLoja && (detalhesLoja.desenvolvedoras?.length > 0 || detalhesLoja.publicadoras?.length > 0 || detalhesLoja.dataLancamento) && (
            <div className="detalhes-jogo-meta">
              {detalhesLoja.desenvolvedoras?.length > 0 && (
                <div className="detalhes-jogo-meta-item">
                  <span className="detalhes-jogo-estatistica-rotulo">Desenvolvedora</span>
                  <span>{detalhesLoja.desenvolvedoras.join(', ')}</span>
                </div>
              )}
              {detalhesLoja.publicadoras?.length > 0 && (
                <div className="detalhes-jogo-meta-item">
                  <span className="detalhes-jogo-estatistica-rotulo">Publicadora</span>
                  <span>{detalhesLoja.publicadoras.join(', ')}</span>
                </div>
              )}
              {detalhesLoja.dataLancamento && (
                <div className="detalhes-jogo-meta-item">
                  <span className="detalhes-jogo-estatistica-rotulo">Lançamento</span>
                  <span>{detalhesLoja.dataLancamento}</span>
                </div>
              )}
            </div>
          )}

          {detalhesLoja && (detalhesLoja.generos?.length > 0 || detalhesLoja.categorias?.length > 0) && (
            <div className="detalhes-jogo-tags">
              {detalhesLoja.generos?.map((genero) => (
                <span key={`genero-${genero.id}`} className="detalhes-jogo-tag">
                  {genero.nome}
                </span>
              ))}
              {detalhesLoja.categorias?.map((categoria) => (
                <span key={`categoria-${categoria.id}`} className="detalhes-jogo-tag detalhes-jogo-tag-categoria">
                  {categoria.nome}
                </span>
              ))}
            </div>
          )}
        </div>
      </div>

      {carregandoLoja ? (
        <p className="mensagem">Carregando informações da loja...</p>
      ) : erroLoja ? (
        <p className="mensagem mensagem-erro">{erroLoja}</p>
      ) : detalhesLoja ? (
        <div className="detalhes-jogo-loja">
          {detalhesLoja.descricao && <p className="detalhes-jogo-descricao">{detalhesLoja.descricao}</p>}

          {detalhesLoja.capturas?.length > 0 && (
            <div className="detalhes-jogo-capturas">
              {detalhesLoja.capturas.map((captura, i) => (
                <a key={i} href={captura.completa} target="_blank" rel="noreferrer">
                  <img src={captura.miniatura} alt={`Captura de tela ${i + 1}`} className="detalhes-jogo-captura" />
                </a>
              ))}
            </div>
          )}
        </div>
      ) : null}
    </div>
  )
}
