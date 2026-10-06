import { getUsuarioCache } from '../lib/api'

export default function CabecalhoUsuario() {
  const usuario = getUsuarioCache()

  return (
    <header className="barra-usuario">
      <div className="marca-app">
        <span className="marca-titulo">XP Vault</span>
        <span className="marca-subtitulo">Seus jogos e conquistas, reunidos em um só lugar.</span>
      </div>

      <div className="controles-usuario">
        <span className="nickname-topo">{usuario?.nickname}</span>
        <img
          src={usuario?.avatar || 'https://placehold.co/32x32?text=XPV'}
          alt="Foto de perfil"
          className="avatar-pequeno"
        />
      </div>
    </header>
  )
}
