# SD Garagem — Rastreador Inteligente de Ocupação de Garagens de Estacionamento

Projeto Final da disciplina de Sistemas Distribuídos (UTFPR-PG / DAINF —
Tecnologia em Análise e Desenvolvimento de Software, Prof. Dr. Richard
Ribeiro). Esta entrega é a **Entrega Parcial 1 (EP-1)**.

## Tema

Garagens comerciais com displays mostrando vagas livres em tempo real por
andar. Os clientes simulam sensores de portão (ou o app de um motorista)
conversando com um servidor central. O desafio distribuído por trás: manter
o estado consistente com vários terminais mexendo na garagem ao mesmo
tempo — a contagem de vagas não pode ficar negativa nem ser lida pela
metade. Esta entrega ainda não mexe com vagas, mas já deixa o padrão
pronto: a criação/remoção de sessão em `SessionService` roda numa região
crítica isolada com `synchronized`, o mesmo molde que a EP-2 vai
reaproveitar pra contagem de vagas.

## O que está pronto nesta entrega

Login, logout e cadastro de usuário funcionando ponta a ponta: cliente e
servidor em JavaFX, comunicação por socket TCP com JSON, usuários e
sessões persistidos em arquivo, e os validadores de `username`/
`password` cobertos por testes.

Fora do escopo (vai pra EP-2): consultar/atualizar/excluir cadastro, CRUD
de vagas/operações e perfil ADM. Pontos marcados no código com
`// TODO EP-2:`.

## Arquitetura

```
sd-garagem/            (pom pai, packaging pom)
├── common/             modelo de domínio + protocolo + validadores
│                       (não depende de server/client nem de JavaFX)
├── server/             socket server, persistência JSON, GUI do servidor
└── client/             GUI do cliente, conector de socket
```

O `common` é a única fonte de verdade do formato das mensagens — garante
que cliente e servidor (e os de outros colegas, mais pra frente) falem a
mesma língua. Além do modelo (`User`, `Session`) e do protocolo
(`LoginRequest`, `RegisterRequest`, `Response`, `StatusCode`...), guarda
os validadores de formato e a folha de estilo (`css/`) das duas GUIs.

O `server` cuida do socket (`GarageServer`, `ClientHandler`,
`RequestDispatcher`), da persistência em JSON com escrita atômica, do
hash de senha (PBKDF2) e da própria janela de status/log. O `client` só
tem o conector de socket e as telas (login, cadastro, principal) com
seus FXML — nada de lógica de negócio no controller, isso fica nos
validadores e serviços de `common`/`server`.

## Protocolo de troca de mensagens

A fonte da verdade é a planilha de protocolo (mantida por Nathan e
Rafael) — hoje só o login está fechado nela; logout e cadastro seguem o
mesmo padrão por analogia (ver suposições mais abaixo).

O framing é simples: um objeto JSON por linha (newline-delimited JSON),
sempre em UTF-8, lido com `BufferedReader.readLine()` e escrito com
`PrintWriter` em auto-flush (`common.transport.MessageIO`).

### Login

```json
// cliente -> servidor
{"method":"login","data":{"username":"admin","password":"Admin@123"}}
```
```json
// sucesso
{"statusCode":200,"message":"Sucesso no Login","data":{"token":"3fa2...uuid"}}
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
{"method":"register","data":{"username":"novo.usuario","password":"SenhaForte9"}}
```
```json
// sucesso
{"statusCode":201,"message":"Usuário criado com sucesso","data":null}
// username ja existe
{"statusCode":409,"message":"Usuario ja cadastrado","data":null}
```

### Logout

```json
// cliente -> servidor
{"method":"logout","data":{"token":"3fa2...uuid"}}
```
```json
// sucesso
{"statusCode":200,"message":"Usuário deslogado com sucesso","data":null}
// token invalido ou ja expirado
{"statusCode":401,"message":"Token invalido ou sessao inexistente","data":null}
```

### Códigos de status

Centralizados em `common.protocol.StatusCode`, com semântica de HTTP:
`200` sucesso, `201` recurso criado (cadastro), `400` requisição
malformada ou fora do formato, `401` credenciais ou token inválidos,
`404` operação não suportada, `409` conflito (username já cadastrado),
`500` erro interno, `503` servidor indisponível (operador encerrou a
sessão ou parou o servidor).

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

Maven multi-módulo comum, não depende de IDE específica. Os apps JavaFX
usam o goal `javafx:run` do `javafx-maven-plugin`, que o botão "Run"
nativo do VS Code não conhece — rode pelo terminal (integrado ou não):

```bash
# instala o common no repositorio local (uma vez, ou sempre que ele mudar)
./mvnw install -pl common -am -DskipTests

cd server && ../mvnw javafx:run   # porta default 20000
cd client && ../mvnw javafx:run   # em outro terminal
```

Rode o servidor primeiro e clique em **Iniciar** na janela dele; só
depois rode o cliente e logue com `admin` / `Admin@123` (senha padrão
criada no primeiro início — o aviso pra trocá-la aparece no log do
servidor). O servidor grava `dados/usuarios.json` e `dados/sessoes.json`
relativos ao diretório de trabalho do processo, por isso os comandos
entram em `server/` antes de rodar.

A porta muda em `server/server.properties` ou no primeiro argumento de
linha de comando. Precisa estar **entre 20000 e 25000** — faixa definida
pelo professor para a disciplina; o servidor recusa (com aviso no log)
qualquer porta fora dela (ver `ServerProperties.MIN_PORT`/`MAX_PORT`).

No VS Code, instale a extensão **Extension Pack for Java** (Microsoft)
para suporte a Maven/autocomplete/debug, e confirme um **JDK 21+**
configurado (`Ctrl+Shift+P` → "Java: Configure Java Runtime").

### Atalhos: `iniciar.cmd` e `cliente.cmd`

`iniciar.cmd`, na raiz do repositório, sobe o servidor (em segundo
plano) e abre o cliente ao mesmo tempo, numa única janela de console.

`cliente.cmd` abre só uma instância nova do cliente — útil pra testar
vários clientes ao mesmo tempo contra o mesmo servidor já rodando; cada
clique abre um cliente a mais.

## Suposições a validar com a turma

O protocolo ainda está sendo fechado por Nathan e Rafael; onde a
planilha não define algo, tomamos uma decisão e documentamos aqui em vez
de travar o desenvolvimento.

**Protocolo:**
- `logout` e `register` seguem o mesmo padrão estrutural do login
  (`method` na requisição, `statusCode`/`message`/`data` na resposta).
- `statusCode` usa semântica HTTP, centralizado numa única classe.
- Minúsculas obrigatórias valem só pro valor de `method` — `message` é
  texto pra leitura humana, começa com maiúscula.

**Login e segurança:**
- Um usuário não acumula sessões: logar de novo derruba a sessão
  anterior dele.
- O servidor distingue "usuário não encontrado" de "senha incorreta" no
  campo `message`, mas o cliente nunca repassa essa diferença — qualquer
  login que falhe vira a mesma notificação genérica, pra não dar pista
  de qual campo errou. Pelo mesmo motivo, o login não valida em tempo
  real enquanto o usuário digita (diferente do cadastro, onde faz
  sentido: é uma senha nova sendo criada).
- Cadastro não loga automaticamente — volta pra tela de login com o
  username preenchido. Todo usuário criado nasce com papel `CLIENTE`; o
  único ADM é o `admin` da seed inicial.

**Servidor:**
- Último acesso de uma sessão atualiza em memória a cada uso do token,
  só grava em disco quando a sessão é criada ou encerrada.
- Pool de threads fixo em 50 conexões simultâneas (`GarageServer`).
- `dados/` e `server.properties` ficam relativos ao diretório de
  trabalho, não empacotados como recurso — dá pra editar sem recompilar.

## Próximas entregas

**EP-2** (marcado no código com `// TODO EP-2:`): os demais `methods` do
protocolo (CRUD de cadastro, vagas/operações, admin) assim que a
planilha fechar; painel de vagas disponíveis por andar na tela do
cliente; região crítica da contagem de vagas, seguindo o mesmo
isolamento já usado para sessão.

**EP-3**: testes em rede com clientes e servidores dos outros alunos, e
a funcionalidade completa do perfil ADM.
