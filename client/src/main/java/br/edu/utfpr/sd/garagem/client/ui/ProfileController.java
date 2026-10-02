package br.edu.utfpr.sd.garagem.client.ui;

import br.edu.utfpr.sd.garagem.client.net.ConnectionException;
import br.edu.utfpr.sd.garagem.client.net.SocketConnector;
import br.edu.utfpr.sd.garagem.common.json.JsonSupport;
import br.edu.utfpr.sd.garagem.common.protocol.DeleteUserRequest;
import br.edu.utfpr.sd.garagem.common.protocol.GetUserRequest;
import br.edu.utfpr.sd.garagem.common.protocol.Response;
import br.edu.utfpr.sd.garagem.common.protocol.StatusCode;
import br.edu.utfpr.sd.garagem.common.protocol.UpdateUserNameRequest;
import br.edu.utfpr.sd.garagem.common.protocol.UpdateUserPasswordRequest;
import br.edu.utfpr.sd.garagem.common.protocol.UserData;
import br.edu.utfpr.sd.garagem.common.validation.NameValidator;
import br.edu.utfpr.sd.garagem.common.validation.PasswordValidator;
import com.google.gson.JsonSyntaxException;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller da tela de perfil: consulta os dados do usuário autenticado
 * ({@code getuser}) e permite editar o nome, trocar a senha ou excluir o
 * cadastro. {@code username} nunca é editável aqui — é a chave primária do
 * usuário. Segue o mesmo padrão de {@link RegisterController}: cada
 * requisição abre sua própria conexão numa thread de segundo plano e o
 * formato dos campos é validado em tempo real com um checklist.
 */
public final class ProfileController {

    private static final Logger LOGGER = Logger.getLogger(ProfileController.class.getName());
    private static final String NAME_LENGTH_HINT = "Entre 1 e 60 caracteres";
    private static final String NAME_CHARSET_HINT = "Somente letras e espacos";
    private static final String PASSWORD_LENGTH_HINT = "Entre 8 e 20 caracteres";
    private static final String PASSWORD_CHARSET_HINT = "Somente letras, numeros e os simbolos # . * & % $ @ ! ( ) - _ = +";
    private static final String CONFIRM_HINT = "As senhas coincidem";

    @FXML
    private Label usernameLabel;
    @FXML
    private TextField nameField;
    @FXML
    private Label nameLengthHint;
    @FXML
    private Label nameCharsetHint;
    @FXML
    private Button saveNameButton;
    @FXML
    private Label nameStatusLabel;

    @FXML
    private PasswordField oldPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmNewPasswordField;
    @FXML
    private Label newPasswordLengthHint;
    @FXML
    private Label newPasswordCharsetHint;
    @FXML
    private Label confirmNewPasswordHint;
    @FXML
    private Button changePasswordButton;
    @FXML
    private Label passwordStatusLabel;

    @FXML
    private Button deleteAccountButton;
    @FXML
    private Button backButton;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label statusLabel;

    private ClienteApp app;
    private String host;
    private int port;
    private String username;
    private String token;
    private boolean loading;

    /** Recebe host/porta e a sessão aberta, e já dispara a busca dos dados do usuário. */
    public void init(ClienteApp app, String host, int port, String username, String token) {
        this.app = app;
        this.host = host;
        this.port = port;
        this.username = username;
        this.token = token;
        usernameLabel.setText(username);
        loadUserData();
    }

    @FXML
    private void initialize() {
        for (Label hint : new Label[] {nameLengthHint, nameCharsetHint, newPasswordLengthHint,
                newPasswordCharsetHint, confirmNewPasswordHint}) {
            attachHintDot(hint);
        }
        nameField.textProperty().addListener((obs, old, value) -> validateName());
        newPasswordField.textProperty().addListener((obs, old, value) -> {
            validateNewPassword();
            validateConfirmNewPassword();
        });
        confirmNewPasswordField.textProperty().addListener((obs, old, value) -> validateConfirmNewPassword());
        oldPasswordField.textProperty().addListener((obs, old, value) -> updateChangePasswordButtonState());
        validateName();
        validateNewPassword();
        validateConfirmNewPassword();
    }

    private static void attachHintDot(Label hint) {
        Region dot = new Region();
        dot.getStyleClass().add("hint-dot");
        hint.setGraphic(dot);
        hint.setGraphicTextGap(6);
    }

    private static void setHint(Label hint, String text, boolean satisfied) {
        hint.setText(text);
        hint.getGraphic().getStyleClass().setAll("hint-dot", satisfied ? "hint-dot-valid" : "hint-dot-invalid");
        hint.getStyleClass().setAll(satisfied ? "hint-valid" : "hint-invalid");
    }

    private void validateName() {
        String name = nameField.getText();
        setHint(nameLengthHint, NAME_LENGTH_HINT, NameValidator.hasValidLength(name));
        setHint(nameCharsetHint, NAME_CHARSET_HINT, NameValidator.hasOnlyAllowedCharacters(name));
        updateSaveNameButtonState();
    }

    private void validateNewPassword() {
        String password = newPasswordField.getText();
        setHint(newPasswordLengthHint, PASSWORD_LENGTH_HINT, PasswordValidator.hasValidLength(password));
        setHint(newPasswordCharsetHint, PASSWORD_CHARSET_HINT, PasswordValidator.hasOnlyAllowedCharacters(password));
        updateChangePasswordButtonState();
    }

    private void validateConfirmNewPassword() {
        boolean matches = !confirmNewPasswordField.getText().isEmpty()
                && confirmNewPasswordField.getText().equals(newPasswordField.getText());
        setHint(confirmNewPasswordHint, CONFIRM_HINT, matches);
        updateChangePasswordButtonState();
    }

    private void updateSaveNameButtonState() {
        saveNameButton.setDisable(!NameValidator.isValid(nameField.getText()) || loading);
    }

    private void updateChangePasswordButtonState() {
        boolean valid = !oldPasswordField.getText().isEmpty()
                && PasswordValidator.isValid(newPasswordField.getText())
                && confirmNewPasswordField.getText().equals(newPasswordField.getText());
        changePasswordButton.setDisable(!valid || loading);
    }

    private void loadUserData() {
        setLoading(true);
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws ConnectionException {
                try (SocketConnector connector = new SocketConnector(host, port)) {
                    connector.connect();
                    return connector.send(new GetUserRequest(token, username));
                }
            }
        };
        task.setOnSucceeded(event -> onUserDataLoaded(task.getValue()));
        task.setOnFailed(event -> {
            setLoading(false);
            if (task.getException() instanceof ConnectionException) {
                app.showLogin();
            } else {
                showErrorNotification(friendlyMessage(task.getException(), "Nao foi possivel carregar os dados do perfil."));
            }
        });
        TaskRunner.runInBackground(task, "profile-load-task");
    }

    private void onUserDataLoaded(Response response) {
        setLoading(false);
        if (response.getStatusCode() == StatusCode.UNAUTHORIZED) {
            app.showLogin();
            return;
        }
        if (response.getStatusCode() != StatusCode.OK) {
            showErrorNotification(response.getMessage());
            return;
        }
        try {
            UserData data = JsonSupport.GSON.fromJson(response.getData(), UserData.class);
            nameField.setText(data.getName());
        } catch (JsonSyntaxException e) {
            LOGGER.log(Level.WARNING, "resposta de getuser em formato inesperado", e);
            showErrorNotification("O servidor respondeu num formato inesperado.");
        }
    }

    @FXML
    private void handleSaveName() {
        String name = nameField.getText().trim();
        setLoading(true);
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws ConnectionException {
                try (SocketConnector connector = new SocketConnector(host, port)) {
                    connector.connect();
                    return connector.send(new UpdateUserNameRequest(token, username, name));
                }
            }
        };
        task.setOnSucceeded(event -> {
            setLoading(false);
            Response response = task.getValue();
            if (response.getStatusCode() == StatusCode.UNAUTHORIZED) {
                app.showLogin();
                return;
            }
            nameStatusLabel.setText(response.getMessage());
            if (response.getStatusCode() != StatusCode.OK) {
                showErrorNotification(response.getMessage());
            }
        });
        task.setOnFailed(event -> {
            setLoading(false);
            if (task.getException() instanceof ConnectionException) {
                app.showLogin();
            } else {
                String message = friendlyMessage(task.getException(), "Erro inesperado ao atualizar o nome.");
                nameStatusLabel.setText(message);
                LOGGER.log(Level.WARNING, "falha ao atualizar nome", task.getException());
            }
        });
        TaskRunner.runInBackground(task, "profile-update-name-task");
    }

    @FXML
    private void handleChangePassword() {
        String oldPassword = oldPasswordField.getText();
        String newPassword = newPasswordField.getText();
        setLoading(true);
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws ConnectionException {
                try (SocketConnector connector = new SocketConnector(host, port)) {
                    connector.connect();
                    return connector.send(new UpdateUserPasswordRequest(token, username, oldPassword, newPassword));
                }
            }
        };
        task.setOnSucceeded(event -> {
            setLoading(false);
            Response response = task.getValue();
            if (response.getStatusCode() == StatusCode.UNAUTHORIZED
                    && response.getMessage() != null && response.getMessage().contains("Sess")) {
                app.showLogin();
                return;
            }
            passwordStatusLabel.setText(response.getMessage());
            if (response.getStatusCode() == StatusCode.OK) {
                oldPasswordField.clear();
                newPasswordField.clear();
                confirmNewPasswordField.clear();
            } else {
                showErrorNotification(response.getMessage());
            }
        });
        task.setOnFailed(event -> {
            setLoading(false);
            if (task.getException() instanceof ConnectionException) {
                app.showLogin();
            } else {
                String message = friendlyMessage(task.getException(), "Erro inesperado ao trocar a senha.");
                passwordStatusLabel.setText(message);
                LOGGER.log(Level.WARNING, "falha ao trocar senha", task.getException());
            }
        });
        TaskRunner.runInBackground(task, "profile-change-password-task");
    }

    @FXML
    private void handleDeleteAccount() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Tem certeza que deseja excluir sua conta? Essa acao nao pode ser desfeita.",
                ButtonType.YES, ButtonType.NO);
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
                    return connector.send(new DeleteUserRequest(token, username));
                }
            }
        };
        task.setOnSucceeded(event -> {
            setLoading(false);
            Response response = task.getValue();
            if (response.getStatusCode() == StatusCode.OK) {
                app.showLogin();
            } else {
                showErrorNotification(response.getMessage());
            }
        });
        task.setOnFailed(event -> {
            setLoading(false);
            showErrorNotification(friendlyMessage(task.getException(), "Erro inesperado ao excluir a conta."));
        });
        TaskRunner.runInBackground(task, "profile-delete-task");
    }

    @FXML
    private void handleBack() {
        app.showMain(host, port, username, token);
    }

    private static String friendlyMessage(Throwable throwable, String fallback) {
        return throwable instanceof ConnectionException ? throwable.getMessage() : fallback;
    }

    private void setLoading(boolean loading) {
        this.loading = loading;
        progressIndicator.setVisible(loading);
        progressIndicator.setManaged(loading);
        nameField.setDisable(loading);
        oldPasswordField.setDisable(loading);
        newPasswordField.setDisable(loading);
        confirmNewPasswordField.setDisable(loading);
        deleteAccountButton.setDisable(loading);
        backButton.setDisable(loading);
        statusLabel.setText(loading ? "Processando..." : "");
        updateSaveNameButtonState();
        updateChangePasswordButtonState();
    }

    private void showErrorNotification(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
