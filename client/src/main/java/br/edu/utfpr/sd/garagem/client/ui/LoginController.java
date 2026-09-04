package br.edu.utfpr.sd.garagem.client.ui;

import br.edu.utfpr.sd.garagem.client.net.ConnectionException;
import br.edu.utfpr.sd.garagem.client.net.SocketConnector;
import br.edu.utfpr.sd.garagem.common.json.JsonSupport;
import br.edu.utfpr.sd.garagem.common.protocol.LoginRequest;
import br.edu.utfpr.sd.garagem.common.protocol.Response;
import br.edu.utfpr.sd.garagem.common.protocol.StatusCode;
import br.edu.utfpr.sd.garagem.common.protocol.TokenData;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller da tela de login: delega a autenticação ao
 * {@link SocketConnector} em uma thread de segundo plano, para não travar
 * a interface. Não julga o formato de usuário/senha enquanto o usuário
 * digita — ao contrário do cadastro, aqui a senha já existe, e reagir em
 * tempo real a cada tecla só vazaria informação sobre a política de senha
 * sem necessidade. Qualquer falha (credenciais erradas, rede fora do ar)
 * aparece como uma notificação avulsa, nunca como texto fixo empurrando o
 * formulário.
 */
public final class LoginController {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());
    private static final String INVALID_CREDENTIALS_MESSAGE = "Usuário e/ou senha incorretos.";

    @FXML
    private TextField hostField;
    @FXML
    private TextField portField;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label statusLabel;
    @FXML
    private Button loginButton;
    @FXML
    private ProgressIndicator progressIndicator;

    private ClienteApp app;
    private boolean loading;

    private record LoginOutcome(SocketConnector connector, Response response) {
    }

    /** Recebe a referência à aplicação, para poder trocar de tela após o login. */
    public void init(ClienteApp app) {
        this.app = app;
    }

    /** Preenche o campo de usuário (ex.: username recém-cadastrado). */
    public void setUsername(String username) {
        usernameField.setText(username);
    }

    @FXML
    private void initialize() {
        hostField.setText("localhost");
        portField.setText("5555");
        usernameField.textProperty().addListener((obs, old, value) -> updateLoginButtonState());
        passwordField.textProperty().addListener((obs, old, value) -> updateLoginButtonState());
        updateLoginButtonState();
    }

    /** Só exige que os campos não estejam vazios — o formato é assunto do servidor. */
    private void updateLoginButtonState() {
        boolean filled = !usernameField.getText().isBlank() && !passwordField.getText().isBlank();
        loginButton.setDisable(!filled || loading);
    }

    @FXML
    private void handleLogin() {
        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            showErrorNotification("Porta inválida.");
            return;
        }
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        setLoading(true);
        Task<LoginOutcome> task = new Task<>() {
            @Override
            protected LoginOutcome call() throws ConnectionException {
                SocketConnector connector = new SocketConnector(host, port);
                connector.connect();
                Response response = connector.send(new LoginRequest(username, password));
                return new LoginOutcome(connector, response);
            }
        };
        task.setOnSucceeded(event -> onLoginSucceeded(task.getValue(), username));
        task.setOnFailed(event -> onLoginFailed(task.getException()));
        Thread thread = new Thread(task, "login-task");
        thread.setDaemon(true);
        thread.start();
    }

    private void onLoginSucceeded(LoginOutcome outcome, String username) {
        setLoading(false);
        Response response = outcome.response();
        if (response.getStatusCode() == StatusCode.OK) {
            TokenData tokenData = JsonSupport.GSON.fromJson(response.getData(), TokenData.class);
            app.showMain(username, tokenData.getToken(), outcome.connector());
        } else {
            // mensagem sempre genérica, independente do motivo devolvido pelo
            // servidor — não revelar se foi o usuario ou a senha que falhou.
            showErrorNotification(INVALID_CREDENTIALS_MESSAGE);
            outcome.connector().close();
        }
    }

    private void onLoginFailed(Throwable throwable) {
        setLoading(false);
        String message = throwable instanceof ConnectionException
                ? throwable.getMessage()
                : "Erro inesperado ao tentar conectar.";
        showErrorNotification(message);
        LOGGER.log(Level.WARNING, "falha ao efetuar login", throwable);
    }

    @FXML
    private void handleGoToRegister() {
        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            showErrorNotification("Porta inválida.");
            return;
        }
        app.showRegister(host, port);
    }

    private void setLoading(boolean loading) {
        this.loading = loading;
        progressIndicator.setVisible(loading);
        progressIndicator.setManaged(loading);
        hostField.setDisable(loading);
        portField.setDisable(loading);
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
        statusLabel.setText(loading ? "Conectando..." : "");
        updateLoginButtonState();
    }

    /** Mostra o erro em uma notificação separada, sem alterar o layout do formulário. */
    private void showErrorNotification(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
