import { Navigate, Route, Routes } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import DefinirSenhaPage from './pages/DefinirSenhaPage'
import ProfilePage from './pages/ProfilePage'
import ContasPage from './pages/ContasPage'
import BibliotecaPage from './pages/BibliotecaPage'
import MeuPerfilPage from './pages/MeuPerfilPage'
import RecomendacoesPage from './pages/RecomendacoesPage'
import AvaliacoesPage from './pages/AvaliacoesPage'
import ProtectedRoute from './components/ProtectedRoute'
import { getSessionToken } from './lib/api'
import './App.css'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/definir-senha"
        element={
          <ProtectedRoute>
            <DefinirSenhaPage />
          </ProtectedRoute>
        }
      />
      {/* Ainda não existe uma tela inicial própria (Issue #40): por enquanto /inicio cai no perfil. */}
      <Route path="/inicio" element={<Navigate to="/meu-perfil" replace />} />
      <Route
        path="/perfil"
        element={
          <ProtectedRoute>
            <ProfilePage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/contas"
        element={
          <ProtectedRoute>
            <ContasPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/biblioteca"
        element={
          <ProtectedRoute>
            <BibliotecaPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/meu-perfil"
        element={
          <ProtectedRoute>
            <MeuPerfilPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/recomendacoes"
        element={
          <ProtectedRoute>
            <RecomendacoesPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/avaliacoes"
        element={
          <ProtectedRoute>
            <AvaliacoesPage />
          </ProtectedRoute>
        }
      />
      <Route path="/" element={<Navigate to={getSessionToken() ? '/meu-perfil' : '/login'} replace />} />
    </Routes>
  )
}
