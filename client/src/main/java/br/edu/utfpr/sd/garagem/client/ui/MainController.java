package br.edu.utfpr.sd.garagem.client.ui;

import br.edu.utfpr.sd.garagem.client.net.SocketConnector;
import br.edu.utfpr.sd.garagem.common.protocol.Response;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller da tela principal pós-login: mostra o usuário logado e escuta
 * a conexão em segundo plano para reagir a avisos que o servidor empurre
 * sem o cliente pedir nada (ex.: operador encerrando todas as sessões). O
 * token não é exibido na tela — os documentos de requisitos pedem apenas
 * que o cliente o armazene para uso posterior. O restante da tela é espaço
 * reservado para o painel de vagas da EP-2.
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
    private SocketConnector connector;
    private volatile boolean listening;

    /**
     * Preenche a tela com os dados da sessão aberta no login. O parâmetro
     * {@code token} ainda não é usado aqui — "Sair" só avisa que não foi
     * implementado (ver {@link #handleLogout}) — mas fica na assinatura
     * porque é o que a implementação real de logout vai precisar.
     */
    public void init(ClienteApp app, String username, String token, SocketConnector connector) {
        this.app = app;
        this.connector = connector;
        usernameLabel.setText("Bem-vindo, " + username);
        startPushListener();
    }

    /**
     * Ainda não implementado: sair de verdade exige encerrar a sessão no
     * servidor e voltar para o login, mas isso está sendo revisado junto do
     * fluxo de "encerrar todas as sessões" do operador — por ora só avisa.
     */
    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Funcionalidade ainda nao implementada.", ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /** Escuta a conexão em segundo plano por mensagens fora do ciclo requisição/resposta. */
    private void startPushListener() {
        listening = true;
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                listenForPush();
                return null;
            }
        };
        TaskRunner.runInBackground(task, "server-push-listener");
    }

    private void listenForPush() {
        while (listening) {
            try {
                Response pushed = connector.awaitPush();
                if (listening) {
                    Platform.runLater(() -> handleServerPush(pushed));
                }
                return;
            } catch (SocketTimeoutException e) {
                // ocioso dentro do timeout de leitura do socket -- sem
                // noticia do servidor nesse intervalo nao e erro, so
                // continua esperando.
            } catch (IOException e) {
                if (listening) {
                    LOGGER.log(Level.FINE, "escuta de avisos do servidor encerrada", e);
                }
                return;
            }
        }
    }

    /**
     * Reage a um aviso do servidor (ex.: sessão encerrada pelo operador),
     * voltando ao login. Traz a janela pra frente antes de avisar: com
     * vários clientes abertos (ex.: testando multi-cliente), o Windows não
     * deixa uma janela em segundo plano roubar o foco sozinha, então o
     * aviso de uma delas podia ficar escondido atrás da outra sem o
     * usuário perceber que precisava fechá-lo.
     */
    private void handleServerPush(Response pushed) {
        listening = false;
        Stage window = (Stage) usernameLabel.getScene().getWindow();
        window.setIconified(false);
        window.toFront();
        window.requestFocus();
        Alert alert = new Alert(Alert.AlertType.WARNING, pushed.getMessage(), ButtonType.OK);
        alert.initOwner(window);
        alert.setTitle("Sessão encerrada pelo servidor");
        alert.setHeaderText("Sessão encerrada pelo servidor");
        alert.showAndWait();
        connector.close();
        app.showLogin();
    }
}
