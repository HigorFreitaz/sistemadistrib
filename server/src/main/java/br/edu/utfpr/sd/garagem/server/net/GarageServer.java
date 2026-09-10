package br.edu.utfpr.sd.garagem.server.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orquestra o socket servidor: aceita conexões em uma thread dedicada e
 * delega cada uma a um {@link ClientHandler}, executado em um pool de
 * threads limitado (nunca uma thread solta por cliente sem limite).
 */
public final class GarageServer {

    private static final Logger LOGGER = Logger.getLogger(GarageServer.class.getName());

    /** Tamanho fixo do pool de threads de atendimento a clientes. */
    private static final int MAX_CLIENTS = 50;

    private final int port;
    private final RequestDispatcher dispatcher;
    private final ServerEventListener listener;
    private final ConnectedClientRegistry registry;
    private final AtomicInteger connectedClients = new AtomicInteger();

    private ExecutorService pool;
    private ServerSocket serverSocket;
    private volatile boolean running;

    public GarageServer(int port, RequestDispatcher dispatcher, ServerEventListener listener,
                         ConnectedClientRegistry registry) {
        this.port = port;
        this.dispatcher = dispatcher;
        this.listener = listener;
        this.registry = registry;
    }

    /** Abre o server socket e começa a aceitar conexões em segundo plano. */
    public synchronized void start() throws IOException {
        if (running) {
            return;
        }
        serverSocket = new ServerSocket(port);
        pool = Executors.newFixedThreadPool(MAX_CLIENTS);
        running = true;
        Thread acceptThread = new Thread(this::acceptLoop, "server-accept-loop");
        acceptThread.setDaemon(true);
        acceptThread.start();
        listener.onStarted(getPort());
    }

    /** Porta em que o servidor está de fato escutando (relevante quando a porta configurada é 0, escolhida pelo SO). */
    public synchronized int getPort() {
        return serverSocket != null ? serverSocket.getLocalPort() : port;
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                listener.onClientCountChanged(connectedClients.incrementAndGet());
                pool.submit(() -> runClient(socket));
            } catch (IOException e) {
                if (running) {
                    LOGGER.log(Level.WARNING, "falha ao aceitar conexao", e);
                }
            }
        }
    }

    private void runClient(Socket socket) {
        try {
            new ClientHandler(socket, dispatcher, listener, registry).run();
        } finally {
            listener.onClientCountChanged(connectedClients.decrementAndGet());
        }
    }

    /** Encerra o server socket e o pool de threads. */
    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "erro ao fechar o server socket", e);
        }
        pool.shutdownNow();
        listener.onStopped();
    }
}
