import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  atualizarPerfil,
  buscarPerfil,
  getUsuarioCache,
  listarContas,
  listarConquistasDoJogo,
  listarDestaques,
  listarJogos,
  logout,
  salvarDestaques,
} from '../lib/api'
import LayoutApp from '../components/LayoutApp'
import Modal from '../components/Modal'
import {
  CardDestaque,
  LIMITE_POR_TIPO,
  MAXIMO_CONQUISTAS_ESPECIFICAS,
  MAXIMO_PERFECCIONISTA,
  MAXIMO_TAMANHO_TEXTO,
  MINIMO_CONQUISTAS_ESPECIFICAS,
  MINIMO_PERFECCIONISTA,
  TIPOS_DESTAQUE,
  jogoEstaPlatinado,
} from '../components/Destaques'

function arquivoParaBase64(arquivo) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = reject
    reader.readAsDataURL(arquivo)
  })
}

/** Reconstrói o DestaqueRequest equivalente a um DestaqueResponse já salvo, para reenviar junto de uma mudança. */
function paraRequest(destaque, contaId) {
  if (destaque.tipo === 'IMAGEM_PERSONALIZADA') {
    return { tipo: destaque.tipo, plataforma: null, jogos: null, imagem: destaque.imagem, texto: null }
  }
  if (destaque.tipo === 'CAIXA_TEXTO') {
    return { tipo: destaque.tipo, plataforma: null, jogos: null, imagem: null, texto: destaque.texto }
  }
  if (destaque.tipo === 'CONQUISTAS_ESPECIFICAS') {
    const jogos = destaque.conquistas?.map((c) => ({
      contaPlataformaId: contaId,
      appId: c.appId,
      conquistaChave: c.conquistaChave,
    }))
    return { tipo: destaque.tipo, plataforma: null, jogos, imagem: null, texto: null }
  }
  return {
    tipo: destaque.tipo,
    plataforma: destaque.plataforma,
    jogos: destaque.jogos?.map((j) => ({ contaPlataformaId: contaId, appId: j.appId, conquistaChave: null })) ?? null,
    imagem: null,
    texto: null,
  }
}

export default function ProfilePage() {
  const navigate = useNavigate()
  const [usuario, setUsuario] = useState(getUsuarioCache())
  const [nickname, setNickname] = useState(usuario?.nickname || '')
  const [avatarPreview, setAvatarPreview] = useState(usuario?.avatar || '')
  const [descricao, setDescricao] = useState(usuario?.descricao || '')
  const [salvando, setSalvando] = useState(false)
  const [mensagem, setMensagem] = useState('')
  const [erro, setErro] = useState('')

  const [destaques, setDestaques] = useState([])
  const [conta, setConta] = useState(null)
  const [jogos, setJogos] = useState([])
  const [carregandoDestaques, setCarregandoDestaques] = useState(true)
  const [erroDestaques, setErroDestaques] = useState('')
  const [salvandoDestaque, setSalvandoDestaque] = useState(false)
  const [mostrarFormulario, setMostrarFormulario] = useState(false)
  const [editandoIndex, setEditandoIndex] = useState(null)
  const [tipoNovo, setTipoNovo] = useState('')
  const [jogoFavoritoAppId, setJogoFavoritoAppId] = useState('')
  const [perfeccionistaAppIds, setPerfeccionistaAppIds] = useState([])
  const [imagemDestaque, setImagemDestaque] = useState('')
  const [textoDestaque, setTextoDestaque] = useState('')
  const [modalConquistasAberto, setModalConquistasAberto] = useState(false)
  const [jogoConquistaAppId, setJogoConquistaAppId] = useState('')
  const [conquistasDoJogo, setConquistasDoJogo] = useState([])
  const [carregandoConquistas, setCarregandoConquistas] = useState(false)
  const [conquistasEscolhidas, setConquistasEscolhidas] = useState([])

  useEffect(() => {
    buscarPerfil()
      .then((dados) => {
        setUsuario(dados)
        setNickname(dados.nickname)
        setAvatarPreview(dados.avatar)
        setDescricao(dados.descricao || '')
      })
      .catch(() => {
        logout()
        navigate('/login')
      })
  }, [navigate])

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
      .catch((err) => setErroDestaques(err.message || 'Não foi possível carregar seus destaques.'))
      .finally(() => setCarregandoDestaques(false))
  }, [])

  const jogosPlatinados = useMemo(() => jogos.filter(jogoEstaPlatinado), [jogos])
  const temSteamComBiblioteca = conta && conta.bibliotecaPublica === true

  const contagemPorTipo = useMemo(() => {
    const contagem = {}
    for (const d of destaques) {
      contagem[d.tipo] = (contagem[d.tipo] || 0) + 1
    }
    return contagem
  }, [destaques])

  const tiposDisponiveis = useMemo(
    () => Object.keys(TIPOS_DESTAQUE).filter((tipo) => (contagemPorTipo[tipo] || 0) < LIMITE_POR_TIPO[tipo]),
    [contagemPorTipo],
  )

  async function handleTrocarFoto(event) {
    const arquivo = event.target.files?.[0]
    if (!arquivo) return
    const base64 = await arquivoParaBase64(arquivo)
    setAvatarPreview(base64)
  }

  async function handleEscolherImagemDestaque(event) {
    const arquivo = event.target.files?.[0]
    if (!arquivo) return
    const base64 = await arquivoParaBase64(arquivo)
    setImagemDestaque(base64)
  }

  async function handleSalvar(event) {
    event.preventDefault()
    setSalvando(true)
    setErro('')
    setMensagem('')
    try {
      const atualizado = await atualizarPerfil({ nickname, avatar: avatarPreview, descricao })
      setUsuario(atualizado)
      setMensagem('Perfil atualizado com sucesso!')
    } catch (err) {
      setErro(err.message || 'Não foi possível salvar o perfil.')
    } finally {
      setSalvando(false)
    }
  }

  function requestsAtuais() {
    return destaques.map((d) => paraRequest(d, conta?.id))
  }

  function limparFormularioDestaque() {
    setMostrarFormulario(false)
    setEditandoIndex(null)
    setTipoNovo('')
    setJogoFavoritoAppId('')
    setPerfeccionistaAppIds([])
    setImagemDestaque('')
    setTextoDestaque('')
    setJogoConquistaAppId('')
    setConquistasDoJogo([])
    setConquistasEscolhidas([])
  }

  function handleAbrirFormulario() {
    setEditandoIndex(null)
    setTipoNovo(tiposDisponiveis[0] || '')
    setMostrarFormulario(true)
  }

  function handleEditarDestaque(index) {
    const destaque = destaques[index]
    setEditandoIndex(index)
    setTipoNovo(destaque.tipo)
    setJogoFavoritoAppId(destaque.tipo === 'JOGO_FAVORITO' ? String(destaque.jogos[0].appId) : '')
    setPerfeccionistaAppIds(
      destaque.tipo === 'PERFECCIONISTA' ? destaque.jogos.map((j) => String(j.appId)) : [],
    )
    setImagemDestaque(destaque.tipo === 'IMAGEM_PERSONALIZADA' ? destaque.imagem : '')
    setTextoDestaque(destaque.tipo === 'CAIXA_TEXTO' ? destaque.texto : '')
    setConquistasEscolhidas(destaque.tipo === 'CONQUISTAS_ESPECIFICAS' ? destaque.conquistas : [])
    setJogoConquistaAppId('')
    setConquistasDoJogo([])
    setMostrarFormulario(true)
  }

  async function persistirDestaques(novaLista) {
    setSalvandoDestaque(true)
    setErroDestaques('')
    try {
      const atualizados = await salvarDestaques(novaLista)
      setDestaques(atualizados)
      limparFormularioDestaque()
    } catch (err) {
      setErroDestaques(err.message || 'Não foi possível salvar os destaques.')
    } finally {
      setSalvandoDestaque(false)
    }
  }

  function handleRemoverDestaque(index) {
    persistirDestaques(requestsAtuais().filter((_, i) => i !== index))
  }

  function handleAdicionarDestaque() {
    const novoRequest = { tipo: tipoNovo, plataforma: null, jogos: null, imagem: null, texto: null }
    if (tipoNovo === 'HORAS_PLATAFORMA') {
      novoRequest.plataforma = 'Steam'
    } else if (tipoNovo === 'JOGO_FAVORITO') {
      if (!jogoFavoritoAppId) return
      novoRequest.jogos = [{ contaPlataformaId: conta.id, appId: Number(jogoFavoritoAppId), conquistaChave: null }]
    } else if (tipoNovo === 'PERFECCIONISTA') {
      if (perfeccionistaAppIds.length < MINIMO_PERFECCIONISTA || perfeccionistaAppIds.length > MAXIMO_PERFECCIONISTA) return
      novoRequest.jogos = perfeccionistaAppIds.map((appId) => ({
        contaPlataformaId: conta.id,
        appId: Number(appId),
        conquistaChave: null,
      }))
    } else if (tipoNovo === 'IMAGEM_PERSONALIZADA') {
      if (!imagemDestaque) return
      novoRequest.imagem = imagemDestaque
    } else if (tipoNovo === 'CAIXA_TEXTO') {
      if (!textoDestaque.trim()) return
      novoRequest.texto = textoDestaque
    } else if (tipoNovo === 'CONQUISTAS_ESPECIFICAS') {
      if (
        conquistasEscolhidas.length < MINIMO_CONQUISTAS_ESPECIFICAS ||
        conquistasEscolhidas.length > MAXIMO_CONQUISTAS_ESPECIFICAS
      )
        return
      novoRequest.jogos = conquistasEscolhidas.map((c) => ({
        contaPlataformaId: conta.id,
        appId: c.appId,
        conquistaChave: c.conquistaChave,
      }))
    }

    if (editandoIndex !== null) {
      const atuais = requestsAtuais()
      atuais[editandoIndex] = novoRequest
      persistirDestaques(atuais)
    } else {
      persistirDestaques([...requestsAtuais(), novoRequest])
    }
  }

  function toggleJogoPerfeccionista(appId) {
    setPerfeccionistaAppIds((atual) => (atual.includes(appId) ? atual.filter((id) => id !== appId) : [...atual, appId]))
  }

  async function handleEscolherJogoConquista(appId) {
    setJogoConquistaAppId(appId)
    setConquistasDoJogo([])
    if (!appId) return
    setCarregandoConquistas(true)
    try {
      const conquistas = await listarConquistasDoJogo(conta.id, appId)
      setConquistasDoJogo(conquistas)
    } catch (err) {
      setErroDestaques(err.message || 'Não foi possível carregar as conquistas desse jogo.')
    } finally {
      setCarregandoConquistas(false)
    }
  }

  function toggleConquistaEspecifica(conquista) {
    const jogo = jogos.find((j) => String(j.appId) === String(jogoConquistaAppId))
    setConquistasEscolhidas((atual) => {
      const jaEscolhida = atual.some((c) => c.appId === jogo.appId && c.conquistaChave === conquista.chave)
      if (jaEscolhida) {
        return atual.filter((c) => !(c.appId === jogo.appId && c.conquistaChave === conquista.chave))
      }
      if (atual.length >= MAXIMO_CONQUISTAS_ESPECIFICAS) return atual
      return [
        ...atual,
        {
          appId: jogo.appId,
          jogoNome: jogo.nome,
          conquistaChave: conquista.chave,
          conquistaNome: conquista.nome,
          conquistaIcone: conquista.icone,
        },
      ]
    })
  }

  if (!usuario) return null

  return (
    <LayoutApp>
      <h1 className="titulo-pagina">Editar Perfil</h1>

      <div className="cartao cartao-largo cartao-formulario-perfil">
        <form onSubmit={handleSalvar} className="formulario-perfil">
          <div className="avatar-wrapper">
            <img
              src={avatarPreview || 'https://placehold.co/120x120?text=XPV'}
              alt="Foto de perfil"
              className="avatar"
            />
            <label className="botao-trocar-foto">
              Trocar foto
              <input type="file" accept="image/*" onChange={handleTrocarFoto} hidden />
            </label>
          </div>

          <label className="campo">
            <span>Nickname</span>
            <input
              type="text"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              minLength={3}
              maxLength={30}
              required
            />
          </label>

          <label className="campo">
            <span>E-mail</span>
            <input type="email" value={usuario.email} disabled />
          </label>

          <label className="campo">
            <span>Descrição</span>
            <textarea
              className="campo-textarea"
              value={descricao}
              onChange={(e) => setDescricao(e.target.value)}
              maxLength={500}
              rows={4}
              placeholder="Conte um pouco sobre você..."
            />
          </label>

          <button type="submit" className="botao-principal" disabled={salvando}>
            {salvando ? 'Salvando...' : 'Salvar alterações'}
          </button>

          {mensagem && <p className="mensagem mensagem-sucesso">{mensagem}</p>}
          {erro && <p className="mensagem mensagem-erro">{erro}</p>}
        </form>
      </div>

      <h2 className="subtitulo-secao">Destaques do perfil</h2>
      <p className="subtitulo-pagina">Cada tipo de destaque tem seu próprio limite (a maioria permite 1; imagem e texto permitem 3; perfeccionista e conquistas permitem 2).</p>

      {erroDestaques && <p className="mensagem mensagem-erro">{erroDestaques}</p>}

      {carregandoDestaques ? (
        <p className="mensagem">Carregando...</p>
      ) : (
        <>
          <div className="lista-destaques">
            {destaques.map((destaque, index) => (
              <CardDestaque
                key={destaque.id ?? index}
                destaque={destaque}
                onRemover={() => handleRemoverDestaque(index)}
                onEditar={
                  destaque.tipo === 'MAIS_JOGADOS' || destaque.tipo === 'TEMPO_POR_PLATAFORMA'
                    ? undefined
                    : () => handleEditarDestaque(index)
                }
              />
            ))}
          </div>

          {!temSteamComBiblioteca ? (
            <p className="mensagem">
              Vincule uma conta Steam com biblioteca pública em "Contas vinculadas" para poder escolher destaques.
            </p>
          ) : mostrarFormulario ? (
            <div className="formulario-destaque">
              <label className="campo">
                <span>Tipo de destaque</span>
                <select value={tipoNovo} onChange={(e) => setTipoNovo(e.target.value)} disabled={editandoIndex !== null}>
                  {editandoIndex !== null ? (
                    <option value={tipoNovo}>{TIPOS_DESTAQUE[tipoNovo]}</option>
                  ) : (
                    tiposDisponiveis.map((tipo) => (
                      <option key={tipo} value={tipo}>
                        {TIPOS_DESTAQUE[tipo]}
                      </option>
                    ))
                  )}
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
                    Jogos platinados ({perfeccionistaAppIds.length}/{MAXIMO_PERFECCIONISTA}, mínimo{' '}
                    {MINIMO_PERFECCIONISTA})
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

              {tipoNovo === 'IMAGEM_PERSONALIZADA' && (
                <div className="campo">
                  <span>Imagem</span>
                  {imagemDestaque && <img src={imagemDestaque} alt="Pré-visualização" className="imagem-destaque-preview" />}
                  <label className="botao-trocar-foto">
                    {imagemDestaque ? 'Trocar imagem' : 'Escolher imagem'}
                    <input type="file" accept="image/*" onChange={handleEscolherImagemDestaque} hidden />
                  </label>
                </div>
              )}

              {tipoNovo === 'CAIXA_TEXTO' && (
                <label className="campo">
                  <span>
                    Texto ({textoDestaque.length}/{MAXIMO_TAMANHO_TEXTO})
                  </span>
                  <textarea
                    className="campo-textarea"
                    value={textoDestaque}
                    onChange={(e) => setTextoDestaque(e.target.value)}
                    maxLength={MAXIMO_TAMANHO_TEXTO}
                    rows={4}
                    placeholder="Escreva o que quiser..."
                  />
                </label>
              )}

              {tipoNovo === 'CONQUISTAS_ESPECIFICAS' && (
                <div className="campo">
                  <span>
                    Conquistas escolhidas ({conquistasEscolhidas.length}/{MAXIMO_CONQUISTAS_ESPECIFICAS})
                  </span>

                  {conquistasEscolhidas.length > 0 && (
                    <div className="lista-selecao-jogos">
                      {conquistasEscolhidas.map((c) => (
                        <span key={`${c.appId}-${c.conquistaChave}`} className="opcao-jogo">
                          {c.conquistaNome} ({c.jogoNome})
                          <button
                            type="button"
                            className="botao-remover-destaque"
                            onClick={() => toggleConquistaEspecifica({ chave: c.conquistaChave })}
                            aria-label="Remover conquista escolhida"
                          >
                            ×
                          </button>
                        </span>
                      ))}
                    </div>
                  )}

                  <button type="button" className="botao-secundario" onClick={() => setModalConquistasAberto(true)}>
                    Escolher jogo e conquistas
                  </button>
                </div>
              )}

              <div className="acoes-formulario-destaque">
                <button type="button" className="botao-principal" onClick={handleAdicionarDestaque} disabled={salvandoDestaque}>
                  {salvandoDestaque ? 'Salvando...' : editandoIndex !== null ? 'Salvar edição' : 'Confirmar'}
                </button>
                <button type="button" className="botao-secundario" onClick={limparFormularioDestaque}>
                  Cancelar
                </button>
              </div>
            </div>
          ) : tiposDisponiveis.length === 0 ? (
            <p className="mensagem">Você atingiu o limite de todos os tipos de destaque. Remova algum para escolher outro.</p>
          ) : (
            <button type="button" className="botao-secundario" onClick={handleAbrirFormulario}>
              Adicionar destaque
            </button>
          )}
        </>
      )}

      <button type="button" className="botao-secundario" onClick={() => navigate('/meu-perfil')}>
        Ver meu perfil
      </button>

      {modalConquistasAberto && (
        <Modal
          titulo={jogoConquistaAppId ? 'Escolha as conquistas' : 'Escolha um jogo'}
          onFechar={() => setModalConquistasAberto(false)}
        >
          {!jogoConquistaAppId ? (
            <div className="modal-lista-jogos">
              {jogos.map((jogo) => (
                <button
                  key={jogo.appId}
                  type="button"
                  className="modal-item-jogo"
                  onClick={() => handleEscolherJogoConquista(String(jogo.appId))}
                >
                  <img src={jogo.imagem || 'https://placehold.co/48x48?text=%20'} alt={jogo.nome} className="modal-icone-jogo" />
                  <span>{jogo.nome}</span>
                </button>
              ))}
            </div>
          ) : (
            <>
              <button type="button" className="botao-secundario" onClick={() => handleEscolherJogoConquista('')}>
                ← Escolher outro jogo
              </button>

              {carregandoConquistas ? (
                <p className="mensagem">Carregando conquistas...</p>
              ) : conquistasDoJogo.length === 0 ? (
                <p className="mensagem">Você ainda não obteve nenhuma conquista nesse jogo.</p>
              ) : (
                <div className="modal-lista-conquistas">
                  {conquistasDoJogo.map((conquista) => {
                    const jogo = jogos.find((j) => String(j.appId) === String(jogoConquistaAppId))
                    const marcada = conquistasEscolhidas.some(
                      (c) => c.appId === jogo?.appId && c.conquistaChave === conquista.chave,
                    )
                    return (
                      <label key={conquista.chave} className="modal-item-conquista">
                        <input type="checkbox" checked={marcada} onChange={() => toggleConquistaEspecifica(conquista)} />
                        <img
                          src={conquista.icone || 'https://placehold.co/48x48?text=%20'}
                          alt={conquista.nome}
                          className="modal-icone-conquista"
                        />
                        <span className="modal-info-conquista">
                          <strong>{conquista.nome}</strong>
                          {conquista.descricao && <span className="modal-descricao-conquista">{conquista.descricao}</span>}
                        </span>
                      </label>
                    )
                  })}
                </div>
              )}
            </>
          )}
        </Modal>
      )}
    </LayoutApp>
  )
}
