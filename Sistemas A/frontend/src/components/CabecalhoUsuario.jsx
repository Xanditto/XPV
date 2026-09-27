import { useNavigate } from 'react-router-dom'
import { getUsuarioCache, logout } from '../lib/api'

export default function CabecalhoUsuario() {
  const navigate = useNavigate()
  const usuario = getUsuarioCache()

  function handleSair() {
    logout()
    navigate('/login')
  }

  return (
    <header className="barra-usuario">
      <div className="marca-app">
        <span className="marca-titulo">XP Vault</span>
        <span className="marca-subtitulo">Seus jogos e conquistas, reunidos em um só lugar.</span>
      </div>

      <div className="controles-usuario">
        <img
          src={usuario?.avatar || 'https://placehold.co/32x32?text=XPV'}
          alt="Foto de perfil"
          className="avatar-pequeno"
        />
        <span className="nickname-topo">{usuario?.nickname}</span>
        <button type="button" className="botao-editar-perfil" onClick={() => navigate('/perfil')}>
          Editar perfil
        </button>
        <button type="button" className="botao-sair-topo" onClick={handleSair}>
          Sair
        </button>
      </div>
    </header>
  )
}
