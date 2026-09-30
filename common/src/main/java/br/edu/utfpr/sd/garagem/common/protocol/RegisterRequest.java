package br.edu.utfpr.sd.garagem.common.protocol;

/**
 * Requisição de cadastro:
 * {@code { "method": "register", "data": { "name": "", "username": "", "password": "" } } },
 * conforme a planilha de protocolo.
 */
public final class RegisterRequest {

    private final String method = Methods.REGISTER;
    private final Data data;

    public RegisterRequest(String name, String username, String password) {
        this.data = new Data(name, username, password);
    }

    public String getMethod() {
        return method;
    }

    public Data getData() {
        return data;
    }

    public String getName() {
        return data.name;
    }

    public String getUsername() {
        return data.username;
    }

    public String getPassword() {
        return data.password;
    }

    /** Carga de dados de {@code data} na requisição de cadastro. */
    public static final class Data {

        private final String name;
        private final String username;
        private final String password;

        public Data(String name, String username, String password) {
            this.name = name;
            this.username = username;
            this.password = password;
        }

        public String getName() {
            return name;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }
}
