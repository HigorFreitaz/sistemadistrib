package br.edu.utfpr.sd.garagem.client.ui;

import br.edu.utfpr.sd.garagem.client.net.ConnectionException;
import br.edu.utfpr.sd.garagem.client.net.SocketConnector;
import br.edu.utfpr.sd.garagem.common.protocol.RegisterRequest;
import br.edu.utfpr.sd.garagem.common.protocol.Response;
import br.edu.utfpr.sd.garagem.common.protocol.StatusCode;
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
 * Controller da tela de cadastro de usuário (fluxo descrito em
 * {@code docs/Requisitos Funcionais e não funcionais.docx}). Valida o
 * formato dos campos em tempo real e envia o cadastro em uma thread de
 * segundo plano, usando uma conexão própria (fechada logo em seguida —
 * cadastro não abre sessão, o usuário precisa logar depois).
 */
public final class RegisterController {

    private static final Logger LOGGER = Logger.getLogger(RegisterController.class.getName());

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label usernameErrorLabel;
    @FXML
    private Label passwordErrorLabel;
    @FXML
    private Label confirmPasswordErrorLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Button registerButton;
    @FXML
    private ProgressIndicator progressIndicator;

    private ClienteApp app;
    private String host;
    private int port;
    private boolean loading;

    /** Recebe host/porta já preenchidos na tela de login, para não pedir de novo. */
    public void init(ClienteApp app, String host, int port) {
        this.app = app;
        this.host = host;
        this.port = port;
    }

    @FXML
    private void initialize() {
        usernameErrorLabel.setText("usuario invalido: 3-20 letras minusculas, numeros, '.' ou '_'");
        passwordErrorLabel.setText("senha invalida: 8-20 caracteres, sem simbolo nao permitido");
        confirmPasswordErrorLabel.setText("as senhas nao conferem");
        usernameField.textProperty().addListener((obs, old, value) -> validateUsername());
        passwordField.textProperty().addListener((obs, old, value) -> {
            validatePassword();
            validateConfirmPassword();
        });
        confirmPasswordField.textProperty().addListener((obs, old, value) -> validateConfirmPassword());
        validateUsername();
        validatePassword();
        validateConfirmPassword();
    }

    private void validateUsername() {
        boolean valid = UsernameValidator.isValid(usernameField.getText());
        usernameErrorLabel.setVisible(!usernameField.getText().isEmpty() && !valid);
        updateRegisterButtonState();
    }

    private void validatePassword() {
        boolean valid = PasswordValidator.isValid(passwordField.getText());
        passwordErrorLabel.setVisible(!passwordField.getText().isEmpty() && !valid);
        updateRegisterButtonState();
    }

    private void validateConfirmPassword() {
        boolean matches = confirmPasswordField.getText().equals(passwordField.getText());
        confirmPasswordErrorLabel.setVisible(!confirmPasswordField.getText().isEmpty() && !matches);
        updateRegisterButtonState();
    }

    private void updateRegisterButtonState() {
        boolean valid = UsernameValidator.isValid(usernameField.getText())
                && PasswordValidator.isValid(passwordField.getText())
                && confirmPasswordField.getText().equals(passwordField.getText());
        registerButton.setDisable(!valid || loading);
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        setLoading(true);
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws ConnectionException {
                try (SocketConnector connector = new SocketConnector(host, port)) {
                    connector.connect();
                    return connector.send(new RegisterRequest(username, password));
                }
            }
        };
        task.setOnSucceeded(event -> onRegisterSucceeded(task.getValue(), username));
        task.setOnFailed(event -> onRegisterFailed(task.getException()));
        Thread thread = new Thread(task, "register-task");
        thread.setDaemon(true);
        thread.start();
    }

    private void onRegisterSucceeded(Response response, String username) {
        setLoading(false);
        if (response.getStatusCode() == StatusCode.OK) {
            app.showLogin(username);
        } else {
            statusLabel.setText(response.getMessage());
        }
    }

    private void onRegisterFailed(Throwable throwable) {
        setLoading(false);
        String message = throwable instanceof ConnectionException
                ? throwable.getMessage()
                : "Erro inesperado ao tentar conectar.";
        statusLabel.setText(message);
        LOGGER.log(Level.WARNING, "falha ao efetuar cadastro", throwable);
    }

    @FXML
    private void handleBackToLogin() {
        app.showLogin();
    }

    private void setLoading(boolean loading) {
        this.loading = loading;
        progressIndicator.setVisible(loading);
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
        confirmPasswordField.setDisable(loading);
        statusLabel.setText(loading ? "Enviando..." : "");
        updateRegisterButtonState();
    }
}
