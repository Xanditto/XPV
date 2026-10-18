import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { logout } from '../lib/api'

const ITENS = [
  { rota: '/meu-perfil', rotulo: 'Meu perfil' },
  { rota: '/biblioteca', rotulo: 'Biblioteca' },
  { rota: '/avaliacoes', rotulo: 'Avaliações' },
  { rota: '/recomendacoes', rotulo: 'Recomendação de jogos' },
  { rota: '/contas', rotulo: 'Contas vinculadas' },
  { rota: '/perfil', rotulo: 'Editar perfil' },
]

export default function MenuLateral() {
  const navigate = useNavigate()
  const location = useLocation()
  const [aberto, setAberto] = useState(false)
  const menuRef = useRef(null)

  const itemAtual = ITENS.find((item) => item.rota === location.pathname)

  useEffect(() => {
    function fecharSeClicarFora(event) {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setAberto(false)
      }
    }
    document.addEventListener('mousedown', fecharSeClicarFora)
    return () => document.removeEventListener('mousedown', fecharSeClicarFora)
  }, [])

  function handleNavegar(rota) {
    navigate(rota)
    setAberto(false)
  }

  function handleSair() {
    logout()
    navigate('/login')
    setAberto(false)
  }

  return (
    <nav className="menu-dropdown" ref={menuRef}>
      <button type="button" className="menu-dropdown-botao" onClick={() => setAberto((atual) => !atual)}>
        <span>{itemAtual?.rotulo || 'Menu'}</span>
        <span className={`menu-dropdown-seta ${aberto ? 'aberta' : ''}`}>▾</span>
      </button>

      {aberto && (
        <div className="menu-dropdown-lista">
          {ITENS.map((item) => (
            <button
              key={item.rota}
              type="button"
              className={`menu-dropdown-item ${location.pathname === item.rota ? 'ativo' : ''}`}
              onClick={() => handleNavegar(item.rota)}
            >
              {item.rotulo}
            </button>
          ))}
          <button type="button" className="menu-dropdown-item" onClick={handleSair}>
            Sair
          </button>
        </div>
      )}
    </nav>
  )
}
