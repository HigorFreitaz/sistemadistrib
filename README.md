# SD Garagem — Rastreador Inteligente de Ocupação de Garagens de Estacionamento

Projeto Final da disciplina de Sistemas Distribuídos (UTFPR-PG / DAINF —
Tecnologia em Análise e Desenvolvimento de Software, Prof. Dr. Richard
Ribeiro). Esta entrega é a **Entrega Parcial 1 (EP-1)**.

## Tema e contexto

Garagens comerciais costumam ter displays digitais nos portões de entrada
mostrando, em tempo real, quantas vagas estão livres e como elas se
distribuem por andar. É esse cenário que o projeto simula: os clientes
representam sensores de portão de entrada/saída (ou o app de um
motorista), conversando com um servidor central que gerencia a garagem.

O desafio de sistema distribuído por trás disso é manter o estado
consistente com vários terminais de portão mexendo na garagem ao mesmo
tempo — a contagem de vagas não pode nunca ficar negativa nem ser lida
pela metade. Essa entrega ainda não mexe com vagas, mas já deixa o
padrão pronto: a criação e remoção de sessão em `SessionService` roda
dentro de uma região crítica isolada com `synchronized`, que é
exatamente o molde que a EP-2 vai reaproveitar para proteger a contagem
de vagas.

## O que está pronto nesta entrega

Login, logout e cadastro de usuário funcionando ponta a ponta: cliente e
servidor em JavaFX, comunicação por socket TCP com JSON, usuários e
sessões persistidos em arquivo, e os validadores de `username`/
`password` cobertos por testes.

Ficou fora do escopo (vai para a EP-2, ver mais abaixo): consultar,
atualizar ou excluir um cadastro, o CRUD de vagas/operações e qualquer
funcionalidade do perfil ADM. Os pontos onde isso vai entrar estão
marcados no código com `// TODO EP-2:`.

## Arquitetura

```
sd-garagem/            (pom pai, packaging pom)
├── common/             modelo de domínio + protocolo + validadores
│                       (não depende de server/client nem de JavaFX)
├── server/             socket server, persistência JSON, GUI do servidor
└── client/             GUI do cliente, conector de socket
```

O `common` é a única fonte de verdade do formato das mensagens — é o que
garante que cliente e servidor (e, mais pra frente, os de outros colegas)
falem a mesma língua. Além do modelo (`User`, `Session`) e do protocolo
(`LoginRequest`, `RegisterRequest`, `Response`, `StatusCode`...), ele
também guarda os validadores de formato e a folha de estilo (`css/`) que
as duas GUIs importam.

O `server` cuida do socket (`GarageServer`, `ClientHandler`,
`RequestDispatcher`), da persistência em JSON com escrita atômica, do
hash de senha (PBKDF2) e da própria janela de status/log. O `client` só
tem o conector de socket e as telas (login, cadastro, principal) com
seus respectivos FXML — nada de lógica de negócio no controller, isso
fica nos validadores e serviços de `common`/`server`.

### Como um login acontece

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

A fonte da verdade é a planilha de protocolo (mantida por Nathan e
Rafael) — hoje só o login está fechado nela; logout e cadastro seguem o
mesmo padrão por analogia, documentado como suposição mais abaixo.

O framing é simples: um objeto JSON por linha (newline-delimited JSON),
sempre em UTF-8, lido com `BufferedReader.readLine()` e escrito com
`PrintWriter` em auto-flush (`common.transport.MessageIO`).

### Login

```json
// cliente -> servidor
{"method":"login","username":"admin","password":"Admin@123"}
```
```json
// sucesso
{"statusCode":200,"message":"Login realizado com sucesso","data":{"token":"3fa2...uuid"}}
// usuario nao encontrado
{"statusCode":401,"message":"Usuario nao encontrado","data":null}
// senha incorreta
{"statusCode":401,"message":"Senha incorreta","data":null}
```

### Cadastro

Não abre sessão — depois de cadastrar, o usuário precisa logar
separadamente.

```json
// cliente -> servidor
{"method":"register","username":"novo.usuario","password":"SenhaForte9"}
```
```json
// sucesso
{"statusCode":200,"message":"Cadastro realizado com sucesso","data":null}
// username ja existe
{"statusCode":409,"message":"Usuario ja cadastrado","data":null}
```

### Logout

```json
// cliente -> servidor
{"method":"logout","token":"3fa2...uuid"}
```
```json
// sucesso
{"statusCode":200,"message":"Logout realizado com sucesso","data":null}
// token invalido ou ja expirado
{"statusCode":401,"message":"Token invalido ou sessao inexistente","data":null}
```

### Códigos de status

Centralizados em `common.protocol.StatusCode`, com semântica de HTTP:
`200` sucesso, `400` requisição malformada ou fora do formato, `401`
credenciais ou token inválidos, `404` operação não suportada, `409`
conflito (username já cadastrado), `500` erro interno.

## Requisitos não funcionais de validação

**`username`:** só letras minúsculas e números, com `.` e `_` liberados
como símbolos; entre 3 e 20 caracteres; sem acento nem espaço.

**`password`:** letras maiúsculas, minúsculas, números e os símbolos
`# . * & % $ @ ! ( ) - _ = +`; nada além disso; entre 8 e 20 caracteres.

Os dois validadores moram em `common.validation` e rodam duas vezes: no
cliente, para dar feedback imediato (o checklist da tela de cadastro), e
de novo no servidor, porque o cliente nunca é confiável. Testes JUnit 5
cobrem os casos de borda dos dois.

## Como rodar

### Pelo IntelliJ IDEA Ultimate

1. Abra a pasta como projeto **Maven** — o IntelliJ acha o `pom.xml` pai
   sozinho.
2. Configure o **JDK 21** no projeto (File → Project Structure → SDK).
3. Crie duas run configurations Maven: uma para o **servidor**
   (diretório de trabalho `server/`, comando `javafx:run`) e outra para
   o **cliente** (diretório de trabalho `client/`, mesmo comando).
4. Rode o servidor primeiro e clique em **Iniciar** na janela dele; só
   depois rode o cliente e logue com `admin` / `Admin@123` (a senha
   padrão criada no primeiro início — o aviso pra trocá-la aparece no
   log do servidor).

O servidor grava `dados/usuarios.json` e `dados/sessoes.json` relativos
ao diretório de trabalho, por isso a run configuration dele precisa
apontar para `server/`.

### Por linha de comando

```bash
# instala o common no repositorio local (uma vez, ou sempre que ele mudar)
./mvnw install -pl common -am -DskipTests

# servidor, a partir de server/ (porta default 5555)
cd server && ../mvnw javafx:run

# cliente, a partir de client/, em outro terminal
cd client && ../mvnw javafx:run
```

A porta muda em `server/server.properties` ou no primeiro argumento de
linha de comando.

### Ou só clique duas vezes: `iniciar.cmd`

Na raiz do repositório tem um lançador único: `iniciar.cmd` sobe o
servidor e abre o cliente ao mesmo tempo, cada um na sua janela. Se a
máquina não tiver `JAVA_HOME` configurado, ele usa o JBR que vem junto
do IntelliJ IDEA instalado.

## Suposições a validar com a turma

O protocolo ainda está sendo fechado por Nathan e Rafael, e alguns
comportamentos não estavam escritos em lugar nenhum — então tomamos
decisões e documentamos aqui em vez de travar o desenvolvimento.

**Sobre o protocolo:**
- Nem `logout` nem `register` estão na planilha ainda. Os dois seguem o
  mesmo padrão estrutural do login (`method` na requisição,
  `statusCode`/`message`/`data` na resposta); o nome `register` foi
  escolhido em inglês pra ficar consistente com `login`/`logout`.
- A planilha também não fixa os valores de `statusCode`; usamos
  semântica HTTP, centralizada numa única classe, fácil de renegociar
  depois.
- O requisito de minúsculas obrigatórias, no documento, é só para o
  valor de `method`. O texto de `message` é para leitura humana, então
  começa com maiúscula como o resto da interface.

**Sobre login e segurança:**
- Um usuário não acumula sessões: logar de novo derruba a sessão
  anterior dele.
- O documento de requisitos pede mensagens diferentes para "usuário não
  encontrado" e "senha incorreta", e o servidor realmente devolve isso
  no campo `message`. Mas o cliente nunca repassa essa diferença pro
  usuário final — qualquer login que falhe vira a mesma notificação
  genérica, "Usuário e/ou senha incorretos.", pra não dar pista de qual
  campo errou.
- Por isso também o login não valida usuário/senha em tempo real
  enquanto o usuário digita (diferente do cadastro, que mostra um
  checklist ao vivo — ali faz sentido, porque é uma senha nova sendo
  criada). No login a senha já existe; reagir a cada tecla só daria
  pistas sobre a política de senha sem necessidade.
- Cadastro não loga automaticamente: depois de criar a conta, o cliente
  volta pra tela de login com o username já preenchido, mantendo as
  duas ações separadas como o documento descreve. Todo usuário criado
  por esse caminho nasce com papel `CLIENTE` — não dá pra criar um ADM
  pelo protocolo nesta entrega (o único é o `admin` da seed inicial).

**Sobre o servidor:**
- O último acesso de uma sessão é atualizado em memória a cada uso do
  token, mas só vai pro disco quando a sessão é criada ou encerrada —
  não faz sentido gravar arquivo a cada requisição autenticada.
- O pool de threads do servidor está fixo em 50 conexões simultâneas
  (constante em `GarageServer`).
- Tanto a pasta de dados quanto o `server.properties` são relativos ao
  diretório de trabalho do processo, não empacotados como recurso —
  assim dá pra editar sem recompilar.

## Próximas entregas

**EP-2** (pontos marcados no código com `// TODO EP-2:`): os demais
`methods` do protocolo (consulta/atualização/exclusão de cadastro, CRUD
de vagas e operações, CRUD do admin) assim que a planilha fechar; o
painel de vagas disponíveis por andar na tela principal do cliente; e a
região crítica da contagem de vagas, seguindo o mesmo isolamento já
usado para sessão.

**EP-3**: testes em rede com clientes e servidores dos outros alunos, e
a funcionalidade completa do perfil ADM.
