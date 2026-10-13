import { useEffect, useMemo, useState } from 'react'
import {
  listarAvaliacoes,
  listarContas,
  listarJogos,
  removerAvaliacao,
  salvarAvaliacao,
} from '../lib/api'
import LayoutApp from '../components/LayoutApp'
import Modal from '../components/Modal'
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
  const avaliacaoPorAppId = useMemo(() => {
    const mapa = {}
    for (const avaliacao of avaliacoes) mapa[avaliacao.appId] = avaliacao
    return mapa
  }, [avaliacoes])

  function handleAbrirNovaAvaliacao() {
    setEditandoId(null)
    setJogoAppId('')
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
              <img
                src={avaliacao.jogoImagem || 'https://placehold.co/64x64?text=%20'}
                alt={avaliacao.jogoNome}
                className="icone-jogo-avaliacao"
              />
              <div className="conteudo-avaliacao">
                <div className="cabecalho-avaliacao">
                  <strong>
                    <IconeSteam />
                    {avaliacao.jogoNome}
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
          <label className="campo">
            <span>Jogo</span>
            <select value={jogoAppId} onChange={(e) => handleEscolherJogo(e.target.value)} disabled={editandoId !== null}>
              <option value="">Escolha um jogo</option>
              {jogosElegiveis.map((jogo) => (
                <option key={jogo.appId} value={jogo.appId}>
                  {jogo.nome}
                  {avaliacaoPorAppId[jogo.appId] ? ' (já avaliado)' : ''}
                </option>
              ))}
            </select>
          </label>

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
