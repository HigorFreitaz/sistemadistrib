package br.edu.utfpr.sd.garagem.client.ui;

import br.edu.utfpr.sd.garagem.client.net.ConnectionException;
import br.edu.utfpr.sd.garagem.client.net.SocketConnector;
import br.edu.utfpr.sd.garagem.common.protocol.LogoutRequest;
import br.edu.utfpr.sd.garagem.common.protocol.Response;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

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

    /** Preenche a tela com os dados da sessão aberta no login. */
    public void init(ClienteApp app, String host, int port, String username, String token) {
        this.app = app;
        this.host = host;
        this.port = port;
        this.username = username;
        this.token = token;
        usernameLabel.setText("Bem-vindo, " + username);
    }

    @FXML
    private void handleOpenProfile() {
        app.showProfile(host, port, username, token);
    }

    @FXML
    private void handleLogout() {
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
