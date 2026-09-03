package br.edu.utfpr.sd.garagem.client.ui;

import br.edu.utfpr.sd.garagem.client.net.ConnectionException;
import br.edu.utfpr.sd.garagem.client.net.SocketConnector;
import br.edu.utfpr.sd.garagem.common.json.JsonSupport;
import br.edu.utfpr.sd.garagem.common.protocol.LoginRequest;
import br.edu.utfpr.sd.garagem.common.protocol.Response;
import br.edu.utfpr.sd.garagem.common.protocol.StatusCode;
import br.edu.utfpr.sd.garagem.common.protocol.TokenData;
import br.edu.utfpr.sd.garagem.common.validation.PasswordValidator;
import br.edu.utfpr.sd.garagem.common.validation.UsernameValidator;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller da tela de login: valida o formato dos campos em tempo real
 * (feedback imediato) e delega a autenticação ao {@link SocketConnector},
 * sempre em uma thread de segundo plano para não travar a interface.
 */
public final class LoginController {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    @FXML
    private TextField hostField;
    @FXML
    private TextField portField;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label usernameErrorLabel;
    @FXML
    private Label passwordErrorLabel;
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

    @FXML
    private void initialize() {
        hostField.setText("localhost");
        portField.setText("5555");
        usernameErrorLabel.setText("usuario invalido: 3-20 letras minusculas, numeros, '.' ou '_'");
        passwordErrorLabel.setText("senha invalida: contem caractere nao permitido");
        usernameField.textProperty().addListener((obs, old, value) -> validateUsername());
        passwordField.textProperty().addListener((obs, old, value) -> validatePassword());
        validateUsername();
        validatePassword();
    }

    private void validateUsername() {
        boolean valid = UsernameValidator.isValid(usernameField.getText());
        usernameErrorLabel.setVisible(!usernameField.getText().isEmpty() && !valid);
        updateLoginButtonState();
    }

    private void validatePassword() {
        boolean valid = PasswordValidator.isValid(passwordField.getText());
        passwordErrorLabel.setVisible(!passwordField.getText().isEmpty() && !valid);
        updateLoginButtonState();
    }

    private void updateLoginButtonState() {
        boolean valid = UsernameValidator.isValid(usernameField.getText())
                && PasswordValidator.isValid(passwordField.getText());
        loginButton.setDisable(!valid || loading);
    }

    @FXML
    private void handleLogin() {
        String host = hostField.getText().trim();
        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            statusLabel.setText("porta invalida");
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
            statusLabel.setText(response.getMessage());
            outcome.connector().close();
        }
    }

    private void onLoginFailed(Throwable throwable) {
        setLoading(false);
        String message = throwable instanceof ConnectionException
                ? throwable.getMessage()
                : "Erro inesperado ao tentar conectar.";
        statusLabel.setText(message);
        LOGGER.log(Level.WARNING, "falha ao efetuar login", throwable);
    }

    private void setLoading(boolean loading) {
        this.loading = loading;
        progressIndicator.setVisible(loading);
        hostField.setDisable(loading);
        portField.setDisable(loading);
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
        statusLabel.setText(loading ? "Conectando..." : "");
        updateLoginButtonState();
    }
}
