import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { definirSenha } from '../lib/api'

export default function DefinirSenhaPage() {
  const navigate = useNavigate()
  const [senha, setSenha] = useState('')
  const [confirmarSenha, setConfirmarSenha] = useState('')
  const [salvando, setSalvando] = useState(false)
  const [erro, setErro] = useState('')

  async function handleSubmit(event) {
    event.preventDefault()
    setErro('')

    if (senha !== confirmarSenha) {
      setErro('As senhas não coincidem.')
      return
    }

    setSalvando(true)
    try {
      await definirSenha(senha)
      navigate('/inicio')
    } catch (err) {
      setErro(err.message || 'Não foi possível definir a senha.')
    } finally {
      setSalvando(false)
    }
  }

  return (
    <div className="pagina-centralizada">
      <div className="cartao">
        <h1 className="titulo">Quase lá!</h1>
        <p className="subtitulo">
          Crie uma senha para poder entrar pelo e-mail ou nickname nas próximas vezes,
          sem precisar do Google.
        </p>

        <form onSubmit={handleSubmit} className="formulario-perfil">
          <label className="campo">
            <span>Nova senha</span>
            <input
              type="password"
              value={senha}
              onChange={(e) => setSenha(e.target.value)}
              minLength={6}
              maxLength={72}
              required
              autoFocus
            />
          </label>
          <label className="campo">
            <span>Confirmar senha</span>
            <input
              type="password"
              value={confirmarSenha}
              onChange={(e) => setConfirmarSenha(e.target.value)}
              minLength={6}
              maxLength={72}
              required
            />
          </label>
          <button type="submit" className="botao-principal" disabled={salvando}>
            {salvando ? 'Salvando...' : 'Criar senha e continuar'}
          </button>
          {erro && <p className="mensagem mensagem-erro">{erro}</p>}
        </form>
      </div>
    </div>
  )
}
