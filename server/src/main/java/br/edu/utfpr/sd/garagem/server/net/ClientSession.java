package br.edu.utfpr.sd.garagem.server.net;

/**
 * Vínculo entre uma conexão de cliente e o token autenticado nela, usado
 * pelo {@link RequestDispatcher} para registrar/liberar a conexão no
 * {@link ConnectedClientRegistry} sem depender da classe concreta de
 * {@link ClientHandler}.
 */
public interface ClientSession {

    /** Chamado quando um login autentica esta conexão com o token informado. */
    void bindToken(String token);

    /** Chamado quando o token vinculado a esta conexão deixa de ser válido. */
    void unbindToken();
}
