package br.edu.utfpr.sd.garagem.common.protocol;

/**
 * Requisição de logout: {@code { "method": "logout", "data": { "token": "" } } }.
 * Suposição a validar com Nathan e Rafael: o formato de logout ainda não
 * está na planilha; seguimos aqui o mesmo padrão estrutural do login,
 * identificando a sessão pelo token recebido no login.
 */
public final class LogoutRequest {

    private final String method = Methods.LOGOUT;
    private final Data data;

    public LogoutRequest(String token) {
        this.data = new Data(token);
    }

    public String getMethod() {
        return method;
    }

    public Data getData() {
        return data;
    }

    public String getToken() {
        return data.token;
    }

    /** Carga de dados de {@code data} na requisição de logout. */
    public static final class Data {

        private final String token;

        public Data(String token) {
            this.token = token;
        }

        public String getToken() {
            return token;
        }
    }
}
