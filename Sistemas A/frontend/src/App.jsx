import { Navigate, Route, Routes } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import DefinirSenhaPage from './pages/DefinirSenhaPage'
import HomePage from './pages/HomePage'
import ProfilePage from './pages/ProfilePage'
import ContasPage from './pages/ContasPage'
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
      <Route
        path="/inicio"
        element={
          <ProtectedRoute>
            <HomePage />
          </ProtectedRoute>
        }
      />
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
      <Route path="/" element={<Navigate to={getSessionToken() ? '/inicio' : '/login'} replace />} />
    </Routes>
  )
}
