package br.edu.utfpr.sd.garagem.common.protocol;

/**
 * Requisição de logout: {@code { "method": "logout", "token": "" } }.
 * Suposição a validar com Nathan e Rafael: o formato de logout ainda não
 * está na planilha; seguimos aqui o mesmo padrão estrutural do login,
 * identificando a sessão pelo token recebido no login.
 */
public final class LogoutRequest {

    private final String method = Methods.LOGOUT;
    private final String token;

    public LogoutRequest(String token) {
        this.token = token;
    }

    public String getMethod() {
        return method;
    }

    public String getToken() {
        return token;
    }
}
