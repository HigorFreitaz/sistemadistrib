package br.edu.utfpr.sd.garagem.common.protocol;

/**
 * Requisição de cadastro: {@code { "method": "register", "data": { "username": "", "password": "" } } }.
 * Suposição a validar com Nathan e Rafael: o formato de cadastro ainda não
 * está na planilha; seguimos o mesmo padrão estrutural do login.
 */
public final class RegisterRequest {

    private final String method = Methods.REGISTER;
    private final Data data;

    public RegisterRequest(String username, String password) {
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

    /** Carga de dados de {@code data} na requisição de cadastro. */
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
