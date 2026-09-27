import { useLocation, useNavigate } from 'react-router-dom'

const ITENS = [
  { rota: '/contas', rotulo: 'Contas vinculadas' },
  { rota: '/biblioteca', rotulo: 'Biblioteca' },
]

export default function MenuLateral() {
  const navigate = useNavigate()
  const location = useLocation()

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
    </nav>
  )
}
