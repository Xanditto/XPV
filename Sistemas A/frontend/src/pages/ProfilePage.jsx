import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { atualizarPerfil, buscarPerfil, getUsuarioCache, logout } from '../lib/api'

function arquivoParaBase64(arquivo) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = reject
    reader.readAsDataURL(arquivo)
  })
}

export default function ProfilePage() {
  const navigate = useNavigate()
  const [usuario, setUsuario] = useState(getUsuarioCache())
  const [nickname, setNickname] = useState(usuario?.nickname || '')
  const [avatarPreview, setAvatarPreview] = useState(usuario?.avatar || '')
  const [salvando, setSalvando] = useState(false)
  const [mensagem, setMensagem] = useState('')
  const [erro, setErro] = useState('')

  useEffect(() => {
    buscarPerfil()
      .then((dados) => {
        setUsuario(dados)
        setNickname(dados.nickname)
        setAvatarPreview(dados.avatar)
      })
      .catch(() => {
        logout()
        navigate('/login')
      })
  }, [navigate])

  async function handleTrocarFoto(event) {
    const arquivo = event.target.files?.[0]
    if (!arquivo) return
    const base64 = await arquivoParaBase64(arquivo)
    setAvatarPreview(base64)
  }

  async function handleSalvar(event) {
    event.preventDefault()
    setSalvando(true)
    setErro('')
    setMensagem('')
    try {
      const atualizado = await atualizarPerfil({ nickname, avatar: avatarPreview })
      setUsuario(atualizado)
      setMensagem('Perfil atualizado com sucesso!')
    } catch (err) {
      setErro(err.message || 'Não foi possível salvar o perfil.')
    } finally {
      setSalvando(false)
    }
  }

  if (!usuario) return null

  return (
    <div className="pagina-centralizada">
      <div className="cartao">
        <h1 className="titulo">Meu Perfil</h1>

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

          <button type="submit" className="botao-principal" disabled={salvando}>
            {salvando ? 'Salvando...' : 'Salvar alterações'}
          </button>

          {mensagem && <p className="mensagem mensagem-sucesso">{mensagem}</p>}
          {erro && <p className="mensagem mensagem-erro">{erro}</p>}
        </form>

        <button type="button" className="botao-secundario" onClick={() => navigate('/inicio')}>
          Voltar
        </button>
      </div>
    </div>
  )
}
