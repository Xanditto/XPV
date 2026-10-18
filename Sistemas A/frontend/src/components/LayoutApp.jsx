import { useState } from 'react'
import CabecalhoUsuario from './CabecalhoUsuario'
import MenuLateral from './MenuLateral'

export default function LayoutApp({ children }) {
  const [menuAberto, setMenuAberto] = useState(true)

  return (
    <div className="layout-app">
      <CabecalhoUsuario onAlternarMenu={() => setMenuAberto((atual) => !atual)} />
      <div className="corpo-app">
        <MenuLateral aberto={menuAberto} />
        <main className="conteudo-principal">{children}</main>
      </div>
    </div>
  )
}
