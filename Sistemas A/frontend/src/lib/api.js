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
    // Erros de validação de campo (@Valid) trazem a mensagem específica
    // dentro de "errors", não em "message" (que ali vem genérico).
    const mensagemValidacao = erro.errors?.[0]?.defaultMessage
    throw new Error(mensagemValidacao || erro.message || `Erro na requisição (${response.status})`)
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

export async function atualizarPerfil({ nickname, avatar, descricao }) {
  const response = await fetch(`${API_URL}/api/profile`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${getSessionToken()}`,
    },
    body: JSON.stringify({ nickname, avatar, descricao }),
  })
  const usuario = await tratarResposta(response)
  localStorage.setItem(USER_KEY, JSON.stringify(usuario))
  return usuario
}

export async function listarContas() {
  const response = await fetch(`${API_URL}/api/contas`, {
    headers: { Authorization: `Bearer ${getSessionToken()}` },
  })
  return tratarResposta(response)
}

export async function desvincularConta(id) {
  const response = await fetch(`${API_URL}/api/contas/${id}`, {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${getSessionToken()}` },
  })
  if (!response.ok) {
    const erro = await response.json().catch(() => ({}))
    const mensagemValidacao = erro.errors?.[0]?.defaultMessage
    throw new Error(mensagemValidacao || erro.message || `Erro na requisição (${response.status})`)
  }
}

export function iniciarVinculoSteam() {
  window.location.href = `${API_URL}/api/contas/steam/iniciar?session=${encodeURIComponent(getSessionToken())}`
}

export function iniciarVinculoBattleNet() {
  window.location.href = `${API_URL}/api/contas/battlenet/iniciar?session=${encodeURIComponent(getSessionToken())}`
}

export async function vincularRiot(riotId) {
  const response = await fetch(`${API_URL}/api/contas/riot`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${getSessionToken()}`,
    },
    body: JSON.stringify({ riotId }),
  })
  return tratarResposta(response)
}

export async function listarJogos(contaId) {
  const response = await fetch(`${API_URL}/api/contas/${contaId}/jogos`, {
    headers: { Authorization: `Bearer ${getSessionToken()}` },
  })
  return tratarResposta(response)
}

export async function sincronizarJogos(contaId) {
  const response = await fetch(`${API_URL}/api/contas/${contaId}/sincronizar`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${getSessionToken()}` },
  })
  return tratarResposta(response)
}

export async function listarConquistasDoJogo(contaId, appId) {
  const response = await fetch(`${API_URL}/api/contas/${contaId}/jogos/${appId}/conquistas`, {
    headers: { Authorization: `Bearer ${getSessionToken()}` },
  })
  return tratarResposta(response)
}

export async function buscarDetalhesLojaDoJogo(contaId, appId) {
  const response = await fetch(`${API_URL}/api/contas/${contaId}/jogos/${appId}/loja`, {
    headers: { Authorization: `Bearer ${getSessionToken()}` },
  })
  return tratarResposta(response)
}

export async function listarDestaques() {
  const response = await fetch(`${API_URL}/api/destaques`, {
    headers: { Authorization: `Bearer ${getSessionToken()}` },
  })
  return tratarResposta(response)
}

export async function salvarDestaques(destaques) {
  const response = await fetch(`${API_URL}/api/destaques`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${getSessionToken()}`,
    },
    body: JSON.stringify(destaques),
  })
  return tratarResposta(response)
}
