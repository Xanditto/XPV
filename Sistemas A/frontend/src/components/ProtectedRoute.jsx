import { Navigate } from 'react-router-dom'
import { getSessionToken } from '../lib/api'

export default function ProtectedRoute({ children }) {
  if (!getSessionToken()) {
    return <Navigate to="/login" replace />
  }
  return children
}
