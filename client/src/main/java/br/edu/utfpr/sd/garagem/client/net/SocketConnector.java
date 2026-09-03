package br.edu.utfpr.sd.garagem.client.net;

import br.edu.utfpr.sd.garagem.common.json.JsonSupport;
import br.edu.utfpr.sd.garagem.common.protocol.Response;
import br.edu.utfpr.sd.garagem.common.transport.MessageIO;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Conexão de socket TCP com o servidor, usada por toda a sessão do cliente
 * (aberta no login, reaproveitada até o logout). Traduz qualquer falha de
 * rede em {@link ConnectionException} com mensagem amigável, nunca deixando
 * uma exceção crua chegar à GUI.
 */
public final class SocketConnector implements AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(SocketConnector.class.getName());
    private static final int DEFAULT_TIMEOUT_MS = 5000;

    private final String host;
    private final int port;
    private final int timeoutMs;

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public SocketConnector(String host, int port) {
        this(host, port, DEFAULT_TIMEOUT_MS);
    }

    public SocketConnector(String host, int port, int timeoutMs) {
        this.host = host;
        this.port = port;
        this.timeoutMs = timeoutMs;
    }

    /** Abre a conexão TCP com o servidor, com timeout de conexão e de leitura. */
    public void connect() throws ConnectionException {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);
            reader = MessageIO.newReader(socket.getInputStream());
            writer = MessageIO.newWriter(socket.getOutputStream());
        } catch (UnknownHostException e) {
            throw new ConnectionException("Endereço do servidor não encontrado. Verifique o host informado.", e);
        } catch (SocketTimeoutException e) {
            throw new ConnectionException("Tempo de conexão esgotado. O servidor pode estar sobrecarregado ou inacessível.", e);
        } catch (ConnectException e) {
            throw new ConnectionException("Não foi possível conectar ao servidor. Verifique se ele está em execução e se o host/porta estão corretos.", e);
        } catch (IOException e) {
            throw new ConnectionException("Falha de comunicação com o servidor.", e);
        }
    }

    /** Envia uma requisição e aguarda a resposta correspondente, em uma linha JSON. */
    public Response send(Object request) throws ConnectionException {
        try {
            writer.println(JsonSupport.GSON.toJson(request));
            if (writer.checkError()) {
                throw new IOException("falha ao enviar dados para o servidor");
            }
            String line = reader.readLine();
            if (line == null) {
                throw new IOException("conexão encerrada pelo servidor");
            }
            return JsonSupport.GSON.fromJson(line, Response.class);
        } catch (SocketTimeoutException e) {
            throw new ConnectionException("O servidor demorou demais para responder.", e);
        } catch (JsonSyntaxException e) {
            throw new ConnectionException("Resposta inválida recebida do servidor.", e);
        } catch (IOException e) {
            throw new ConnectionException("Falha de comunicação com o servidor.", e);
        }
    }

    @Override
    public void close() {
        if (socket == null) {
            return;
        }
        try {
            socket.close();
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "erro ao fechar a conexao (ignorado, conexao ja pode estar encerrada)", e);
        }
    }
}
