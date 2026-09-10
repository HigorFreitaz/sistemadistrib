package br.edu.utfpr.sd.garagem.server.net;

import br.edu.utfpr.sd.garagem.common.json.JsonSupport;
import br.edu.utfpr.sd.garagem.common.protocol.Response;
import br.edu.utfpr.sd.garagem.common.transport.MessageIO;
import br.edu.utfpr.sd.garagem.server.log.LogMasking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Trata uma conexão de cliente em sua própria thread do pool: lê uma linha
 * JSON por vez, delega ao {@link RequestDispatcher} e escreve a resposta,
 * repetindo até a conexão ser encerrada. Também aceita mensagens fora desse
 * ciclo (ver {@link #push}), usadas pelo operador do servidor para avisar o
 * cliente de algo sem esperar a próxima requisição dele.
 */
public final class ClientHandler implements Runnable, ClientSession {

    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final RequestDispatcher dispatcher;
    private final ServerEventListener listener;
    private final ConnectedClientRegistry registry;

    private volatile PrintWriter writer;
    private volatile String boundToken;

    public ClientHandler(Socket socket, RequestDispatcher dispatcher, ServerEventListener listener,
                          ConnectedClientRegistry registry) {
        this.socket = socket;
        this.dispatcher = dispatcher;
        this.listener = listener;
        this.registry = registry;
    }

    @Override
    public void run() {
        Socket connection = this.socket;
        String remote = String.valueOf(connection.getRemoteSocketAddress());
        listener.onLog("Cliente conectado: " + remote);
        try (connection;
             BufferedReader reader = MessageIO.newReader(connection.getInputStream());
             PrintWriter out = MessageIO.newWriter(connection.getOutputStream())) {
            this.writer = out;
            String line;
            while ((line = reader.readLine()) != null) {
                listener.onLog("<- " + LogMasking.maskPassword(line));
                Response response = dispatcher.dispatch(line, this);
                String json = JsonSupport.GSON.toJson(response);
                out.println(json);
                listener.onLog("-> " + json);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "conexao encerrada com erro (" + remote + "): " + e.getMessage());
        } finally {
            unbindToken();
            listener.onLog("Cliente desconectado: " + remote);
        }
    }

    @Override
    public void bindToken(String token) {
        this.boundToken = token;
        registry.register(token, this);
    }

    @Override
    public void unbindToken() {
        String token = this.boundToken;
        if (token != null) {
            registry.unregister(token);
            this.boundToken = null;
        }
    }

    /**
     * Envia uma mensagem para este cliente fora do ciclo requisição/resposta
     * normal (ex.: aviso de sessão encerrada pelo operador). Chamado de uma
     * thread diferente da que roda {@link #run()} — {@link PrintWriter}
     * sincroniza cada chamada de {@code println} internamente, então a linha
     * nunca sai corrompida, ainda que possa intercalar com a próxima resposta
     * normal desta conexão.
     */
    void push(Response response) {
        PrintWriter out = this.writer;
        if (out == null) {
            return;
        }
        String json = JsonSupport.GSON.toJson(response);
        out.println(json);
        listener.onLog("-> (push) " + json);
    }

    /**
     * Fecha a conexão de fora da thread de {@link #run()} — usado quando o
     * próprio servidor está parando. Isso faz a leitura bloqueada em
     * {@code reader.readLine()} lançar {@link IOException}, encerrando o
     * loop de {@link #run()} normalmente pelo caminho de erro já existente.
     */
    void forceClose() {
        try {
            socket.close();
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "erro ao forcar o fechamento da conexao (ignorado)", e);
        }
    }
}
