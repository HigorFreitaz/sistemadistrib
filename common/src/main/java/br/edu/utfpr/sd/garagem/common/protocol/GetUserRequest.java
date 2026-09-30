package br.edu.utfpr.sd.garagem.common.protocol;

/**
 * Requisição de consulta dos próprios dados do usuário autenticado:
 * {@code { "method": "getuser", "data": { "token": "", "username": "" } } }.
 */
public final class GetUserRequest {

    private final String method = Methods.GET_USER;
    private final Data data;

    public GetUserRequest(String token, String username) {
        this.data = new Data(token, username);
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

    public String getUsername() {
        return data.username;
    }

    /** Carga de dados de {@code data} na requisição de consulta de usuário. */
    public static final class Data {

        private final String token;
        private final String username;

        public Data(String token, String username) {
            this.token = token;
            this.username = username;
        }

        public String getToken() {
            return token;
        }

        public String getUsername() {
            return username;
        }
    }
}
