import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import {
  desvincularConta,
  iniciarVinculoBattleNet,
  iniciarVinculoSteam,
  listarContas,
  vincularRiot,
} from '../lib/api'

const NOMES_PLATAFORMA = {
  steam: 'Steam',
  battlenet: 'Battle.net',
  riot: 'Riot Games',
}

function mensagemStatus(searchParams) {
  const plataforma = searchParams.get('plataforma')
  const status = searchParams.get('status')
  if (!plataforma || !status) return null

  const nomePlataforma = NOMES_PLATAFORMA[plataforma] || plataforma
  if (status === 'ok') {
    return { tipo: 'sucesso', texto: `Conta ${nomePlataforma} vinculada com sucesso!` }
  }
  const detalhe = searchParams.get('mensagem')
  return {
    tipo: 'erro',
    texto: `Não foi possível vincular sua conta ${nomePlataforma}${detalhe ? `: ${detalhe}` : '.'}`,
  }
}

export default function ContasPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [contas, setContas] = useState([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  const [riotId, setRiotId] = useState('')
  const [vinculandoRiot, setVinculandoRiot] = useState(false)

  const mensagem = mensagemStatus(searchParams)

  useEffect(() => {
    listarContas()
      .then(setContas)
      .catch((err) => setErro(err.message || 'Não foi possível carregar suas contas.'))
      .finally(() => setCarregando(false))
  }, [])

  async function handleDesvincular(id) {
    try {
      await desvincularConta(id)
      setContas((atual) => atual.filter((c) => c.id !== id))
    } catch (err) {
      setErro(err.message || 'Não foi possível desvincular essa conta.')
    }
  }

  async function handleVincularRiot(event) {
    event.preventDefault()
    setErro('')
    setVinculandoRiot(true)
    try {
      const conta = await vincularRiot(riotId)
      setContas((atual) => [...atual.filter((c) => c.plataforma !== 'Riot Games'), conta])
      setRiotId('')
    } catch (err) {
      setErro(err.message || 'Não foi possível vincular esse Riot ID.')
    } finally {
      setVinculandoRiot(false)
    }
  }

  const temSteam = contas.some((c) => c.plataforma === 'Steam')
  const temBattleNet = contas.some((c) => c.plataforma === 'Battle.net')
  const temRiot = contas.some((c) => c.plataforma === 'Riot Games')

  return (
    <div className="pagina-centralizada">
      <div className="cartao cartao-largo">
        <h1 className="titulo">Contas Vinculadas</h1>
        <p className="subtitulo">
          Conecte suas contas de jogos para trazer seus dados para o XP Vault.
        </p>

        {mensagem && (
          <p className={mensagem.tipo === 'sucesso' ? 'mensagem mensagem-sucesso' : 'mensagem mensagem-erro'}>
            {mensagem.texto}
          </p>
        )}
        {erro && <p className="mensagem mensagem-erro">{erro}</p>}

        {carregando ? (
          <p className="mensagem">Carregando...</p>
        ) : (
          <div className="lista-contas">
            {contas.map((conta) => (
              <div key={conta.id} className="linha-conta">
                <img
                  src={conta.avatar || 'https://placehold.co/48x48?text=%20'}
                  alt={conta.plataforma}
                  className="avatar-conta"
                />
                <div className="info-conta">
                  <strong>{conta.plataforma}</strong>
                  <span>{conta.nickname}</span>
                  {conta.plataforma === 'Steam' && conta.perfilPublico === false && (
                    <span className="aviso-perfil-privado">
                      Perfil Steam privado — jogos, horas e conquistas não podem ser exibidos.
                      Torne seu perfil público nas configurações de privacidade da Steam.
                    </span>
                  )}
                </div>
                <button type="button" className="botao-secundario botao-desvincular"
                        onClick={() => handleDesvincular(conta.id)}>
                  Desvincular
                </button>
              </div>
            ))}

            {!temSteam && (
              <button type="button" className="botao-principal" onClick={iniciarVinculoSteam}>
                Vincular conta Steam
              </button>
            )}
            {!temBattleNet && (
              <button type="button" className="botao-principal" onClick={iniciarVinculoBattleNet}>
                Vincular conta Battle.net
              </button>
            )}

            {!temRiot && (
              <form onSubmit={handleVincularRiot} className="formulario-riot">
                <label className="campo">
                  <span>Riot ID (League of Legends / Valorant / TFT)</span>
                  <input
                    type="text"
                    placeholder="Nome#TAG"
                    value={riotId}
                    onChange={(e) => setRiotId(e.target.value)}
                    required
                  />
                </label>
                <button type="submit" className="botao-principal" disabled={vinculandoRiot}>
                  {vinculandoRiot ? 'Verificando...' : 'Vincular conta Riot Games'}
                </button>
              </form>
            )}
          </div>
        )}

        <button type="button" className="botao-secundario" onClick={() => navigate('/inicio')}>
          Voltar
        </button>
      </div>
    </div>
  )
}
