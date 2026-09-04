package br.edu.utfpr.sd.garagem.client.ui;

import br.edu.utfpr.sd.garagem.client.net.ConnectionException;
import br.edu.utfpr.sd.garagem.client.net.SocketConnector;
import br.edu.utfpr.sd.garagem.common.protocol.LogoutRequest;
import br.edu.utfpr.sd.garagem.common.protocol.Response;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller da tela principal pós-login: mostra o usuário logado e trata
 * o logout, sempre em thread de segundo plano. O token não é exibido na
 * tela — os documentos de requisitos pedem apenas que o cliente o
 * armazene para uso posterior (aqui, no logout). O restante da tela é
 * espaço reservado para o painel de vagas da EP-2.
 */
public final class MainController {

    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

    @FXML
    private Label usernameLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Button logoutButton;

    private ClienteApp app;
    private String token;
    private SocketConnector connector;

    /** Preenche a tela com os dados da sessão aberta no login. */
    public void init(ClienteApp app, String username, String token, SocketConnector connector) {
        this.app = app;
        this.token = token;
        this.connector = connector;
        usernameLabel.setText("Bem-vindo, " + username);
    }

    @FXML
    private void handleLogout() {
        logoutButton.setDisable(true);
        statusLabel.setText("Saindo...");
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws ConnectionException {
                return connector.send(new LogoutRequest(token));
            }
        };
        task.setOnSucceeded(event -> finishLogout(null));
        task.setOnFailed(event -> finishLogout(task.getException()));
        Thread thread = new Thread(task, "logout-task");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Encerra a sessão localmente independentemente da resposta do
     * servidor: se houve falha de rede, o usuário já pediu para sair e não
     * há nada mais que o cliente possa fazer além de avisar e voltar ao
     * login.
     */
    private void finishLogout(Throwable failure) {
        connector.close();
        if (failure != null) {
            LOGGER.log(Level.WARNING, "falha ao confirmar logout no servidor", failure);
            Alert alert = new Alert(Alert.AlertType.WARNING,
                    "Nao foi possivel confirmar o logout no servidor (sem conexao). "
                            + "Sessao local encerrada mesmo assim.",
                    ButtonType.OK);
            alert.showAndWait();
        }
        app.showLogin();
    }
}
