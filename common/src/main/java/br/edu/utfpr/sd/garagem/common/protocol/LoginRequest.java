package br.edu.utfpr.sd.garagem.common.protocol;

/**
 * Requisição de login: {@code { "method": "login", "data": { "username": "", "password": "" } } }.
 * Formato definido na planilha de protocolo (única operação fechada hoje).
 */
public final class LoginRequest {

    private final String method = Methods.LOGIN;
    private final Data data;

    public LoginRequest(String username, String password) {
        this.data = new Data(username, password);
    }

    public String getMethod() {
        return method;
    }

    public Data getData() {
        return data;
    }

    public String getUsername() {
        return data.username;
    }

    public String getPassword() {
        return data.password;
    }

    /** Carga de dados de {@code data} na requisição de login. */
    public static final class Data {

        private final String username;
        private final String password;

        public Data(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }
}
