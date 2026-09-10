package br.edu.utfpr.sd.garagem.server.ui;

import br.edu.utfpr.sd.garagem.common.protocol.Response;
import br.edu.utfpr.sd.garagem.common.protocol.StatusCode;
import br.edu.utfpr.sd.garagem.server.config.ServerProperties;
import br.edu.utfpr.sd.garagem.server.net.ConnectedClientRegistry;
import br.edu.utfpr.sd.garagem.server.net.GarageServer;
import br.edu.utfpr.sd.garagem.server.net.RequestDispatcher;
import br.edu.utfpr.sd.garagem.server.net.ServerEventListener;
import br.edu.utfpr.sd.garagem.server.repository.JsonSessionRepository;
import br.edu.utfpr.sd.garagem.server.repository.JsonUserRepository;
import br.edu.utfpr.sd.garagem.server.repository.SessionRepository;
import br.edu.utfpr.sd.garagem.server.repository.UserRepository;
import br.edu.utfpr.sd.garagem.server.service.AuthService;
import br.edu.utfpr.sd.garagem.server.service.SessionService;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

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
    private static final Duration TOOLTIP_SHOW_DELAY = Duration.millis(150);
    private static final double TOOLTIP_VERTICAL_GAP = 8;

    @FXML
    private TextField portField;
    @FXML
    private Button startButton;
    @FXML
    private Button stopButton;
    @FXML
    private Button logoutAllButton;
    @FXML
    private Label statusLabel;
    @FXML
    private Label connectedClientsLabel;
    @FXML
    private Label activeSessionsLabel;
    @FXML
    private TextArea logArea;
    @FXML
    private Label statusCodeHelpBadge;
    @FXML
    private Tooltip statusCodeTooltip;

    private GarageServer server;
    private AuthService authService;
    private ConnectedClientRegistry clientRegistry;

    /**
     * Mostra/esconde o tooltip de ajuda na mao, ancorado abaixo do badge, em
     * vez de deixar o JavaFX seguir automaticamente o cursor: o badge é
     * pequeno (16x16) e o popup automático acaba sobrepondo o próprio
     * cursor, o que faz o JavaFX interpretar como "mouse saiu" e entrar em
     * loop de mostra/esconde (bug corrigido nesta versão).
     */
    @FXML
    private void initialize() {
        PauseTransition showDelay = new PauseTransition(TOOLTIP_SHOW_DELAY);
        showDelay.setOnFinished(event -> showStatusCodeTooltip());
        statusCodeHelpBadge.setOnMouseEntered(event -> showDelay.playFromStart());
        statusCodeHelpBadge.setOnMouseExited(event -> {
            showDelay.stop();
            statusCodeTooltip.hide();
        });
    }

    private void showStatusCodeTooltip() {
        Bounds bounds = statusCodeHelpBadge.localToScreen(statusCodeHelpBadge.getBoundsInLocal());
        statusCodeTooltip.show(statusCodeHelpBadge, bounds.getMinX(), bounds.getMaxY() + TOOLTIP_VERTICAL_GAP);
    }

    /** Preenche o estado inicial da tela a partir da configuração carregada. */
    public void init(ServerProperties properties) {
        portField.setText(String.valueOf(properties.getPort()));
        stopButton.setDisable(true);
        logoutAllButton.setDisable(true);
    }

    @FXML
    private void handleStart() {
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            appendLog("Porta invalida: " + portField.getText());
            return;
        }
        if (!ServerProperties.isValidPort(port)) {
            appendLog("Porta fora da faixa permitida (" + ServerProperties.MIN_PORT + "-"
                    + ServerProperties.MAX_PORT + "): " + port);
            return;
        }
        UserRepository userRepository = new JsonUserRepository(USERS_FILE);
        SessionRepository sessionRepository = new JsonSessionRepository(SESSIONS_FILE);
        authService = new AuthService(userRepository, new SessionService(sessionRepository));
        clientRegistry = new ConnectedClientRegistry();
        RequestDispatcher dispatcher = new RequestDispatcher(authService, this);
        server = new GarageServer(port, dispatcher, this, clientRegistry);
        try {
            server.start();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "falha ao iniciar o servidor", e);
            appendLog("Falha ao iniciar o servidor: " + e.getMessage());
        }
    }

    @FXML
    private void handleStop() {
        if (server != null) {
            server.stop();
        }
    }

    @FXML
    private void handleLogoutAll() {
        if (authService == null) {
            return;
        }
        authService.logoutAllSessions();
        clientRegistry.pushToAllAndForget(Response.error(StatusCode.SERVICE_UNAVAILABLE,
                "Servidor em manutencao. Sua sessao foi encerrada pelo administrador."));
        onSessionCountChanged(authService.activeSessionCount());
        appendLog("Todas as sessoes foram encerradas pelo operador do servidor.");
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
            setStatusPill("Escutando na porta " + port, true);
            startButton.setDisable(true);
            stopButton.setDisable(false);
            logoutAllButton.setDisable(false);
            portField.setDisable(true);
            appendLog("Servidor iniciado na porta " + port);
        });
    }

    @Override
    public void onStopped() {
        Platform.runLater(() -> {
            setStatusPill("Parado", false);
            startButton.setDisable(false);
            stopButton.setDisable(true);
            logoutAllButton.setDisable(true);
            portField.setDisable(false);
            connectedClientsLabel.setText("0");
            appendLog("Servidor parado");
        });
    }

    private void setStatusPill(String text, boolean online) {
        statusLabel.setText(text);
        statusLabel.getStyleClass().setAll(online ? "status-pill-online" : "status-pill-offline");
    }

    @Override
    public void onClientCountChanged(int connectedClients) {
        Platform.runLater(() -> connectedClientsLabel.setText(String.valueOf(connectedClients)));
    }

    @Override
    public void onSessionCountChanged(int activeSessions) {
        Platform.runLater(() -> activeSessionsLabel.setText(String.valueOf(activeSessions)));
    }

    @Override
    public void onLog(String message) {
        Platform.runLater(() -> appendLog(message));
    }

    private void appendLog(String message) {
        logArea.appendText(message + System.lineSeparator());
    }
}
