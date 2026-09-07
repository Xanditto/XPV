import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { loginComGoogle } from '../lib/api'

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID

export default function LoginPage() {
  const navigate = useNavigate()
  const botaoRef = useRef(null)
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(false)

  useEffect(() => {
    async function handleCredentialResponse(response) {
      setErro('')
      setCarregando(true)
      try {
        await loginComGoogle(response.credential)
        navigate('/perfil')
      } catch (err) {
        setErro(err.message || 'Não foi possível entrar com o Google.')
      } finally {
        setCarregando(false)
      }
    }

    if (!GOOGLE_CLIENT_ID) {
      setErro('VITE_GOOGLE_CLIENT_ID não configurado. Veja o README para criar o Client ID no Google Cloud.')
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
          text: 'signin_with',
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
        <div ref={botaoRef} className="botao-google" />
        {carregando && <p className="mensagem">Entrando...</p>}
        {erro && <p className="mensagem mensagem-erro">{erro}</p>}
      </div>
    </div>
  )
}
