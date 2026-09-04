# SD Garagem — Rastreador Inteligente de Ocupação de Garagens de Estacionamento

Projeto Final da disciplina de Sistemas Distribuídos (UTFPR-PG / DAINF —
Tecnologia em Análise e Desenvolvimento de Software, Prof. Dr. Richard
Ribeiro). Esta entrega corresponde à **Entrega Parcial 1 (EP-1)**.

## Tema e contexto

Garagens comerciais usam displays digitais nos portões de entrada para
mostrar, em tempo real, a contagem de vagas disponíveis e a disponibilidade
por andar. Neste projeto, os clientes simulam sensores de portão de
entrada/saída ou aplicativos de motoristas, comunicando-se com um servidor
central de gerenciamento da garagem.

**Desafio de sistema distribuído:** múltiplos terminais de portão podem
operar simultaneamente sobre o mesmo servidor. A arquitetura precisa manter
a consistência do estado compartilhado para que a contagem de vagas nunca
fique negativa nem seja lida de forma inconsistente. Nesta entrega isso se
reflete na forma como sessões são criadas/removidas (região crítica
isolada com `synchronized` em `SessionService`) — o mesmo padrão que a
EP-2 vai usar para proteger a contagem de vagas.

## Escopo desta entrega (EP-1)

Implementado: estrutura Maven multi-módulo, JavaFX no cliente e no
servidor, comunicação por sockets TCP com JSON, login, logout e cadastro
de usuário ponta a ponta, persistência de usuários e sessões em arquivos
JSON, GUI no cliente e no servidor, e os validadores de `username`/
`password` com testes unitários.

**Fora do escopo** (ver "Próximas entregas"): consulta/atualização/exclusão
de cadastro, CRUD de vagas/operações, perfil ADM. Os pontos de extensão
estão marcados no código com `// TODO EP-2:`.

## Arquitetura

```
sd-garagem/            (pom pai, packaging pom)
├── common/             modelo de domínio + protocolo + validadores
│                       (não depende de server/client nem de JavaFX)
├── server/             socket server, persistência JSON, GUI do servidor
└── client/             GUI do cliente, conector de socket
```

- **common**: `model` (User, UserRole, Session), `protocol` (LoginRequest,
  LogoutRequest, RegisterRequest, Response, TokenData, StatusCode,
  Methods), `validation` (UsernameValidator, PasswordValidator), `json`
  (configuração do Gson), `transport` (framing de linha JSON em UTF-8) e
  `css` (folha de estilo compartilhada pelas GUIs de cliente e servidor).
- **server**: `config` (porta), `security` (hash PBKDF2), `repository`
  (persistência JSON com escrita atômica), `service` (AuthService,
  SessionService, LoginResult), `net` (GarageServer, ClientHandler,
  RequestDispatcher), `log` (mascaramento de senha), `ui` (GUI JavaFX).
- **client**: `net` (SocketConnector), `ui` (ClienteApp, LoginController,
  RegisterController, MainController) + FXML.

### Fluxo de login (diagrama)

```
Cliente (LoginController)                     Servidor (GarageServer)
        |                                              |
        |--- 1. abre socket TCP (host:porta) --------->|
        |                                              |--- aceita conexao;
        |                                              |    ClientHandler dedicado
        |                                              |    (thread do pool)
        |--- 2. {"method":"login",                     |
        |        "username":"...",                     |
        |        "password":"..."}       ------------->|
        |                                              |--- RequestDispatcher
        |                                              |      -> valida formato (common)
        |                                              |      -> AuthService.login()
        |                                              |           -> UserRepository (usuarios.json)
        |                                              |           -> PasswordHasher.matches()
        |                                              |           -> SessionService.login()
        |                                              |                (gera token UUID v4,
        |                                              |                 invalida sessao anterior,
        |                                              |                 grava sessoes.json)
        |<--- 3. {"statusCode":200,                    |
        |         "message":"...",                     |
        |         "data":{"token":"..."}} -------------|
        |                                              |
        | (mantem o socket aberto, guarda o token)      |
        |                                              |
        |--- 4. {"method":"logout",                    |
        |        "token":"..."}          ------------->|
        |                                              |--- SessionService.logout()
        |                                              |    remove a sessao, grava sessoes.json
        |<--- 5. {"statusCode":200,                    |
        |         "message":"...","data":null} --------|
        |                                              |
        |--- 6. fecha o socket                          |
```

## Protocolo de troca de mensagens

Fonte da verdade: `docs/Protocolo de Troca de Mensagens.xlsx` (hoje só o
login está fechado). Framing: um objeto JSON por linha (newline-delimited
JSON), UTF-8, lido com `BufferedReader.readLine()` e escrito com
`PrintWriter` em auto-flush (ver `common.transport.MessageIO`).

### Login

Requisição (cliente → servidor):
```json
{"method":"login","username":"admin","password":"Admin@123"}
```

Resposta de sucesso (servidor → cliente):
```json
{"statusCode":200,"message":"Login realizado com sucesso","data":{"token":"3fa2...uuid"}}
```

Resposta de erro (usuário não encontrado):
```json
{"statusCode":401,"message":"Usuario nao encontrado","data":null}
```

Resposta de erro (senha incorreta):
```json
{"statusCode":401,"message":"Senha incorreta","data":null}
```

### Cadastro

Formato ainda não definido na planilha; implementado seguindo o fluxo
descrito em `docs/Requisitos Funcionais e não funcionais.docx` e o mesmo
padrão estrutural do login (ver seção de suposições abaixo). Não abre
sessão — o usuário precisa logar separadamente depois de se cadastrar.

Requisição (cliente → servidor):
```json
{"method":"register","username":"novo.usuario","password":"SenhaForte9"}
```

Resposta de sucesso:
```json
{"statusCode":200,"message":"Cadastro realizado com sucesso","data":null}
```

Resposta de erro (username já cadastrado):
```json
{"statusCode":409,"message":"Usuario ja cadastrado","data":null}
```

### Logout

Formato ainda não definido na planilha; implementado seguindo o mesmo
padrão estrutural do login (ver seção de suposições abaixo).

Requisição (cliente → servidor):
```json
{"method":"logout","token":"3fa2...uuid"}
```

Resposta de sucesso:
```json
{"statusCode":200,"message":"Logout realizado com sucesso","data":null}
```

Resposta de erro (token inválido ou inexistente):
```json
{"statusCode":401,"message":"Token invalido ou sessao inexistente","data":null}
```

### Códigos de status

Centralizados em `common.protocol.StatusCode`, com semântica inspirada em
HTTP: `200` sucesso, `400` requisição malformada/validação, `401`
credenciais ou token inválidos, `404` operação não suportada, `409`
conflito (ex.: username já cadastrado), `500` erro interno.

## Requisitos não funcionais de validação

**`username`:** somente letras minúsculas e números; símbolos especiais
liberados `.` e `_`; mínimo 3, máximo 20 caracteres; sem acentuação nem
espaços.

**`password`:** letras maiúsculas, minúsculas, números e os símbolos
`# . * & % $ @ ! ( ) - _ = +`; nenhum outro caractere é aceito; mínimo 8,
máximo 20 caracteres.

Ambos implementados em `common.validation`, validados no cliente (feedback
imediato na tela de cadastro, como um checklist) e novamente no servidor
(o cliente nunca é confiável), com testes JUnit 5 cobrindo os casos de
borda.

## Como rodar no IntelliJ IDEA Ultimate

1. Abrir a pasta do repositório e importar como projeto **Maven** (o
   IntelliJ detecta o `pom.xml` pai automaticamente).
2. Configurar o **JDK 21** no projeto (File → Project Structure → SDK).
3. Criar duas run configurations do tipo **Maven**:
   - **Servidor**: diretório de trabalho `server/`, comando `javafx:run`.
   - **Cliente**: diretório de trabalho `client/`, comando `javafx:run`.
4. Rodar primeiro o servidor e clicar em **Iniciar** na GUI; depois rodar
   o cliente e fazer login com `admin` / `Admin@123` (senha padrão criada
   no primeiro início — troque-a depois, o aviso aparece no log do
   servidor).

O servidor grava `dados/usuarios.json` e `dados/sessoes.json` relativos ao
diretório de trabalho — por isso a run configuration do servidor deve ter
o diretório de trabalho apontando para `server/`.

## Como rodar por linha de comando

```bash
# instala o modulo common no repositorio local (uma vez, ou apos alterar o common)
./mvnw install -pl common -am -DskipTests

# servidor (a partir da pasta server/, porta default 5555)
cd server && ../mvnw javafx:run

# cliente (a partir da pasta client/, em outro terminal)
cd client && ../mvnw javafx:run
```

A porta do servidor pode ser trocada em `server/server.properties` ou
passada como primeiro argumento de linha de comando.

### Atalho: `iniciar.cmd`

Para não precisar abrir terminal nem IntelliJ, um lançador único fica na
raiz do repositório: dois cliques em `iniciar.cmd` abrem o servidor e o
cliente ao mesmo tempo, cada um na sua própria janela de console. Se
`JAVA_HOME` não estiver definido no sistema, ele usa como alternativa o
JBR que acompanha o IntelliJ IDEA instalado na máquina.

## Suposições a validar com a turma

- **Framing:** um objeto JSON por linha (newline-delimited), UTF-8. Opção
  mais simples e que sobrevive à interoperabilidade entre implementações
  diferentes.
- **Valores de `statusCode`:** a planilha não fixa esses números; usamos
  semântica HTTP, centralizada em `StatusCode`, fácil de renegociar.
- **Formato do `logout` e do `register`:** nenhum dos dois está na
  planilha; seguimos o mesmo padrão estrutural do login (`method` na
  requisição, `statusCode`/`message`/`data` na resposta). O nome do
  method de cadastro (`register`) foi escolhido em inglês, seguindo a
  mesma convenção de `login`/`logout`.
- **Política de sessão única:** um usuário não acumula sessões — logar de
  novo invalida a sessão anterior dele.
- **Capitalização de `message`:** o requisito de minúsculas obrigatórias
  vale só para o valor de `method` (é explícito no documento). O texto de
  `message` é para leitura humana, então começa com maiúscula — igual ao
  resto dos textos da interface.
- **Categorias obrigatórias de `password`:** o documento de requisitos não
  exige que todas as categorias de caractere (maiúscula/minúscula/número/
  símbolo) estejam presentes simultaneamente; validamos o conjunto de
  caracteres permitido e o tamanho (8 a 20).
- **Mensagens distintas no login, só no servidor:** `Requisitos Funcionais
  e não funcionais.docx` pede explicitamente mensagens diferentes para
  "usuário não encontrado" e "senha incorreta", e o servidor devolve
  exatamente isso no campo `message`. O cliente, porém, nunca repassa essa
  mensagem ao usuário: qualquer login que não dê certo (usuário, senha ou
  os dois) aparece como a mesma notificação genérica "Usuário e/ou senha
  incorretos.", para não deixar visível qual dos dois campos errou.
- **Sem validação de formato em tempo real no login:** ao contrário do
  cadastro (que mostra um checklist ao vivo, útil para criar uma senha
  nova), o login não valida usuário/senha enquanto o usuário digita —
  são credenciais que já existem, e reagir a cada tecla digitada só
  daria pistas sobre a política de senha sem necessidade. O formato é
  responsabilidade do servidor; qualquer erro vira a mesma notificação
  genérica de credenciais inválidas.
- **Persistência do último acesso da sessão:** atualizado em memória a
  cada uso do token, mas só é gravado em disco quando a sessão é criada ou
  encerrada (evita escrita a cada requisição autenticada).
- **Tamanho do pool de threads do servidor:** fixado em 50 conexões
  simultâneas (constante em `GarageServer`).
- **Diretório de dados e `server.properties`:** ambos relativos ao
  diretório de trabalho do processo (não empacotados como recurso), para
  poderem ser editados sem recompilar.
- **Papel do cadastro via `register`:** todo usuário cadastrado por esse
  method nasce com papel `CLIENTE`; não há como criar um ADM pelo
  protocolo nesta entrega (o único ADM é o `admin` da seed inicial).
- **Cadastro não abre sessão:** depois de cadastrar, o cliente volta para
  a tela de login (com o username já preenchido) em vez de logar
  automaticamente — mantém cadastro e login como passos separados, como
  descrito no documento de requisitos.

## Próximas entregas

**EP-2** (marcado no código com `// TODO EP-2:`):
- Demais `methods` do protocolo (consulta/atualização/exclusão de
  cadastro, CRUD de vagas/operações, CRUD admin), assim que Nathan e
  Rafael fecharem a planilha.
- Painel de vagas disponíveis por andar na tela principal do cliente.
- A região crítica da contagem de vagas, usando o mesmo padrão de
  isolamento já usado em `SessionService` (criação/remoção de sessão).

**EP-3**: testes em rede com clientes e servidores de outros alunos;
funcionalidade completa do perfil ADM.
