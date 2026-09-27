import { useNavigate } from 'react-router-dom'
import { getUsuarioCache, logout } from '../lib/api'

export default function HomePage() {
  const navigate = useNavigate()
  const usuario = getUsuarioCache()

  function handleSair() {
    logout()
    navigate('/login')
  }

  return (
    <div className="pagina-inicio">
      <header className="barra-usuario">
        <img
          src={usuario?.avatar || 'https://placehold.co/32x32?text=XPV'}
          alt="Foto de perfil"
          className="avatar-pequeno"
        />
        <span className="nickname-topo">{usuario?.nickname}</span>
        <button type="button" className="botao-editar-perfil" onClick={() => navigate('/contas')}>
          Contas vinculadas
        </button>
        <button type="button" className="botao-editar-perfil" onClick={() => navigate('/perfil')}>
          Editar perfil
        </button>
        <button type="button" className="botao-sair-topo" onClick={handleSair}>
          Sair
        </button>
      </header>
    </div>
  )
}
