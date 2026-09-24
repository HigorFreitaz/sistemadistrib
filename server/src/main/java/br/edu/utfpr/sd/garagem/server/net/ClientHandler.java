package br.edu.utfpr.sd.garagem.server.net;

import br.edu.utfpr.sd.garagem.common.json.JsonSupport;
import br.edu.utfpr.sd.garagem.common.protocol.Response;
import br.edu.utfpr.sd.garagem.common.protocol.StatusCode;
import br.edu.utfpr.sd.garagem.common.transport.MessageIO;
import br.edu.utfpr.sd.garagem.server.log.LogMasking;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Trata uma conexão de cliente em sua própria thread do pool: lê uma única
 * linha JSON, delega ao {@link RequestDispatcher}, escreve a resposta e
 * encerra a conexão. Uma conexão TCP nova por requisição — nenhum estado
 * fica associado a ela além da duração dessa troca.
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
        listener.onLog("Cliente conectado: " + remote);
        try (connection;
             BufferedReader reader = MessageIO.newReader(connection.getInputStream());
             PrintWriter out = MessageIO.newWriter(connection.getOutputStream())) {
            String line = reader.readLine();
            if (line != null) {
                listener.onLog("<- " + LogMasking.maskPassword(line));
                Response response;
                try {
                    response = dispatcher.dispatch(line);
                } catch (RuntimeException e) {
                    // Qualquer falha inesperada no processamento (ex.: erro de
                    // I/O ao persistir estado em disco) nao pode simplesmente
                    // fechar o socket sem resposta -- o cliente ficaria com
                    // leitura null, indistinguivel de "servidor caiu".
                    LOGGER.log(Level.SEVERE, "erro inesperado ao processar requisicao (" + remote + ")", e);
                    response = Response.error(StatusCode.INTERNAL_ERROR, "Erro interno no servidor");
                }
                String json = JsonSupport.GSON.toJson(response);
                out.println(json);
                listener.onLog("-> " + json);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "conexao encerrada com erro (" + remote + "): " + e.getMessage());
        } finally {
            listener.onLog("Cliente desconectado: " + remote);
        }
    }
}
