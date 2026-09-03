package br.edu.utfpr.sd.garagem.client.net;

/**
 * Erro de comunicação com o servidor, já com mensagem amigável pronta para
 * exibição na GUI (nunca uma stack trace crua para o usuário).
 */
public final class ConnectionException extends Exception {

    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
