import CabecalhoUsuario from './CabecalhoUsuario'
import MenuLateral from './MenuLateral'

export default function LayoutApp({ children }) {
  return (
    <div className="layout-app">
      <CabecalhoUsuario />
      <div className="corpo-app">
        <MenuLateral />
        <main className="conteudo-principal">{children}</main>
      </div>
    </div>
  )
}
