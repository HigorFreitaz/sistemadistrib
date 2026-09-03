package br.edu.utfpr.sd.garagem.client.ui;

import br.edu.utfpr.sd.garagem.client.net.SocketConnector;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Ponto de entrada JavaFX do cliente: alterna entre a tela de login e a
 * tela principal na mesma janela.
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
        showLogin();
        primaryStage.show();
    }

    /** Exibe a tela de login. */
    void showLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login-view.fxml"));
            Parent root = loader.load();
            LoginController controller = loader.getController();
            controller.init(this);
            primaryStage.setScene(new Scene(root, 420, 320));
        } catch (IOException e) {
            throw new IllegalStateException("nao foi possivel carregar a tela de login", e);
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
