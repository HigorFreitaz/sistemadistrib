package br.edu.utfpr.sd.garagem.client.ui;

import br.edu.utfpr.sd.garagem.client.net.ConnectionException;
import br.edu.utfpr.sd.garagem.client.net.SocketConnector;
import br.edu.utfpr.sd.garagem.common.json.JsonSupport;
import br.edu.utfpr.sd.garagem.common.protocol.GetUserRequest;
import br.edu.utfpr.sd.garagem.common.protocol.LogoutRequest;
import br.edu.utfpr.sd.garagem.common.protocol.Response;
import br.edu.utfpr.sd.garagem.common.protocol.StatusCode;
import br.edu.utfpr.sd.garagem.common.protocol.UserData;
import com.google.gson.JsonSyntaxException;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller da tela principal pós-login: mostra o usuário logado e dá
 * acesso à tela de perfil. O restante da tela é espaço reservado para o
 * painel de vagas da EP-2.
 */
public final class MainController {

    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

    @FXML
    private Label usernameLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Button logoutButton;
    @FXML
    private Button profileButton;

    private ClienteApp app;
    private String host;
    private int port;
    private String username;
    private String token;
    private boolean loading;

    /** Preenche a tela com os dados da sessão aberta no login e busca o nome de exibição. */
    public void init(ClienteApp app, String host, int port, String username, String token) {
        this.app = app;
        this.host = host;
        this.port = port;
        this.username = username;
        this.token = token;
        usernameLabel.setText("Bem-vindo, " + username);
        loadDisplayName();
    }

    private void loadDisplayName() {
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws ConnectionException {
                try (SocketConnector connector = new SocketConnector(host, port)) {
                    connector.connect();
                    return connector.send(new GetUserRequest(token, username));
                }
            }
        };
        task.setOnSucceeded(event -> {
            Response response = task.getValue();
            if (response.getStatusCode() == StatusCode.OK) {
                try {
                    UserData data = JsonSupport.GSON.fromJson(response.getData(), UserData.class);
                    if (data.getName() != null && !data.getName().isBlank()) {
                        usernameLabel.setText("Bem-vindo, " + data.getName());
                    }
                } catch (JsonSyntaxException e) {
                    LOGGER.log(Level.WARNING, "resposta de getuser em formato inesperado", e);
                }
            } else if (response.getStatusCode() == StatusCode.UNAUTHORIZED) {
                app.showLogin();
            }
        });
        task.setOnFailed(event -> LOGGER.log(Level.WARNING, "falha ao carregar nome de exibicao", task.getException()));
        TaskRunner.runInBackground(task, "main-load-name-task");
    }

    @FXML
    private void handleOpenProfile() {
        app.showProfile(host, port, username, token);
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Tem certeza que deseja sair?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.YES) {
            return;
        }
        setLoading(true);
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws ConnectionException {
                try (SocketConnector connector = new SocketConnector(host, port)) {
                    connector.connect();
                    return connector.send(new LogoutRequest(token));
                }
            }
        };
        task.setOnSucceeded(event -> onLogoutFinished());
        task.setOnFailed(event -> {
            LOGGER.log(Level.WARNING, "falha ao efetuar logout", task.getException());
            onLogoutFinished();
        });
        TaskRunner.runInBackground(task, "logout-task");
    }

    /** Volta para o login independente do resultado — o token, se ainda válido, só ficaria sem uso. */
    private void onLogoutFinished() {
        setLoading(false);
        app.showLogin();
    }

    private void setLoading(boolean loading) {
        this.loading = loading;
        logoutButton.setDisable(loading);
        profileButton.setDisable(loading);
        statusLabel.setText(loading ? "Saindo..." : "");
    }
}
