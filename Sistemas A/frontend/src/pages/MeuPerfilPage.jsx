import { useEffect, useMemo, useState } from 'react'
import { listarContas, listarDestaques, listarJogos, salvarDestaques } from '../lib/api'
import { formatarHoras } from '../lib/formato'
import LayoutApp from '../components/LayoutApp'
import { IconeConquista100, IconeSteam } from '../components/Icones'

const MAXIMO_DESTAQUES = 2
const MINIMO_PERFECCIONISTA = 2
const MAXIMO_PERFECCIONISTA = 6

const TIPOS = {
  HORAS_PLATAFORMA: 'Horas totais na Steam',
  JOGO_FAVORITO: 'Jogo favorito',
  PERFECCIONISTA: 'Perfeccionista (jogos platinados)',
  MAIS_JOGADOS: 'Mais jogados (top 3)',
}

function jogoEstaPlatinado(jogo) {
  return jogo.conquistasTotais > 0 && jogo.conquistasObtidas === jogo.conquistasTotais
}

export default function MeuPerfilPage() {
  const [destaques, setDestaques] = useState([])
  const [conta, setConta] = useState(null)
  const [jogos, setJogos] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [salvando, setSalvando] = useState(false)

  const [mostrarFormulario, setMostrarFormulario] = useState(false)
  const [tipoNovo, setTipoNovo] = useState('HORAS_PLATAFORMA')
  const [jogoFavoritoAppId, setJogoFavoritoAppId] = useState('')
  const [perfeccionistaAppIds, setPerfeccionistaAppIds] = useState([])

  useEffect(() => {
    Promise.all([listarDestaques(), listarContas()])
      .then(async ([destaquesAtuais, contas]) => {
        setDestaques(destaquesAtuais)
        const contaSteam = contas.find((c) => c.plataforma === 'Steam')
        setConta(contaSteam || null)
        if (contaSteam && contaSteam.bibliotecaPublica === true) {
          setJogos(await listarJogos(contaSteam.id))
        }
      })
      .catch((err) => setErro(err.message || 'Não foi possível carregar seus destaques.'))
      .finally(() => setCarregando(false))
  }, [])

  const jogosPlatinados = useMemo(() => jogos.filter(jogoEstaPlatinado), [jogos])

  function requestsAtuais() {
    return destaques.map((d) => ({
      tipo: d.tipo,
      plataforma: d.plataforma,
      jogos: d.jogos?.map((j) => ({ contaPlataformaId: conta?.id, appId: j.appId })) ?? null,
    }))
  }

  async function persistir(novaLista) {
    setSalvando(true)
    setErro('')
    try {
      const atualizados = await salvarDestaques(novaLista)
      setDestaques(atualizados)
      setMostrarFormulario(false)
      setTipoNovo('HORAS_PLATAFORMA')
      setJogoFavoritoAppId('')
      setPerfeccionistaAppIds([])
    } catch (err) {
      setErro(err.message || 'Não foi possível salvar os destaques.')
    } finally {
      setSalvando(false)
    }
  }

  function handleRemover(index) {
    const novaLista = requestsAtuais().filter((_, i) => i !== index)
    persistir(novaLista)
  }

  function handleAdicionar() {
    const novoRequest = { tipo: tipoNovo, plataforma: null, jogos: null }
    if (tipoNovo === 'HORAS_PLATAFORMA') {
      novoRequest.plataforma = 'Steam'
    } else if (tipoNovo === 'JOGO_FAVORITO') {
      if (!jogoFavoritoAppId) return
      novoRequest.jogos = [{ contaPlataformaId: conta.id, appId: Number(jogoFavoritoAppId) }]
    } else if (tipoNovo === 'PERFECCIONISTA') {
      if (perfeccionistaAppIds.length < MINIMO_PERFECCIONISTA || perfeccionistaAppIds.length > MAXIMO_PERFECCIONISTA) return
      novoRequest.jogos = perfeccionistaAppIds.map((appId) => ({ contaPlataformaId: conta.id, appId: Number(appId) }))
    }
    persistir([...requestsAtuais(), novoRequest])
  }

  function toggleJogoPerfeccionista(appId) {
    setPerfeccionistaAppIds((atual) =>
      atual.includes(appId) ? atual.filter((id) => id !== appId) : [...atual, appId],
    )
  }

  const temSteamComBiblioteca = conta && conta.bibliotecaPublica === true

  return (
    <LayoutApp>
      <h1 className="titulo-pagina">Meu Perfil</h1>
      <p className="subtitulo-pagina">
        Escolha até {MAXIMO_DESTAQUES} destaques para exibir no seu perfil.
      </p>

      {erro && <p className="mensagem mensagem-erro">{erro}</p>}

      {carregando ? (
        <p className="mensagem">Carregando...</p>
      ) : (
        <>
          <div className="lista-destaques">
            {destaques.map((destaque, index) => (
              <CardDestaque key={destaque.id ?? index} destaque={destaque} onRemover={() => handleRemover(index)} />
            ))}
          </div>

          {!temSteamComBiblioteca ? (
            <p className="mensagem">
              Vincule uma conta Steam com biblioteca pública em "Contas vinculadas" para poder escolher destaques.
            </p>
          ) : destaques.length >= MAXIMO_DESTAQUES ? (
            <p className="mensagem">Você já escolheu {MAXIMO_DESTAQUES} destaques. Remova um para escolher outro.</p>
          ) : !mostrarFormulario ? (
            <button type="button" className="botao-secundario" onClick={() => setMostrarFormulario(true)}>
              Adicionar destaque
            </button>
          ) : (
            <div className="formulario-destaque">
              <label className="campo">
                <span>Tipo de destaque</span>
                <select value={tipoNovo} onChange={(e) => setTipoNovo(e.target.value)}>
                  {Object.entries(TIPOS).map(([chave, rotulo]) => (
                    <option key={chave} value={chave}>
                      {rotulo}
                    </option>
                  ))}
                </select>
              </label>

              {tipoNovo === 'JOGO_FAVORITO' && (
                <label className="campo">
                  <span>Jogo</span>
                  <select value={jogoFavoritoAppId} onChange={(e) => setJogoFavoritoAppId(e.target.value)}>
                    <option value="">Escolha um jogo</option>
                    {jogos.map((jogo) => (
                      <option key={jogo.appId} value={jogo.appId}>
                        {jogo.nome}
                      </option>
                    ))}
                  </select>
                </label>
              )}

              {tipoNovo === 'PERFECCIONISTA' && (
                <div className="campo">
                  <span>
                    Jogos platinados ({perfeccionistaAppIds.length}/{MAXIMO_PERFECCIONISTA}, mínimo {MINIMO_PERFECCIONISTA})
                  </span>
                  {jogosPlatinados.length === 0 ? (
                    <p className="mensagem">Você ainda não tem jogos 100% platinados.</p>
                  ) : (
                    <div className="lista-selecao-jogos">
                      {jogosPlatinados.map((jogo) => (
                        <label key={jogo.appId} className="opcao-jogo">
                          <input
                            type="checkbox"
                            checked={perfeccionistaAppIds.includes(String(jogo.appId))}
                            onChange={() => toggleJogoPerfeccionista(String(jogo.appId))}
                          />
                          {jogo.nome}
                        </label>
                      ))}
                    </div>
                  )}
                </div>
              )}

              <div className="acoes-formulario-destaque">
                <button type="button" className="botao-principal" onClick={handleAdicionar} disabled={salvando}>
                  {salvando ? 'Salvando...' : 'Confirmar'}
                </button>
                <button type="button" className="botao-secundario" onClick={() => setMostrarFormulario(false)}>
                  Cancelar
                </button>
              </div>
            </div>
          )}
        </>
      )}
    </LayoutApp>
  )
}

function CardDestaque({ destaque, onRemover }) {
  return (
    <div className="cartao-destaque">
      <div className="cabecalho-cartao-destaque">
        <strong>{TIPOS[destaque.tipo]}</strong>
        <button type="button" className="botao-remover-destaque" onClick={onRemover} aria-label="Remover destaque">
          ×
        </button>
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
