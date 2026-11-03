package br.edu.utfpr.sd.garagem.client.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

import java.io.IOException;

/**
 * Ponto de entrada JavaFX do cliente: alterna entre as telas de login,
 * cadastro e principal na mesma janela, sempre no mesmo tamanho — trocar
 * de tela não deve fazer a janela "pular" de tamanho.
 */
public final class ClienteApp extends Application {

    private static final double LOGIN_WIDTH  = 480;
    private static final double LOGIN_HEIGHT = 560;
    private static final double MAIN_WIDTH   = 960;
    private static final double MAIN_HEIGHT  = 660;

    public static void main(String[] args) {
        launch(args);
    }

    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("SD Garagem - Cliente");
        primaryStage.setMinWidth(LOGIN_WIDTH);
        primaryStage.setMinHeight(LOGIN_HEIGHT);
        showLogin();
        primaryStage.show();
    }

    /** Exibe a tela de login. */
    void showLogin() {
        showLogin(null);
    }

    /** Exibe a tela de login, com o username já preenchido (ex.: após cadastro). */
    void showLogin(String prefillUsername) {
        showLogin(prefillUsername, null, 0);
    }

    /** Exibe a tela de login, preservando o host/porta usados na tela anterior. */
    void showLogin(String prefillUsername, String host, int port) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login-view.fxml"));
            Parent root = loader.load();
            LoginController controller = loader.getController();
            controller.init(this);
            if (prefillUsername != null) {
                controller.setUsername(prefillUsername);
            }
            if (host != null) {
                controller.setHost(host, port);
            }
            setScene(root);
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
            setScene(root);
        } catch (IOException e) {
            throw new IllegalStateException("nao foi possivel carregar a tela de cadastro", e);
        }
    }

    /** Exibe a tela principal pós-login, com a sessão já aberta. */
    void showMain(String host, int port, String username, String token) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-view.fxml"));
            Parent root = loader.load();
            MainController controller = loader.getController();
            controller.init(this, host, port, username, token);
            setScene(root, MAIN_WIDTH, MAIN_HEIGHT);
            primaryStage.setMinWidth(600);
            primaryStage.setMinHeight(480);
        } catch (IOException e) {
            throw new IllegalStateException("nao foi possivel carregar a tela principal", e);
        }
    }

    /** Exibe a tela de perfil, reaproveitando host/porta e a sessão já aberta. */
    void showProfile(String host, int port, String username, String token) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/profile-view.fxml"));
            Parent root = loader.load();
            ProfileController controller = loader.getController();
            controller.init(this, host, port, username, token);
            setScene(root);
        } catch (IOException e) {
            throw new IllegalStateException("nao foi possivel carregar a tela de perfil", e);
        }
    }

    private void setScene(Parent root) {
        setScene(root, LOGIN_WIDTH, LOGIN_HEIGHT);
        primaryStage.setMinWidth(LOGIN_WIDTH);
        primaryStage.setMinHeight(LOGIN_HEIGHT);
    }

    private void setScene(Parent root, double w, double h) {
        Scene scene = new Scene(root, w, h);
        URL cssUrl = getClass().getResource("/css/app.css");
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
        primaryStage.setScene(scene);
    }
}
