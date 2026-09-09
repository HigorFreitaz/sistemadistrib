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
 * {@code docs/Requisitos Funcionais e não funcionais.docx}). Cada regra de
 * formato é mostrada como um item de checklist que fica vermelho ou verde
 * conforme o usuário digita, em vez de uma única mensagem de erro genérica.
 * Envia o cadastro em uma thread de segundo plano, usando uma conexão
 * própria (fechada logo em seguida — cadastro não abre sessão, o usuário
 * precisa logar depois).
 */
public final class RegisterController {

    private static final Logger LOGGER = Logger.getLogger(RegisterController.class.getName());
    private static final String USERNAME_LENGTH_HINT = "Entre 3 e 20 caracteres";
    private static final String USERNAME_CHARSET_HINT = "Somente letras minusculas, numeros, '.' ou '_'";
    private static final String PASSWORD_LENGTH_HINT = "Entre 8 e 20 caracteres";
    private static final String PASSWORD_CHARSET_HINT = "Somente letras, numeros e os simbolos # . * & % $ @ ! ( ) - _ = +";
    private static final String CONFIRM_HINT = "As senhas coincidem";

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label usernameLengthHint;
    @FXML
    private Label usernameCharsetHint;
    @FXML
    private Label passwordLengthHint;
    @FXML
    private Label passwordCharsetHint;
    @FXML
    private Label confirmPasswordHint;
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
        String username = usernameField.getText();
        setHint(usernameLengthHint, USERNAME_LENGTH_HINT, UsernameValidator.hasValidLength(username));
        setHint(usernameCharsetHint, USERNAME_CHARSET_HINT, UsernameValidator.hasOnlyAllowedCharacters(username));
        updateRegisterButtonState();
    }

    private void validatePassword() {
        String password = passwordField.getText();
        setHint(passwordLengthHint, PASSWORD_LENGTH_HINT, PasswordValidator.hasValidLength(password));
        setHint(passwordCharsetHint, PASSWORD_CHARSET_HINT, PasswordValidator.hasOnlyAllowedCharacters(password));
        updateRegisterButtonState();
    }

    private void validateConfirmPassword() {
        boolean matches = !confirmPasswordField.getText().isEmpty()
                && confirmPasswordField.getText().equals(passwordField.getText());
        setHint(confirmPasswordHint, CONFIRM_HINT, matches);
        updateRegisterButtonState();
    }

    /** Marca um item do checklist como satisfeito (verde, com "✓") ou pendente (vermelho, com "•"). */
    private static void setHint(Label hint, String text, boolean satisfied) {
        hint.setText((satisfied ? "✓ " : "• ") + text);
        hint.getStyleClass().setAll(satisfied ? "hint-valid" : "hint-invalid");
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
        TaskRunner.runInBackground(task, "register-task");
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
        progressIndicator.setManaged(loading);
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
        confirmPasswordField.setDisable(loading);
        statusLabel.setText(loading ? "Enviando..." : "");
        updateRegisterButtonState();
    }
}
