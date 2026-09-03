package br.edu.utfpr.sd.garagem.server.ui;

import br.edu.utfpr.sd.garagem.server.config.ServerProperties;
import br.edu.utfpr.sd.garagem.server.net.GarageServer;
import br.edu.utfpr.sd.garagem.server.net.RequestDispatcher;
import br.edu.utfpr.sd.garagem.server.net.ServerEventListener;
import br.edu.utfpr.sd.garagem.server.repository.JsonSessionRepository;
import br.edu.utfpr.sd.garagem.server.repository.JsonUserRepository;
import br.edu.utfpr.sd.garagem.server.repository.SessionRepository;
import br.edu.utfpr.sd.garagem.server.repository.UserRepository;
import br.edu.utfpr.sd.garagem.server.service.AuthService;
import br.edu.utfpr.sd.garagem.server.service.SessionService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller da GUI do servidor: apenas liga os controles a
 * {@link GarageServer}, sem lógica de negócio própria. Toda atualização de
 * tela chega via {@link ServerEventListener}, sempre despachada por
 * {@code Platform.runLater} porque o socket roda fora da JavaFX Application
 * Thread.
 */
public final class ServerController implements ServerEventListener {

    private static final Logger LOGGER = Logger.getLogger(ServerController.class.getName());
    private static final Path USERS_FILE = Path.of("dados", "usuarios.json");
    private static final Path SESSIONS_FILE = Path.of("dados", "sessoes.json");

    @FXML
    private TextField portField;
    @FXML
    private Button startButton;
    @FXML
    private Button stopButton;
    @FXML
    private Label statusLabel;
    @FXML
    private Label connectedClientsLabel;
    @FXML
    private Label activeSessionsLabel;
    @FXML
    private TextArea logArea;

    private GarageServer server;

    /** Preenche o estado inicial da tela a partir da configuração carregada. */
    public void init(ServerProperties properties) {
        portField.setText(String.valueOf(properties.getPort()));
        stopButton.setDisable(true);
    }

    @FXML
    private void handleStart() {
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            appendLog("porta invalida: " + portField.getText());
            return;
        }
        UserRepository userRepository = new JsonUserRepository(USERS_FILE);
        SessionRepository sessionRepository = new JsonSessionRepository(SESSIONS_FILE);
        AuthService authService = new AuthService(userRepository, new SessionService(sessionRepository));
        RequestDispatcher dispatcher = new RequestDispatcher(authService, this);
        server = new GarageServer(port, dispatcher, this);
        try {
            server.start();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "falha ao iniciar o servidor", e);
            appendLog("falha ao iniciar o servidor: " + e.getMessage());
        }
    }

    @FXML
    private void handleStop() {
        if (server != null) {
            server.stop();
        }
    }

    /** Encerra o servidor ao fechar a janela, para não deixar o socket aberto. */
    public void shutdown() {
        if (server != null) {
            server.stop();
        }
    }

    @Override
    public void onStarted(int port) {
        Platform.runLater(() -> {
            statusLabel.setText("Escutando na porta " + port);
            startButton.setDisable(true);
            stopButton.setDisable(false);
            portField.setDisable(true);
            appendLog("servidor iniciado na porta " + port);
        });
    }

    @Override
    public void onStopped() {
        Platform.runLater(() -> {
            statusLabel.setText("Parado");
            startButton.setDisable(false);
            stopButton.setDisable(true);
            portField.setDisable(false);
            connectedClientsLabel.setText("Clientes conectados: 0");
            appendLog("servidor parado");
        });
    }

    @Override
    public void onClientCountChanged(int connectedClients) {
        Platform.runLater(() -> connectedClientsLabel.setText("Clientes conectados: " + connectedClients));
    }

    @Override
    public void onSessionCountChanged(int activeSessions) {
        Platform.runLater(() -> activeSessionsLabel.setText("Sessoes ativas: " + activeSessions));
    }

    @Override
    public void onLog(String message) {
        Platform.runLater(() -> appendLog(message));
    }

    private void appendLog(String message) {
        logArea.appendText(message + System.lineSeparator());
    }
}
