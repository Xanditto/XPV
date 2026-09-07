import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { loginComGoogle, loginComSenha } from '../lib/api'

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID

export default function LoginPage() {
  const navigate = useNavigate()
  const botaoRef = useRef(null)

  const [identificador, setIdentificador] = useState('')
  const [senha, setSenha] = useState('')
  const [entrando, setEntrando] = useState(false)
  const [erroLogin, setErroLogin] = useState('')

  const [erroGoogle, setErroGoogle] = useState('')
  const [carregandoGoogle, setCarregandoGoogle] = useState(false)

  async function handleLoginComSenha(event) {
    event.preventDefault()
    setErroLogin('')
    setEntrando(true)
    try {
      await loginComSenha(identificador, senha)
      navigate('/inicio')
    } catch (err) {
      setErroLogin(err.message || 'Não foi possível entrar.')
    } finally {
      setEntrando(false)
    }
  }

  useEffect(() => {
    async function handleCredentialResponse(response) {
      setErroGoogle('')
      setCarregandoGoogle(true)
      try {
        const data = await loginComGoogle(response.credential)
        navigate(data.precisaDefinirSenha ? '/definir-senha' : '/inicio')
      } catch (err) {
        setErroGoogle(err.message || 'Não foi possível entrar com o Google.')
      } finally {
        setCarregandoGoogle(false)
      }
    }

    if (!GOOGLE_CLIENT_ID) {
      setErroGoogle('VITE_GOOGLE_CLIENT_ID não configurado. Veja o README para criar o Client ID no Google Cloud.')
      return
    }

    let intervalId
    let cancelado = false

    function tentarInicializar() {
      if (cancelado) return
      if (window.google?.accounts?.id) {
        window.google.accounts.id.initialize({
          client_id: GOOGLE_CLIENT_ID,
          callback: handleCredentialResponse,
        })
        window.google.accounts.id.renderButton(botaoRef.current, {
          theme: 'filled_black',
          size: 'large',
          text: 'signup_with',
          shape: 'pill',
        })
        clearInterval(intervalId)
      }
    }

    intervalId = setInterval(tentarInicializar, 200)
    tentarInicializar()

    return () => {
      cancelado = true
      clearInterval(intervalId)
    }
  }, [navigate])

  return (
    <div className="pagina-centralizada">
      <div className="cartao">
        <h1 className="titulo">XP Vault</h1>
        <p className="subtitulo">Centralize suas estatísticas de jogos em um só lugar.</p>

        <form onSubmit={handleLoginComSenha} className="formulario-perfil">
          <label className="campo">
            <span>E-mail ou nickname</span>
            <input
              type="text"
              value={identificador}
              onChange={(e) => setIdentificador(e.target.value)}
              required
              autoFocus
            />
          </label>
          <label className="campo">
            <span>Senha</span>
            <input
              type="password"
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
              required
            />
          </label>
          <button type="submit" className="botao-principal" disabled={entrando}>
            {entrando ? 'Entrando...' : 'Entrar'}
          </button>
          {erroLogin && <p className="mensagem mensagem-erro">{erroLogin}</p>}
        </form>

        <div className="divisoria"><span>Primeira vez por aqui?</span></div>

        <div className="botao-google" ref={botaoRef} />
        {carregandoGoogle && <p className="mensagem">Entrando...</p>}
        {erroGoogle && <p className="mensagem mensagem-erro">{erroGoogle}</p>}
      </div>
    </div>
  )
}
