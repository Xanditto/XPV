const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

const SESSION_KEY = 'xpv.sessionToken'
const USER_KEY = 'xpv.usuario'

export function getSessionToken() {
  return localStorage.getItem(SESSION_KEY)
}

export function getUsuarioCache() {
  const raw = localStorage.getItem(USER_KEY)
  return raw ? JSON.parse(raw) : null
}

function salvarSessao(sessionToken, usuario) {
  localStorage.setItem(SESSION_KEY, sessionToken)
  localStorage.setItem(USER_KEY, JSON.stringify(usuario))
}

export function logout() {
  localStorage.removeItem(SESSION_KEY)
  localStorage.removeItem(USER_KEY)
}

async function tratarResposta(response) {
  if (!response.ok) {
    const erro = await response.json().catch(() => ({}))
    throw new Error(erro.message || `Erro na requisição (${response.status})`)
  }
  return response.json()
}

export async function loginComGoogle(idToken) {
  const response = await fetch(`${API_URL}/api/auth/google`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ idToken }),
  })
  const data = await tratarResposta(response)
  salvarSessao(data.sessionToken, data.usuario)
  return data
}

export async function loginComSenha(identificador, senha) {
  const response = await fetch(`${API_URL}/api/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ identificador, senha }),
  })
  const data = await tratarResposta(response)
  salvarSessao(data.sessionToken, data.usuario)
  return data
}

export async function definirSenha(senha) {
  const response = await fetch(`${API_URL}/api/auth/senha`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${getSessionToken()}`,
    },
    body: JSON.stringify({ senha }),
  })
  const usuario = await tratarResposta(response)
  localStorage.setItem(USER_KEY, JSON.stringify(usuario))
  return usuario
}

export async function buscarPerfil() {
  const response = await fetch(`${API_URL}/api/profile`, {
    headers: { Authorization: `Bearer ${getSessionToken()}` },
  })
  return tratarResposta(response)
}

export async function atualizarPerfil({ nickname, avatar }) {
  const response = await fetch(`${API_URL}/api/profile`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${getSessionToken()}`,
    },
    body: JSON.stringify({ nickname, avatar }),
  })
  const usuario = await tratarResposta(response)
  localStorage.setItem(USER_KEY, JSON.stringify(usuario))
  return usuario
}
