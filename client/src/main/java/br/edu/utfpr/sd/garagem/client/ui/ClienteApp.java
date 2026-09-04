package br.edu.utfpr.sd.garagem.client.ui;

import br.edu.utfpr.sd.garagem.client.net.SocketConnector;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Ponto de entrada JavaFX do cliente: alterna entre as telas de login,
 * cadastro e principal na mesma janela.
 */
public final class ClienteApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("SD Garagem - Cliente");
        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(380);
        showLogin();
        primaryStage.show();
    }

    /** Exibe a tela de login. */
    void showLogin() {
        showLogin(null);
    }

    /** Exibe a tela de login, com o username já preenchido (ex.: após cadastro). */
    void showLogin(String prefillUsername) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login-view.fxml"));
            Parent root = loader.load();
            LoginController controller = loader.getController();
            controller.init(this);
            if (prefillUsername != null) {
                controller.setUsername(prefillUsername);
            }
            primaryStage.setScene(new Scene(root, 420, 340));
        } catch (IOException e) {
            throw new IllegalStateException("nao foi possivel carregar a tela de login", e);
        }
    }

    /** Exibe a tela de cadastro, reaproveitando host/porta já informados no login. */
    void showRegister(String host, int port) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register-view.fxml"));
            Parent root = loader.load();
            RegisterController controller = loader.getController();
            controller.init(this, host, port);
            primaryStage.setScene(new Scene(root, 440, 420));
        } catch (IOException e) {
            throw new IllegalStateException("nao foi possivel carregar a tela de cadastro", e);
        }
    }

    /** Exibe a tela principal pós-login, com a sessão já aberta. */
    void showMain(String username, String token, SocketConnector connector) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
            Parent root = loader.load();
            MainController controller = loader.getController();
            controller.init(this, username, token, connector);
            primaryStage.setScene(new Scene(root, 480, 360));
        } catch (IOException e) {
            throw new IllegalStateException("nao foi possivel carregar a tela principal", e);
        }
    }
}
