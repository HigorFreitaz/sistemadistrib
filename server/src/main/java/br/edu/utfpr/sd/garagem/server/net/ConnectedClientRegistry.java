package br.edu.utfpr.sd.garagem.server.net;

import br.edu.utfpr.sd.garagem.common.protocol.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Associa cada token de sessão ativa ao {@link ClientHandler} que a
 * autenticou, para o servidor conseguir empurrar uma mensagem para um
 * cliente específico sem esperar a próxima requisição dele — ex.: avisar
 * que a sessão foi encerrada pelo operador antes que o cliente peça
 * qualquer coisa.
 */
public final class ConnectedClientRegistry {

    private final Map<String, ClientHandler> handlersByToken = new ConcurrentHashMap<>();

    void register(String token, ClientHandler handler) {
        handlersByToken.put(token, handler);
    }

    void unregister(String token) {
        handlersByToken.remove(token);
    }

    /** Envia a mesma mensagem para todos os clientes com sessão ativa agora, e esquece deles. */
    public void broadcast(Response response) {
        for (ClientHandler handler : drainHandlers()) {
            handler.push(response);
        }
    }

    /**
     * Como {@link #broadcast}, mas também força o encerramento da conexão
     * de cada cliente — usado quando o próprio servidor está parando, então
     * não há sentido em esperar o cliente fechar por conta própria.
     */
    public void broadcastAndDisconnect(Response response) {
        for (ClientHandler handler : drainHandlers()) {
            handler.push(response);
            handler.forceClose();
        }
    }

    /** Tira uma foto de quem está registrado agora e já esvazia o registro. */
    private List<ClientHandler> drainHandlers() {
        List<ClientHandler> handlers = new ArrayList<>(handlersByToken.values());
        handlersByToken.clear();
        return handlers;
    }
}
