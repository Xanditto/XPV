import { useNavigate } from 'react-router-dom'
import { getUsuarioCache } from '../lib/api'
import { AVATAR_PADRAO } from '../lib/formato'

export default function CabecalhoUsuario({ onAlternarMenu }) {
  const navigate = useNavigate()
  const usuario = getUsuarioCache()

  return (
    <header className="barra-usuario">
      <div className="marca-app">
        <button type="button" className="botao-hamburguer" onClick={onAlternarMenu} aria-label="Abrir ou fechar o menu">
          ☰
        </button>
        <div className="marca-textos">
          <span className="marca-titulo">XP Vault</span>
          <span className="marca-subtitulo">Seus jogos e conquistas, reunidos em um só lugar.</span>
        </div>
      </div>

      <button
        type="button"
        className="controles-usuario"
        onClick={() => navigate('/meu-perfil')}
        aria-label="Ir para o meu perfil"
      >
        <span className="nickname-topo">{usuario?.nickname}</span>
        <img
          src={usuario?.avatar || AVATAR_PADRAO}
          alt="Foto de perfil"
          className="avatar-pequeno"
        />
      </button>
    </header>
  )
}
