import { useState } from 'react'
import CabecalhoUsuario from './CabecalhoUsuario'
import MenuLateral from './MenuLateral'

export default function LayoutApp({ children }) {
  const [menuAberto, setMenuAberto] = useState(() => window.innerWidth > 768)

  function fecharMenu() {
    setMenuAberto(false)
  }

  return (
    <div className="layout-app">
      <CabecalhoUsuario onAlternarMenu={() => setMenuAberto((atual) => !atual)} />
      <div className="corpo-app">
        <MenuLateral aberto={menuAberto} onNavegar={fecharMenu} />
        {menuAberto && <div className="fundo-menu-mobile" onClick={fecharMenu} />}
        <main className="conteudo-principal">{children}</main>
      </div>
    </div>
  )
}
