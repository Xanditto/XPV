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
| 4 | Personalização do perfil 2 (descrição, mais tipos de destaque, edição) e detalhes de jogos na Biblioteca | 🔎 Em revisão — [PR #14](../../pull/14) |
| 5 | Avaliação de usuários e recomendação de jogos | 📝 Planejamento — [Issue #15](../../issues/15) e [Issue #16](../../issues/16) |

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

## Testando a Semana 4 (ainda em Pull Request)

Essa parte ainda não está na `main` — está aguardando aprovação em reunião
em [PR #14](../../pull/14). Para rodar essa versão localmente, depois do
Passo 1 acima:

```powershell
git checkout feature/perfil-estilo-steam
```

Essa branch já contém tudo das semanas anteriores. Repita os Passos 2 e 3
normalmente.
