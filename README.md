# XP Vault (XPV)

[![Deploy](https://github.com/Xanditto/XPV/actions/workflows/deploy-pages.yml/badge.svg)](https://github.com/Xanditto/XPV/actions/workflows/deploy-pages.yml)
[![Site](https://img.shields.io/badge/site-online-brightgreen)](https://xanditto.github.io/XPV/)
[![Backend](https://img.shields.io/badge/backend-Render-46E3B7)](https://xpv-backend.onrender.com)

Plataforma que centraliza estatísticas de jogadores (biblioteca de jogos, horas
jogadas, conquistas) hoje espalhadas entre Steam, Riot Games, Battle.net e
outras plataformas, além de recursos sociais (comunidades, lista de amigos).

Este repositório acompanha o desenvolvimento incremental do projeto ao longo
da disciplina de Sistemas A, semana a semana, com o objetivo de futuramente
evoluir para um TCC.

## 🔗 Acesso ao vivo

### **[xanditto.github.io/XPV →](https://xanditto.github.io/XPV/)**

Site publicado automaticamente (GitHub Pages) a cada atualização da `main`,
com **login e dados funcionando de ponta a ponta** — o backend roda 24h no
[Render](https://render.com), sem depender de nenhum computador ligado.

> **Nota:** o plano gratuito do Render "dorme" depois de 15 min sem uso. Se o
> site estiver parado há um tempo, a primeira ação (ex.: login) pode levar
> uns 30-60s pra responder enquanto ele "acorda". Depois disso funciona
> normal.

## Progresso semanal

| Semana | Entrega | Status |
| --- | --- | --- |
| 1 | Login com conta Google e perfil básico (nickname + foto) | ✅ Na `main` |
| 2 | Vincular conta Steam/Riot Games/Battle.net; aviso de perfil Steam privado | ✅ Na `main` — [PR #7](../../pull/7) e [PR #8](../../pull/8) |
| 3 | Biblioteca de jogos da Steam (conquistas, privacidade) e destaques de perfil | ✅ Na `main` — [PR #11](../../pull/11) |
| 4 | Personalização do perfil 2 (descrição, mais tipos de destaque, edição) e detalhes de jogos na Biblioteca | ✅ Na `main` — [PR #14](../../pull/14) |
| 5 | Avaliação de usuários e recomendação de jogos | ✅ Na `main` — [PR #17](../../pull/17) e [PR #18](../../pull/18) |
| 6 | Menu lateral recolhível (estilo Google Classroom), dica ao passar o mouse nas recomendações, layout responsivo pra celular e deploy automático (GitHub Pages + Render) | ✅ Na `main` |

## Estrutura

- [`Sistemas A/`](Sistemas%20A) — entregas da disciplina de Sistemas A.
  - [`backend/`](Sistemas%20A/backend) — API em Java + Spring Boot, hospedada no Render.
  - [`frontend/`](Sistemas%20A/frontend) — interface em React (Vite), publicada no GitHub Pages.

---

## Como rodar o projeto localmente

Pré-requisitos: **Node.js 18+**, **JDK 17+** e **Git**. Não precisa instalar
Maven — o projeto já vem com o Maven Wrapper (`mvnw`/`mvnw.cmd`).

```bash
git clone https://github.com/Xanditto/XPV.git
cd XPV
```

**1. Backend** (terminal 1):

```powershell
cd "Sistemas A\backend"
# Só se `java -version` der erro de comando não encontrado:
# $env:JAVA_HOME = "C:\caminho\para\seu\jdk"

# Opcionais, para testar Steam/Riot Games (sem elas o resto funciona normal,
# só o botão daquela plataforma dá erro ao clicar):
$env:STEAM_API_KEY = "sua-chave"   # grátis em https://steamcommunity.com/dev/apikey (Domain Name: localhost)
$env:RIOT_API_KEY = "sua-chave"    # em https://developer.riotgames.com/ - expira em 24h

.\mvnw.cmd spring-boot:run
```

Quando aparecer `Started BackendApplication`, a API está em
`http://localhost:8080` (banco SQLite criado automaticamente, sem precisar
instalar nada). Battle.net ainda está em desenvolvimento (Issue #13).

**2. Frontend** (terminal 2, com o backend rodando):

```powershell
cd "Sistemas A\frontend"
copy .env.example .env
npm install
npm run dev
```

No `.env` criado, troque `VITE_GOOGLE_CLIENT_ID` por
`50898832420-up30ovtc4kodqfn9a0v1q1lng2ra7bfs.apps.googleusercontent.com`
(já configurado no backend, não é secreto). Abra `http://localhost:5173`.

**3. Primeiro login** — o banco começa vazio, então a primeira conta só pode
ser criada pelo login com Google (depois disso dá pra criar senha e usar
e-mail/nickname nas próximas vezes). Como o app está em modo "teste" no
Google Cloud, é preciso cadastrar o e-mail usado como **"usuário de
teste"** uma única vez em
[console.cloud.google.com](https://console.cloud.google.com/) → projeto XPV
→ **APIs e Serviços → Tela de consentimento OAuth → Usuários de teste →
Add users** — senão o Google bloqueia o login.

Fluxo: login com Google → define uma senha → cai no perfil (`/meu-perfil`). Nas próximas
vezes, entra só com e-mail/nickname + essa senha, sem precisar do Google.

### Testes automatizados do backend

```powershell
cd "Sistemas A\backend"
.\mvnw.cmd test
```

Não precisam de nenhuma chave — usam banco SQLite em memória, sem chamadas
reais às APIs externas.

---

## 🚀 Deploy (GitHub Pages + Render)

| | |
| --- | --- |
| **Frontend** | [GitHub Pages](https://xanditto.github.io/XPV/), publicado automaticamente via [`.github/workflows/deploy-pages.yml`](.github/workflows/deploy-pages.yml) a cada push na `main`. |
| **Backend** | [Render](https://render.com) (Docker), subido a partir do [`render.yaml`](render.yaml) e do [`Dockerfile`](Sistemas%20A/backend/Dockerfile). |

GitHub Pages só serve arquivos estáticos (não roda Java), então o backend
precisa de uma hospedagem à parte — por isso a dupla GitHub Pages + Render.

<details>
<summary><strong>Passo a passo da configuração (feita uma única vez)</strong></summary>

**GitHub Pages:**

1. `Settings → Pages → Build and deployment → Source` = **"GitHub Actions"**.
2. `Settings → Secrets and variables → Actions → Variables`, criar
   `VITE_API_URL` com a URL do backend no Render (ex.:
   `https://xpv-backend.onrender.com`).
3. `Google Cloud Console → projeto XPV → Credenciais → Client ID OAuth →
   Authorized JavaScript origins`, adicionar `https://xanditto.github.io`.

**Render:**

1. Criar conta grátis em [render.com](https://render.com) (dá pra entrar
   com o GitHub).
2. `New + → Blueprint` → selecionar o repositório `Xanditto/XPV` → o Render
   lê o `render.yaml` sozinho e propõe o serviço `xpv-backend`.
3. Deixar `STEAM_API_KEY`/`RIOT_API_KEY`/`BATTLENET_*` em branco (opcionais
   — sem elas só o botão daquela plataforma dá erro) e clicar em Deploy.
4. Depois do primeiro deploy, copiar a URL pública atribuída e colar em
   `Environment → APP_BASE_URL` do próprio serviço (usada nos links de
   retorno do login Steam/Battle.net).

O banco SQLite do Render é apagado a cada novo deploy/reinício do serviço
(plano gratuito não tem disco permanente) — normal para demonstração, só
significa que o primeiro login de novo precisa ser com o Google de novo.

**Plano B pra apresentação ao vivo**, caso o Render esteja "dormindo" bem na
hora (cold start): rodar o backend local com um túnel de domínio fixo
(`ngrok http 8080 --domain=seu-dominio.ngrok-free.app`) e trocar a
`VITE_API_URL` por essa URL temporariamente.

</details>
