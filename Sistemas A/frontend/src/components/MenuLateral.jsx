import { useLocation, useNavigate } from 'react-router-dom'
import { logout } from '../lib/api'

const ITENS = [
  { rota: '/meu-perfil', rotulo: 'Meu perfil' },
  { rota: '/biblioteca', rotulo: 'Biblioteca' },
  { rota: '/contas', rotulo: 'Contas vinculadas' },
  { rota: '/perfil', rotulo: 'Editar perfil' },
]

export default function MenuLateral() {
  const navigate = useNavigate()
  const location = useLocation()

  function handleSair() {
    logout()
    navigate('/login')
  }

  return (
    <nav className="menu-lateral">
      {ITENS.map((item) => (
        <button
          key={item.rota}
          type="button"
          className={`menu-lateral-item ${location.pathname === item.rota ? 'ativo' : ''}`}
          onClick={() => navigate(item.rota)}
        >
          {item.rotulo}
        </button>
      ))}
      <button type="button" className="menu-lateral-item" onClick={handleSair}>
        Sair
      </button>
    </nav>
  )
}
