import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { logout } from '../lib/api'
import Modal from './Modal'

const ITENS = [
  { rota: '/meu-perfil', rotulo: 'Meu perfil' },
  { rota: '/biblioteca', rotulo: 'Biblioteca' },
  { rota: '/avaliacoes', rotulo: 'Avaliações' },
  { rota: '/recomendacoes', rotulo: 'Recomendação de jogos' },
  { rota: '/contas', rotulo: 'Contas vinculadas' },
  { rota: '/perfil', rotulo: 'Editar perfil' },
]

export default function MenuLateral({ aberto, onNavegar }) {
  const navigate = useNavigate()
  const location = useLocation()
  const [confirmandoSaida, setConfirmandoSaida] = useState(false)

  function irPara(rota) {
    navigate(rota)
    onNavegar?.()
  }

  function handleSair() {
    logout()
    navigate('/login')
  }

  // O modal fica fora do <nav>: no celular o menu usa transform, o que
  // prenderia o position: fixed do modal dentro dele.
  return (
    <>
      <nav className={`menu-lateral ${aberto ? '' : 'menu-lateral-fechado'}`}>
        {ITENS.map((item) => (
          <button
            key={item.rota}
            type="button"
            className={`menu-lateral-item ${location.pathname === item.rota ? 'ativo' : ''}`}
            onClick={() => irPara(item.rota)}
          >
            {item.rotulo}
          </button>
        ))}
        <button type="button" className="menu-lateral-item" onClick={() => setConfirmandoSaida(true)}>
          Sair
        </button>
      </nav>

      {confirmandoSaida && (
        <Modal titulo="Sair da conta" onFechar={() => setConfirmandoSaida(false)}>
          <p className="mensagem">Tem certeza que deseja sair?</p>
          <div className="acoes-formulario-destaque">
            <button type="button" className="botao-principal" onClick={handleSair}>
              Sair
            </button>
            <button type="button" className="botao-secundario" onClick={() => setConfirmandoSaida(false)}>
              Cancelar
            </button>
          </div>
        </Modal>
      )}
    </>
  )
}
