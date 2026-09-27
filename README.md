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
| 2 | Vincular conta Steam/Riot Games/Battle.net; aviso de perfil Steam privado | 🔎 Em revisão — [PR #7](../../pull/7) e [PR #8](../../pull/8) |

## Estrutura

- [`Sistemas A/`](Sistemas%20A) — entregas da disciplina de Sistemas A.
  - [`backend/`](Sistemas%20A/backend) — API em Java + Spring Boot.
  - [`frontend/`](Sistemas%20A/frontend) — interface em React (Vite).

---

## Como rodar o projeto localmente (o que está na `main` — Semana 1)

### O que precisa estar instalado no computador

| Programa | Versão | Link para baixar |
| --- | --- | --- |
| **Node.js** | 18 ou mais recente (LTS) | https://nodejs.org/ |
| **JDK (Java)** | 17 ou mais recente | https://adoptium.net/ (ou qualquer outra distribuição, ex. Oracle) |
| **Git** | qualquer versão recente | https://git-scm.com/downloads |

Não é necessário instalar Maven separadamente — o projeto já vem com o
"Maven Wrapper" (`mvnw` / `mvnw.cmd`), que baixa o Maven automaticamente na
primeira execução.

### Passo 1 — Clonar o repositório

```bash
git clone https://github.com/Xanditto/XPV.git
cd XPV
```

### Passo 2 — Descobrir o caminho do seu JDK

O comando abaixo confirma se o Java está instalado e mostra a versão:

```powershell
java -version
```

Se der erro de "comando não encontrado", o JDK está instalado mas não está
no PATH do Windows — nesse caso, anote o caminho da instalação (geralmente
algo como `C:\Program Files\Java\jdk-21` ou `C:\Program Files\Eclipse
Adoptium\jdk-21...`) para usar no Passo 3.

### Passo 3 — Rodar o backend

Em um terminal, dentro da pasta `Sistemas A/backend`:

```powershell
cd "Sistemas A\backend"

# Só é necessário se o comando `java -version` do Passo 2 tiver dado erro:
$env:JAVA_HOME = "C:\caminho\para\seu\jdk"

.\mvnw.cmd spring-boot:run
```

Na primeira execução ele baixa o Maven e todas as dependências — pode levar
alguns minutos. Quando aparecer `Started BackendApplication`, o backend está
rodando em `http://localhost:8080`. O banco de dados (SQLite) é criado
automaticamente em `backend/data/xpv.db`, sem precisar instalar nada.

### Passo 4 — Rodar o frontend

Em **outro** terminal (deixe o backend rodando no primeiro), dentro da pasta
`Sistemas A/frontend`:

```powershell
cd "Sistemas A\frontend"
copy .env.example .env
npm install
npm run dev
```

Abra o arquivo `.env` que acabou de ser criado e cole esta linha (é o mesmo
Client ID do Google já configurado por padrão no backend — não é secreto):

```
VITE_GOOGLE_CLIENT_ID=50898832420-up30ovtc4kodqfn9a0v1q1lng2ra7bfs.apps.googleusercontent.com
```

Acesse **http://localhost:5173** no navegador.

### ⚠️ Passo importante para conseguir logar

O banco de dados começa vazio, e a única forma de criar a primeira conta é
pelo login com Google (só depois disso é possível criar uma senha e usar
e-mail/nickname). Como o app está em modo "teste" no Google Cloud, **é
preciso adicionar o e-mail do Google que for usado para testar como
"usuário de teste"**, senão o Google bloqueia o login.

Isso é feito pelo aluno, uma única vez, em
[console.cloud.google.com](https://console.cloud.google.com/) → selecionar
o projeto do XPV → **APIs e Serviços → Tela de consentimento OAuth →
Usuários de teste → Add users**.

### Fluxo esperado

1. Abrir `http://localhost:5173` → redireciona para `/login`.
2. **Primeira vez**: clicar no botão do Google e escolher a conta → é levado
   para `/definir-senha` → escolhe uma senha → cai em `/inicio`, já com
   nickname e foto vindos da conta Google.
3. Na tela `/inicio`, clicar em **Editar perfil** para trocar nickname/foto,
   ou em **Sair** para deslogar.
4. **Próximas vezes**: na tela de login, digitar o e-mail ou o nickname e a
   senha criada no passo 2, sem precisar do Google.

### Rodar os testes automatizados do backend

```powershell
cd "Sistemas A\backend"
.\mvnw.cmd test
```

Não precisam de nenhuma chave configurada — usam um banco SQLite em memória
e não fazem chamadas reais às APIs externas.

---

## Testando a Semana 2 (Steam / Riot Games / Battle.net) — ainda em Pull Request

Essa parte ainda não está na `main` — está nas branches abaixo, aguardando
aprovação em reunião (conforme o fluxo pedido pelo professor):

- [PR #7 — Vincular conta Steam/Riot Games/Battle.net](../../pull/7)
- [PR #8 — Aviso de perfil Steam privado](../../pull/8)

Para rodar essa versão localmente, depois de fazer os Passos 1 e 2 acima:

```powershell
git checkout feature/aviso-perfil-privado-steam
```

Essa branch já contém tudo (Semana 1 + Semana 2 juntas). Repita os Passos 3
e 4 normalmente. Além do login com Google, essa versão permite vincular
contas de:

- **Steam** — gere uma chave gratuita em
  https://steamcommunity.com/dev/apikey (Domain Name: `localhost`) e rode o
  backend com `$env:STEAM_API_KEY = "sua-chave"` antes do Passo 3.
- **Riot Games** (League of Legends/Valorant/TFT) — copie a "Development
  API Key" em https://developer.riotgames.com/ e use como `$env:RIOT_API_KEY`.
  ⚠️ Essa chave expira em 24h e precisa ser gerada de novo a cada dia de teste.
- **Battle.net** — 🚧 ainda em desenvolvimento (o cadastro do app na Blizzard
  não foi finalizado ainda).

Sem essas chaves, o resto do sistema funciona normalmente — só o botão
daquela plataforma específica mostra um erro ao ser clicado.
