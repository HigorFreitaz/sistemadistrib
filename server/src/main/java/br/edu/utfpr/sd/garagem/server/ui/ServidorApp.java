package br.edu.utfpr.sd.garagem.server.ui;

import br.edu.utfpr.sd.garagem.server.config.ServerProperties;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Ponto de entrada JavaFX do servidor: carrega a tela de status/log e
 * garante que o servidor seja parado ao fechar a janela.
 */
public final class ServidorApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        String[] args = getParameters().getRaw().toArray(new String[0]);
        ServerProperties properties = ServerProperties.load(args);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/server-view.fxml"));
        Parent root = loader.load();
        ServerController controller = loader.getController();
        controller.init(properties);

        Scene scene = new Scene(root, 760, 520);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());

        primaryStage.setTitle("SD Garagem - Servidor");
        primaryStage.setMinWidth(520);
        primaryStage.setMinHeight(420);
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> controller.shutdown());
        primaryStage.show();
    }
}
