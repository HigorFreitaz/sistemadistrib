package br.edu.utfpr.sd.garagem.client.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;

/**
 * Controller da tela principal pós-login: mostra o usuário logado. O
 * restante da tela é espaço reservado para o painel de vagas da EP-2.
 */
public final class MainController {

    @FXML
    private Label usernameLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Button logoutButton;

    private ClienteApp app;

    /**
     * Preenche a tela com os dados da sessão aberta no login. Nem
     * {@code app} nem {@code token} são usados aqui ainda — "Sair" só
     * avisa que não foi implementado (ver {@link #handleLogout}) — mas
     * ambos ficam na assinatura porque é o que a implementação real de
     * logout vai precisar: {@code app} pra voltar à tela de login, e
     * {@code token} pra montar a própria conexão (cada requisição abre a
     * sua e fecha em seguida — não há conexão aberta da sessão pra
     * reaproveitar).
     */
    public void init(ClienteApp app, String username, String token) {
        this.app = app;
        usernameLabel.setText("Bem-vindo, " + username);
    }

    /**
     * Ainda não implementado: sair de verdade exige encerrar a sessão no
     * servidor e voltar para o login — por ora só avisa.
     */
    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Funcionalidade ainda nao implementada.", ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
