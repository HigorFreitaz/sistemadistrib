package br.edu.utfpr.sd.garagem.server.smoke;

import br.edu.utfpr.sd.garagem.common.json.JsonSupport;
import br.edu.utfpr.sd.garagem.common.protocol.Response;
import br.edu.utfpr.sd.garagem.common.protocol.StatusCode;
import br.edu.utfpr.sd.garagem.common.protocol.TokenData;
import br.edu.utfpr.sd.garagem.common.transport.MessageIO;
import br.edu.utfpr.sd.garagem.server.net.ConnectedClientRegistry;
import br.edu.utfpr.sd.garagem.server.net.GarageServer;
import br.edu.utfpr.sd.garagem.server.net.RequestDispatcher;
import br.edu.utfpr.sd.garagem.server.net.ServerEventListener;
import br.edu.utfpr.sd.garagem.server.repository.JsonSessionRepository;
import br.edu.utfpr.sd.garagem.server.repository.JsonUserRepository;
import br.edu.utfpr.sd.garagem.server.service.AuthService;
import br.edu.utfpr.sd.garagem.server.service.SessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de ponta a ponta contra um {@link GarageServer} real, falando o
 * protocolo cru em linhas JSON (ver {@link TestClient}). Cada teste ganha
 * seu próprio servidor, numa porta efêmera, criado/derrubado em
 * {@link #startServer()}/{@link #stopServer()}.
 */
class EndToEndSmokeTest {

    private static final int EPHEMERAL_PORT = 0;
    private static final int CONNECT_TIMEOUT_MS = 3000;

    private ConnectedClientRegistry registry;
    private GarageServer server;
    private int port;

    private static class NoOpListener implements ServerEventListener {
        @Override public void onStarted(int port) { }
        @Override public void onStopped() { }
        @Override public void onClientCountChanged(int connectedClients) { }
        @Override public void onSessionCountChanged(int activeSessions) { }
        @Override public void onLog(String message) { }
    }

    @BeforeEach
    void startServer() throws IOException {
        Path dir = Files.createTempDirectory("sdgaragem-smoke");
        JsonUserRepository users = new JsonUserRepository(dir.resolve("usuarios.json"));
        SessionService sessions = new SessionService(new JsonSessionRepository(dir.resolve("sessoes.json")));
        AuthService auth = new AuthService(users, sessions);
        RequestDispatcher dispatcher = new RequestDispatcher(auth, new NoOpListener());
        registry = new ConnectedClientRegistry();
        server = new GarageServer(EPHEMERAL_PORT, dispatcher, new NoOpListener(), registry);
        server.start();
        port = server.getPort();
    }

    /** Idempotente (ver {@link GarageServer#stop()}) — inofensivo mesmo quando o teste já parou o servidor sozinho. */
    @AfterEach
    void stopServer() {
        server.stop();
    }

    @Test
    void fluxoCompletoDeLoginELogout() throws Exception {
        String token = doLogin("admin", "Admin@123", StatusCode.OK);
        assertNotNull(token);
        assertTrue(!token.isBlank());

        doLogin("admin", "senhaErrada9", StatusCode.UNAUTHORIZED);
        doLogin("usuarioinexistente", "qualquerSenha123", StatusCode.UNAUTHORIZED);

        doLogout(token, StatusCode.OK);
        doLogout(token, StatusCode.UNAUTHORIZED);
    }

    @Test
    void cadastraNovoUsuarioELoga() throws Exception {
        doRegister("novo.usuario", "SenhaForte9", StatusCode.OK);
        doRegister("novo.usuario", "OutraSenha9", StatusCode.CONFLICT);

        String token = doLogin("novo.usuario", "SenhaForte9", StatusCode.OK);
        assertNotNull(token);

        doLogout(token, StatusCode.OK);
    }

    /**
     * Com dois clientes conectados (não só um) porque foi exatamente esse o
     * bug relatado em produção: o broadcast entregava pro primeiro cliente
     * e esquecia do resto.
     */
    @Test
    void logoutAllAvisaClientesConectados() throws Exception {
        doRegister("outro.usuario", "SenhaForte9", StatusCode.OK);

        try (TestClient admin = new TestClient(port); TestClient outro = new TestClient(port)) {
            admin.login("admin", "Admin@123", StatusCode.OK);
            outro.login("outro.usuario", "SenhaForte9", StatusCode.OK);

            // sem os clientes pedirem nada, o operador encerra todas as sessoes
            registry.broadcast(Response.error(StatusCode.SERVICE_UNAVAILABLE, "Servidor em manutencao"));

            for (TestClient client : List.of(admin, outro)) {
                Response pushed = client.nextResponse();
                assertEquals(StatusCode.SERVICE_UNAVAILABLE, pushed.getStatusCode(), "cliente nao recebeu o aviso");
                assertEquals("Servidor em manutencao", pushed.getMessage());
            }
        }
    }

    /** Cobre as duas consequências de parar o servidor: quem já estava logado é avisado/desconectado, e a porta para de aceitar conexão nova. */
    @Test
    void pararServidorDesconectaCliente() throws Exception {
        try (TestClient client = new TestClient(port)) {
            client.login("admin", "Admin@123", StatusCode.OK);

            server.stop(); // o operador clica em "Parar" com o cliente ainda logado

            assertEquals(StatusCode.SERVICE_UNAVAILABLE, client.nextResponse().getStatusCode());
            assertTrue(client.isClosedByPeer(), "servidor nao fechou a conexao do cliente ao parar");
        }

        Thread.sleep(200);
        assertThrows(ConnectException.class, () -> {
            try (Socket probe = new Socket()) {
                probe.connect(new InetSocketAddress("localhost", port), 1000);
            }
        });
    }

    private String doLogin(String username, String password, int expectedStatus) throws IOException {
        try (TestClient client = new TestClient(port)) {
            return client.login(username, password, expectedStatus);
        }
    }

    private void doLogout(String token, int expectedStatus) throws IOException {
        try (TestClient client = new TestClient(port)) {
            client.send("{\"method\":\"logout\",\"data\":{\"token\":\"" + token + "\"}}", expectedStatus);
        }
    }

    private void doRegister(String username, String password, int expectedStatus) throws IOException {
        try (TestClient client = new TestClient(port)) {
            client.send("{\"method\":\"register\",\"data\":{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}}",
                    expectedStatus);
        }
    }

    /** Uma conexão crua de teste: abre o socket, manda uma linha JSON, lê a resposta. */
    private static final class TestClient implements AutoCloseable {
        private final Socket socket;
        private final BufferedReader reader;
        private final PrintWriter writer;

        TestClient(int port) throws IOException {
            socket = new Socket();
            socket.connect(new InetSocketAddress("localhost", port), CONNECT_TIMEOUT_MS);
            reader = MessageIO.newReader(socket.getInputStream());
            writer = MessageIO.newWriter(socket.getOutputStream());
        }

        /** Loga e devolve o token (só presente quando {@code expectedStatus} é {@link StatusCode#OK}). */
        String login(String username, String password, int expectedStatus) throws IOException {
            Response response = send("{\"method\":\"login\",\"data\":{\"username\":\"" + username
                    + "\",\"password\":\"" + password + "\"}}", expectedStatus);
            return response.getStatusCode() == StatusCode.OK
                    ? JsonSupport.GSON.fromJson(response.getData(), TokenData.class).getToken()
                    : null;
        }

        Response send(String rawJson, int expectedStatus) throws IOException {
            writer.println(rawJson);
            Response response = nextResponse();
            assertEquals(expectedStatus, response.getStatusCode(), "resposta: " + response.getMessage());
            return response;
        }

        Response nextResponse() throws IOException {
            return JsonSupport.GSON.fromJson(reader.readLine(), Response.class);
        }

        /** {@code true} se o servidor já fechou a conexão (a próxima leitura bate em EOF). */
        boolean isClosedByPeer() throws IOException {
            socket.setSoTimeout(CONNECT_TIMEOUT_MS);
            return socket.getInputStream().read() == -1;
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
