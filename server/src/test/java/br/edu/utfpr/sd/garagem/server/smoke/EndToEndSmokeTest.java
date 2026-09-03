package br.edu.utfpr.sd.garagem.server.smoke;

import br.edu.utfpr.sd.garagem.common.json.JsonSupport;
import br.edu.utfpr.sd.garagem.common.protocol.Response;
import br.edu.utfpr.sd.garagem.common.protocol.StatusCode;
import br.edu.utfpr.sd.garagem.common.protocol.TokenData;
import br.edu.utfpr.sd.garagem.common.transport.MessageIO;
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

    private static final int PORT = 18765;

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
        GarageServer server = new GarageServer(PORT, dispatcher, new NoOpListener());
        server.start();
        try {
            String token = doLogin("admin", "Admin@123", StatusCode.OK);
            assertNotNull(token);
            assertTrue(!token.isBlank());

            doLogin("admin", "senhaErrada", StatusCode.UNAUTHORIZED);

            doLogout(token, StatusCode.OK);
            doLogout(token, StatusCode.UNAUTHORIZED);
        } finally {
            server.stop();
        }

        Thread.sleep(200);
        assertThrows(ConnectException.class, () -> new Socket().connect(new InetSocketAddress("localhost", PORT), 1000));
    }

    private String doLogin(String username, String password, int expectedStatus) throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", PORT), 3000);
            try (BufferedReader reader = MessageIO.newReader(socket.getInputStream());
                 PrintWriter writer = MessageIO.newWriter(socket.getOutputStream())) {
                String json = "{\"method\":\"login\",\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
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

    private void doLogout(String token, int expectedStatus) throws Exception {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", PORT), 3000);
            try (BufferedReader reader = MessageIO.newReader(socket.getInputStream());
                 PrintWriter writer = MessageIO.newWriter(socket.getOutputStream())) {
                String json = "{\"method\":\"logout\",\"token\":\"" + token + "\"}";
                writer.println(json);
                String line = reader.readLine();
                Response response = JsonSupport.GSON.fromJson(line, Response.class);
                assertEquals(expectedStatus, response.getStatusCode(), "resposta: " + line);
            }
        }
    }
}
