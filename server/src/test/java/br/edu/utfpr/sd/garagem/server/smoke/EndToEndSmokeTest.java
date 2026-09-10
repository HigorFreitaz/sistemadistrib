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
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndToEndSmokeTest {

    /** Porta 0 pede ao SO uma porta livre qualquer — evita conflito com algo já ocupando uma porta fixa. */
    private static final int EPHEMERAL_PORT = 0;

    private static class NoOpListener implements ServerEventListener {
        @Override public void onStarted(int port) { }
        @Override public void onStopped() { }
        @Override public void onClientCountChanged(int connectedClients) { }
        @Override public void onSessionCountChanged(int activeSessions) { }
        @Override public void onLog(String message) { }
    }

    @Test
    void fluxoCompletoDeLoginELogout() throws Exception {
        Path dir = Files.createTempDirectory("sdgaragem-smoke");
        JsonUserRepository users = new JsonUserRepository(dir.resolve("usuarios.json"));
        SessionService sessions = new SessionService(new JsonSessionRepository(dir.resolve("sessoes.json")));
        AuthService auth = new AuthService(users, sessions);
        RequestDispatcher dispatcher = new RequestDispatcher(auth, new NoOpListener());
        GarageServer server = new GarageServer(EPHEMERAL_PORT, dispatcher, new NoOpListener(), new ConnectedClientRegistry());
        server.start();
        int port = server.getPort();
        try {
            String token = doLogin(port, "admin", "Admin@123", StatusCode.OK);
            assertNotNull(token);
            assertTrue(!token.isBlank());

            doLogin(port, "admin", "senhaErrada9", StatusCode.UNAUTHORIZED);
            doLogin(port, "usuarioinexistente", "qualquerSenha123", StatusCode.UNAUTHORIZED);

            doLogout(port, token, StatusCode.OK);
            doLogout(port, token, StatusCode.UNAUTHORIZED);
        } finally {
            server.stop();
        }

        Thread.sleep(200);
        assertThrows(ConnectException.class, () -> new Socket().connect(new InetSocketAddress("localhost", port), 1000));
    }

    @Test
    void cadastraNovoUsuarioELoga() throws Exception {
        Path dir = Files.createTempDirectory("sdgaragem-smoke-cadastro");
        JsonUserRepository users = new JsonUserRepository(dir.resolve("usuarios.json"));
        SessionService sessions = new SessionService(new JsonSessionRepository(dir.resolve("sessoes.json")));
        AuthService auth = new AuthService(users, sessions);
        RequestDispatcher dispatcher = new RequestDispatcher(auth, new NoOpListener());
        GarageServer server = new GarageServer(EPHEMERAL_PORT, dispatcher, new NoOpListener(), new ConnectedClientRegistry());
        server.start();
        int port = server.getPort();
        try {
            doRegister(port, "novo.usuario", "SenhaForte9", StatusCode.OK);
            doRegister(port, "novo.usuario", "OutraSenha9", StatusCode.CONFLICT);

            String token = doLogin(port, "novo.usuario", "SenhaForte9", StatusCode.OK);
            assertNotNull(token);

            doLogout(port, token, StatusCode.OK);
        } finally {
            server.stop();
        }
    }

    @Test
    void encerrarTodasAsSessoesEmpurraAvisoParaClienteConectado() throws Exception {
        Path dir = Files.createTempDirectory("sdgaragem-smoke-push");
        JsonUserRepository users = new JsonUserRepository(dir.resolve("usuarios.json"));
        SessionService sessions = new SessionService(new JsonSessionRepository(dir.resolve("sessoes.json")));
        AuthService auth = new AuthService(users, sessions);
        RequestDispatcher dispatcher = new RequestDispatcher(auth, new NoOpListener());
        ConnectedClientRegistry registry = new ConnectedClientRegistry();
        GarageServer server = new GarageServer(EPHEMERAL_PORT, dispatcher, new NoOpListener(), registry);
        server.start();
        int port = server.getPort();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 3000);
            try (BufferedReader reader = MessageIO.newReader(socket.getInputStream());
                 PrintWriter writer = MessageIO.newWriter(socket.getOutputStream())) {
                writer.println("{\"method\":\"login\",\"data\":{\"username\":\"admin\",\"password\":\"Admin@123\"}}");
                Response loginResponse = JsonSupport.GSON.fromJson(reader.readLine(), Response.class);
                assertEquals(StatusCode.OK, loginResponse.getStatusCode());

                // sem o cliente pedir nada, o operador encerra todas as sessoes
                registry.pushToAllAndForget(Response.error(StatusCode.SERVICE_UNAVAILABLE, "Servidor em manutencao"));

                Response pushed = JsonSupport.GSON.fromJson(reader.readLine(), Response.class);
                assertEquals(StatusCode.SERVICE_UNAVAILABLE, pushed.getStatusCode());
                assertEquals("Servidor em manutencao", pushed.getMessage());
            }
        } finally {
            server.stop();
        }
    }

    private String doLogin(int port, String username, String password, int expectedStatus) throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 3000);
            try (BufferedReader reader = MessageIO.newReader(socket.getInputStream());
                 PrintWriter writer = MessageIO.newWriter(socket.getOutputStream())) {
                String json = "{\"method\":\"login\",\"data\":{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}}";
                writer.println(json);
                String line = reader.readLine();
                Response response = JsonSupport.GSON.fromJson(line, Response.class);
                assertEquals(expectedStatus, response.getStatusCode(), "resposta: " + line);
                if (response.getStatusCode() == StatusCode.OK) {
                    return JsonSupport.GSON.fromJson(response.getData(), TokenData.class).getToken();
                }
                return null;
            }
        }
    }

    private void doLogout(int port, String token, int expectedStatus) throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 3000);
            try (BufferedReader reader = MessageIO.newReader(socket.getInputStream());
                 PrintWriter writer = MessageIO.newWriter(socket.getOutputStream())) {
                String json = "{\"method\":\"logout\",\"data\":{\"token\":\"" + token + "\"}}";
                writer.println(json);
                String line = reader.readLine();
                Response response = JsonSupport.GSON.fromJson(line, Response.class);
                assertEquals(expectedStatus, response.getStatusCode(), "resposta: " + line);
            }
        }
    }

    private void doRegister(int port, String username, String password, int expectedStatus) throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 3000);
            try (BufferedReader reader = MessageIO.newReader(socket.getInputStream());
                 PrintWriter writer = MessageIO.newWriter(socket.getOutputStream())) {
                String json = "{\"method\":\"register\",\"data\":{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}}";
                writer.println(json);
                String line = reader.readLine();
                Response response = JsonSupport.GSON.fromJson(line, Response.class);
                assertEquals(expectedStatus, response.getStatusCode(), "resposta: " + line);
            }
        }
    }
}
