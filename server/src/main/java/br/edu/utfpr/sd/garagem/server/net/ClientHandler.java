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
 * repetindo até a conexão ser encerrada.
 */
public final class ClientHandler implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final RequestDispatcher dispatcher;
    private final ServerEventListener listener;

    public ClientHandler(Socket socket, RequestDispatcher dispatcher, ServerEventListener listener) {
        this.socket = socket;
        this.dispatcher = dispatcher;
        this.listener = listener;
    }

    @Override
    public void run() {
        Socket connection = this.socket;
        String remote = String.valueOf(connection.getRemoteSocketAddress());
        listener.onLog("cliente conectado: " + remote);
        try (connection;
             BufferedReader reader = MessageIO.newReader(connection.getInputStream());
             PrintWriter writer = MessageIO.newWriter(connection.getOutputStream())) {
            String line;
            while ((line = reader.readLine()) != null) {
                listener.onLog("<- " + LogMasking.maskPassword(line));
                Response response = dispatcher.dispatch(line);
                String json = JsonSupport.GSON.toJson(response);
                writer.println(json);
                listener.onLog("-> " + json);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "conexao encerrada com erro (" + remote + "): " + e.getMessage());
        } finally {
            listener.onLog("cliente desconectado: " + remote);
        }
    }
}
