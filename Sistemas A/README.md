# XPV — Sistemas A

Entrega da Semana 1: login do usuário com conta Google e um perfil básico
(nickname + foto), que o usuário pode editar.

## Arquitetura desta entrega

O login funciona em duas etapas:

- **Primeira vez**: o usuário entra com a conta Google e, na sequência, cria
  uma senha.
- **Próximas vezes**: entra digitando o e-mail (o mesmo da conta Google) ou o
  nickname escolhido, junto com essa senha — sem precisar do Google de novo.

- **Backend** (`backend/`): Java 21 + Spring Boot 4, com banco SQLite. Rotas:
  - `POST /api/auth/google`: recebe o ID Token do Google Identity Services,
    valida junto ao Google, cria o usuário no banco (se ainda não existir) e
    devolve um token de sessão. A resposta indica `precisaDefinirSenha: true`
    quando o usuário ainda não tem senha cadastrada.
  - `POST /api/auth/senha`: define a senha do usuário logado (chamada logo
    após o primeiro login com Google).
  - `POST /api/auth/login`: login com `identificador` (e-mail ou nickname) +
    `senha`.
  - `GET /api/profile` / `PUT /api/profile`: consulta e atualiza o
    nickname/foto do usuário logado (autenticado via `Authorization: Bearer
    <token>`).

  A senha é guardada com hash bcrypt (`spring-security-crypto`). A sessão é
  guardada em memória (sem expiração ainda) — é o suficiente para o MVP desta
  semana; nas próximas entregas isso deve evoluir para algo mais robusto (JWT
  próprio ou Spring Security completo).

- **Frontend** (`frontend/`): React + Vite. Páginas:
  - `/login`: formulário de e-mail/nickname + senha (topo) e, abaixo de uma
    divisória "Primeira vez por aqui?", o botão do Google.
  - `/definir-senha`: tela para criar a senha, mostrada só depois do primeiro
    login com Google.
  - `/inicio`: tela principal (ainda vazia nesta entrega), com o nickname, a
    foto e os botões "Editar perfil"/"Sair" no canto superior direito.
  - `/perfil`: formulário para editar nickname e foto, acessado pelo botão
    "Editar perfil".

## Pré-requisitos

- Node.js (já instalado)
- JDK 17+ (neste ambiente, o JDK está em `C:\Program Files\Java\jdk-26.0.2`,
  mas não está no PATH — veja como configurar o `JAVA_HOME` abaixo)

## Passo 1 — Criar o Client ID do Google (OAuth)

O login usa o **Google Identity Services**: o próprio navegador do usuário
conversa com o Google, então não é preciso senha própria nem back-end
recebendo credenciais — só um "Client ID" público, criado uma única vez.

1. Acesse https://console.cloud.google.com/ e crie um projeto (ou use um
   existente).
2. No menu, vá em **APIs e Serviços → Tela de consentimento OAuth**.
   Escolha "Externo", preencha nome do app ("XP Vault") e seu e-mail. Pode
   deixar como "Em teste" por enquanto.
3. Vá em **APIs e Serviços → Credenciais → Criar Credenciais → ID do cliente
   OAuth**.
4. Tipo de aplicativo: **Aplicativo da Web**.
5. Em **Origens JavaScript autorizadas**, adicione:
   `http://localhost:5173`
6. Crie. Copie o **Client ID** gerado (algo como
   `123456789-abc.apps.googleusercontent.com`).

Não existe "Client Secret" a proteger aqui — esse fluxo (ID Token no
navegador) não usa o secret.

## Passo 2 — Configurar as variáveis de ambiente

**Frontend:**

```bash
cd frontend
cp .env.example .env
```

Edite `.env` e cole o Client ID copiado em `VITE_GOOGLE_CLIENT_ID`.

**Backend:** defina a variável de ambiente `GOOGLE_CLIENT_ID` com o mesmo
valor antes de iniciar (evita que alguém use um ID Token de outro app). No
PowerShell:

```powershell
$env:GOOGLE_CLIENT_ID = "seu-client-id.apps.googleusercontent.com"
```

## Passo 3 — Rodar o backend

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-26.0.2"
cd backend
.\mvnw.cmd spring-boot:run
```

Sobe em `http://localhost:8080`. O arquivo do banco SQLite é criado
automaticamente em `backend/data/xpv.db` (ignorado pelo Git).

## Passo 4 — Rodar o frontend

```powershell
cd frontend
npm install
npm run dev
```

Acesse `http://localhost:5173`.

## Fluxo esperado

1. Abrir `http://localhost:5173` → redireciona para `/login`.
2. **Primeira vez**: clicar no botão do Google (abaixo de "Primeira vez por
   aqui?") e escolher a conta → é levado para `/definir-senha` → escolhe uma
   senha → cai em `/inicio`, já com nickname e foto vindos da conta Google.
3. Na tela `/inicio`, clicar em **Editar perfil** para trocar nickname/foto,
   ou em **Sair** para deslogar.
4. **Próximas vezes**: na tela de login, digitar o e-mail ou o nickname e a
   senha criada no passo 2, sem precisar do Google.
