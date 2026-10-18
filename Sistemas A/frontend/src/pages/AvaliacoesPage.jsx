import { useEffect, useMemo, useState } from 'react'
import {
  listarAvaliacoes,
  listarContas,
  listarJogos,
  removerAvaliacao,
  salvarAvaliacao,
} from '../lib/api'
import { formatarProgresso } from '../lib/formato'
import { useFiltroJogos } from '../lib/useFiltroJogos'
import LayoutApp from '../components/LayoutApp'
import Modal from '../components/Modal'
import CapaJogo from '../components/CapaJogo'
import SeletorEstrelas from '../components/SeletorEstrelas'
import { IconeSteam } from '../components/Icones'

export default function AvaliacoesPage() {
  const [conta, setConta] = useState(null)
  const [jogos, setJogos] = useState([])
  const [avaliacoes, setAvaliacoes] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  const [modalAberto, setModalAberto] = useState(false)
  const [editandoId, setEditandoId] = useState(null)
  const [jogoAppId, setJogoAppId] = useState('')
  const [nota, setNota] = useState(0)
  const [resenha, setResenha] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [erroModal, setErroModal] = useState('')

  useEffect(() => {
    listarContas()
      .then(async (contas) => {
        const contaSteam = contas.find((c) => c.plataforma === 'Steam')
        setConta(contaSteam || null)
        if (contaSteam && contaSteam.bibliotecaPublica === true) {
          setJogos(await listarJogos(contaSteam.id))
        }
        setAvaliacoes(await listarAvaliacoes())
      })
      .catch((err) => setErro(err.message || 'Não foi possível carregar suas avaliações.'))
      .finally(() => setCarregando(false))
  }, [])

  const jogosElegiveis = useMemo(() => jogos.filter((jogo) => jogo.horasJogadas > 0), [jogos])
  const [filtroJogo, setFiltroJogo, jogosFiltrados] = useFiltroJogos(jogosElegiveis)
  const avaliacaoPorAppId = useMemo(() => {
    const mapa = {}
    for (const avaliacao of avaliacoes) mapa[avaliacao.appId] = avaliacao
    return mapa
  }, [avaliacoes])
  const jogoPorAppId = useMemo(() => {
    const mapa = {}
    for (const jogo of jogos) mapa[jogo.appId] = jogo
    return mapa
  }, [jogos])

  const jogoEscolhido = jogosElegiveis.find((jogo) => String(jogo.appId) === jogoAppId)

  function handleAbrirNovaAvaliacao() {
    setEditandoId(null)
    setJogoAppId('')
    setFiltroJogo('')
    setNota(0)
    setResenha('')
    setErroModal('')
    setModalAberto(true)
  }

  function handleEditarAvaliacao(avaliacao) {
    setEditandoId(avaliacao.id)
    setJogoAppId(String(avaliacao.appId))
    setNota(avaliacao.nota)
    setResenha(avaliacao.resenha || '')
    setErroModal('')
    setModalAberto(true)
  }

  function handleFecharModal() {
    setModalAberto(false)
  }

  function handleEscolherJogo(appId) {
    setJogoAppId(appId)
    const existente = avaliacaoPorAppId[Number(appId)]
    if (existente) {
      setNota(existente.nota)
      setResenha(existente.resenha || '')
    } else {
      setNota(0)
      setResenha('')
    }
  }

  async function handleSalvar() {
    if (!jogoAppId || !nota) {
      setErroModal('Escolha um jogo e dê uma nota em estrelas.')
      return
    }
    setSalvando(true)
    setErroModal('')
    try {
      await salvarAvaliacao({
        contaPlataformaId: conta.id,
        appId: Number(jogoAppId),
        nota,
        resenha: resenha.trim() || null,
      })
      setAvaliacoes(await listarAvaliacoes())
      setModalAberto(false)
    } catch (err) {
      setErroModal(err.message || 'Não foi possível salvar a avaliação.')
    } finally {
      setSalvando(false)
    }
  }

  async function handleRemover(id) {
    try {
      await removerAvaliacao(id)
      setAvaliacoes((atual) => atual.filter((a) => a.id !== id))
    } catch (err) {
      setErro(err.message || 'Não foi possível remover a avaliação.')
    }
  }

  const temSteamComBiblioteca = conta && conta.bibliotecaPublica === true

  return (
    <LayoutApp>
      <div className="cabecalho-biblioteca">
        <h1 className="titulo-pagina">Avaliações</h1>
        {temSteamComBiblioteca && jogosElegiveis.length > 0 && (
          <button type="button" className="botao-principal" onClick={handleAbrirNovaAvaliacao}>
            Nova avaliação
          </button>
        )}
      </div>

      {erro && <p className="mensagem mensagem-erro">{erro}</p>}

      {carregando ? (
        <p className="mensagem">Carregando...</p>
      ) : !temSteamComBiblioteca ? (
        <p className="mensagem">
          Vincule uma conta Steam com biblioteca pública em "Contas vinculadas" para poder avaliar seus jogos.
        </p>
      ) : jogosElegiveis.length === 0 ? (
        <p className="mensagem">Você precisa ter jogado algum jogo (mais de 0 horas) para poder avaliá-lo.</p>
      ) : avaliacoes.length === 0 ? (
        <p className="mensagem">Você ainda não avaliou nenhum jogo.</p>
      ) : (
        <div className="lista-avaliacoes">
          {avaliacoes.map((avaliacao) => (
            <div key={avaliacao.id} className="cartao-avaliacao">
              <CapaJogo
                jogo={{ appId: avaliacao.appId, nome: avaliacao.jogoNome, imagem: avaliacao.jogoImagem }}
                className="icone-jogo-avaliacao"
              />
              <div className="conteudo-avaliacao">
                <div className="cabecalho-avaliacao">
                  <strong>
                    <IconeSteam />
                    {avaliacao.jogoNome}
                    {jogoPorAppId[avaliacao.appId] && (
                      <span className="horas-jogo-avaliacao">({formatarProgresso(jogoPorAppId[avaliacao.appId])})</span>
                    )}
                  </strong>
                  <div className="acoes-cartao-destaque">
                    <button type="button" className="botao-editar-destaque" onClick={() => handleEditarAvaliacao(avaliacao)}>
                      Editar
                    </button>
                    <button
                      type="button"
                      className="botao-remover-destaque"
                      onClick={() => handleRemover(avaliacao.id)}
                      aria-label="Remover avaliação"
                    >
                      ×
                    </button>
                  </div>
                </div>
                <SeletorEstrelas valor={avaliacao.nota} somenteLeitura />
                {avaliacao.resenha && <p className="resenha-avaliacao">{avaliacao.resenha}</p>}
              </div>
            </div>
          ))}
        </div>
      )}

      {modalAberto && (
        <Modal titulo={editandoId !== null ? 'Editar avaliação' : 'Nova avaliação'} onFechar={handleFecharModal}>
          <div className="campo">
            <span>Jogo</span>
            {editandoId !== null ? (
              jogoEscolhido && (
                <div className="modal-item-jogo modal-item-jogo-travado">
                  <CapaJogo jogo={jogoEscolhido} className="modal-icone-jogo" />
                  <span>
                    {jogoEscolhido.nome} ({formatarProgresso(jogoEscolhido)})
                  </span>
                </div>
              )
            ) : (
              <>
                <input
                  type="text"
                  value={filtroJogo}
                  onChange={(e) => setFiltroJogo(e.target.value)}
                  placeholder="Buscar jogo pelo nome..."
                />
                <div className="lista-busca-jogos">
                  {jogosFiltrados.length === 0 ? (
                    <p className="mensagem">Nenhum jogo encontrado.</p>
                  ) : (
                    jogosFiltrados.map((jogo) => (
                      <button
                        key={jogo.appId}
                        type="button"
                        className={`modal-item-jogo ${String(jogo.appId) === jogoAppId ? 'modal-item-jogo-selecionado' : ''}`}
                        onClick={() => handleEscolherJogo(String(jogo.appId))}
                      >
                        <CapaJogo jogo={jogo} className="modal-icone-jogo" />
                        <span>
                          {jogo.nome} ({formatarProgresso(jogo)})
                          {avaliacaoPorAppId[jogo.appId] ? ' — já avaliado' : ''}
                        </span>
                      </button>
                    ))
                  )}
                </div>
              </>
            )}
          </div>

          <div className="campo">
            <span>Nota</span>
            <SeletorEstrelas valor={nota} onChange={setNota} />
          </div>

          <label className="campo">
            <span>Resenha (opcional)</span>
            <textarea
              className="campo-textarea"
              value={resenha}
              onChange={(e) => setResenha(e.target.value)}
              rows={4}
              placeholder="O que você achou desse jogo?"
            />
          </label>

          {erroModal && <p className="mensagem mensagem-erro">{erroModal}</p>}

          <div className="acoes-formulario-destaque">
            <button type="button" className="botao-principal" onClick={handleSalvar} disabled={salvando}>
              {salvando ? 'Salvando...' : 'Confirmar'}
            </button>
            <button type="button" className="botao-secundario" onClick={handleFecharModal}>
              Cancelar
            </button>
          </div>
        </Modal>
      )}
    </LayoutApp>
  )
}
