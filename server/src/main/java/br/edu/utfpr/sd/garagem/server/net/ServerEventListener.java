package br.edu.utfpr.sd.garagem.server.net;

/**
 * Notificações de eventos do servidor para quem exibe a GUI. As
 * implementações devem atualizar a interface via {@code Platform.runLater},
 * já que estas chamadas partem de threads de rede, nunca da JavaFX
 * Application Thread.
 */
public interface ServerEventListener {

    /** O servidor começou a escutar na porta informada. */
    void onStarted(int port);

    /** O servidor foi parado. */
    void onStopped();

    /** O número de clientes conectados mudou. */
    void onClientCountChanged(int connectedClients);

    /** O número de sessões ativas mudou. */
    void onSessionCountChanged(int activeSessions);

    /** Uma linha de log deve ser exibida (mensagens do protocolo ou eventos de conexão). */
    void onLog(String message);
}
