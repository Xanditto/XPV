# XP Vault (XPV)

Plataforma que centraliza estatísticas de jogadores (biblioteca de jogos, horas
jogadas, conquistas) hoje espalhadas entre Steam, Riot Games, Battle.net e
outras plataformas, além de recursos sociais (comunidades, lista de amigos).

Este repositório acompanha o desenvolvimento incremental do projeto ao longo
da disciplina de Sistemas A, semana a semana, com o objetivo de futuramente
evoluir para um TCC.

## Progresso semanal

| Semana | Entrega | Status |
| --- | --- | --- |
| 1 | Login com conta Google e perfil básico (nickname + foto) | ✅ Na `main` |
| 2 | Vincular conta Steam/Riot Games/Battle.net; aviso de perfil Steam privado | ✅ Na `main` — [PR #7](../../pull/7) e [PR #8](../../pull/8) |
| 3 | Biblioteca de jogos da Steam (conquistas, privacidade) e destaques de perfil | ✅ Na `main` — [PR #11](../../pull/11) |
| 4 | Personalização do perfil 2 (descrição, mais tipos de destaque, edição) e detalhes de jogos na Biblioteca | ✅ Na `main` — [PR #14](../../pull/14) |
| 5 | Avaliação de usuários e recomendação de jogos | ✅ Na `main` — [PR #17](../../pull/17) e [PR #18](../../pull/18) |
| 6 | Menu lateral recolhível (estilo Google Classroom), dica ao passar o mouse nas recomendações, layout responsivo pra celular e deploy automático no GitHub Pages | ✅ Na `main` |

### 🔗 Acesso ao vivo

**https://xanditto.github.io/XPV/** — site publicado automaticamente (GitHub
Pages) a cada atualização da `main`, com login e dados funcionando sem
precisar do meu PC ligado: o backend roda 24h no [Render](https://render.com)
(ver seção "Backend sempre no ar" abaixo).

Única ressalva: o plano gratuito do Render "dorme" depois de 15 min sem uso,
então a primeira requisição depois de um tempo parado pode demorar uns
30-60 segundos pra responder (ele está "acordando"). Depois disso funciona
normal.

## Estrutura

- [`Sistemas A/`](Sistemas%20A) — entregas da disciplina de Sistemas A.
  - [`backend/`](Sistemas%20A/backend) — API em Java + Spring Boot.
  - [`frontend/`](Sistemas%20A/frontend) — interface em React (Vite).

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

Fluxo: login com Google → define uma senha → cai em `/inicio`. Nas próximas
vezes, entra só com e-mail/nickname + essa senha, sem precisar do Google.

### Testes automatizados do backend

```powershell
cd "Sistemas A\backend"
.\mvnw.cmd test
```

Não precisam de nenhuma chave — usam banco SQLite em memória, sem chamadas
reais às APIs externas.

---

## Deploy no GitHub Pages

O frontend é publicado automaticamente em
[xanditto.github.io/XPV](https://xanditto.github.io/XPV/) a cada push na
`main` que mexa em `Sistemas A/frontend/`, via
[`.github/workflows/deploy-pages.yml`](.github/workflows/deploy-pages.yml).
GitHub Pages só serve arquivos estáticos (sem rodar Java), então o backend
precisa estar hospedado à parte — ver seção seguinte.

Configuração única, só dá pra fazer pela própria conta do GitHub/Google
(não por código):

1. **Settings → Pages → Build and deployment → Source: "GitHub Actions"**
   — habilita o Pages no repositório.
2. **Settings → Secrets and variables → Actions → Variables**, criar
   `VITE_API_URL` com a URL do backend no Render (ex.:
   `https://xpv-backend.onrender.com`) — ver próxima seção. Como o Render
   não muda de endereço a cada reinício (diferente do túnel de antes), essa
   variável só precisa ser configurada uma vez.
3. **Google Cloud Console → projeto XPV → Credenciais → o Client ID OAuth →
   "Authorized JavaScript origins"**, adicionar
   `https://xanditto.github.io` — senão o botão de login com Google recusa
   a origem.

## Backend sempre no ar (Render)

O backend sobe automaticamente no [Render](https://render.com) a partir do
[`render.yaml`](render.yaml) e do
[`Dockerfile`](Sistemas%20A/backend/Dockerfile) deste repositório (plano
gratuito, "Web Service" via Docker).

**Configuração única:**

1. Criar conta grátis em [render.com](https://render.com) (dá pra entrar
   direto com o GitHub).
2. **New + → Blueprint** → selecionar o repositório `Xanditto/XPV` → o
   Render lê o `render.yaml` sozinho e já propõe o serviço `xpv-backend`.
3. Preencher as variáveis marcadas como obrigatórias (`STEAM_API_KEY` e
   `RIOT_API_KEY` podem ficar em branco - sem elas só o botão daquela
   plataforma dá erro) e clicar em **Apply/Deploy**.
4. Depois do primeiro deploy, o Render mostra a URL pública atribuída
   (algo como `https://xpv-backend-xxxx.onrender.com`). Copiar essa URL e:
   - Colar em **Settings → Environment** do próprio serviço, na variável
     `APP_BASE_URL` (usada nos links de retorno do login Steam/Battle.net).
   - Colar como `VITE_API_URL` nas Variables do GitHub Actions (passo 2 da
     seção anterior) e rodar o workflow do Pages de novo (`Actions` →
     `Deploy frontend no GitHub Pages` → `Run workflow`), pra o site
     publicado passar a apontar pro Render.

O banco SQLite do Render é apagado a cada novo deploy/reinício do serviço
(plano gratuito não tem disco permanente) — normal para demonstração, só
significa que o primeiro login de novo precisa ser com o Google de novo.

### Plano B pra apresentação ao vivo

Por causa do "cold start" (30-60s a primeira vez que alguém acessa depois
de um tempo parado), pra apresentação ao vivo vale a pena também deixar o
backend rodando local com um túnel de domínio fixo, como plano B caso o
Render esteja "dormindo" bem na hora:

```powershell
# Backend local rodando (Passo 1 de "Como rodar o projeto localmente")
ngrok http 8080 --domain=seu-dominio-fixo.ngrok-free.app
```

(o domínio fixo é criado uma vez em
[dashboard.ngrok.com](https://dashboard.ngrok.com) → Domains, no plano
grátis). Se precisar usar esse plano B na hora, é só trocar a variável
`VITE_API_URL` do GitHub Actions pra essa URL do ngrok e rodar o workflow
de novo.
