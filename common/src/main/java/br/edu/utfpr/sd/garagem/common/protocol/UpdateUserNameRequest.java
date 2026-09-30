package br.edu.utfpr.sd.garagem.common.protocol;

/**
 * Requisição de atualização do nome do usuário autenticado (o campo
 * alterado é {@code name}, apesar do nome do método na planilha de
 * protocolo):
 * {@code { "method": "updateusername", "data": { "token": "", "username": "", "name": "" } } }.
 */
public final class UpdateUserNameRequest {

    private final String method = Methods.UPDATE_USER_NAME;
    private final Data data;

    public UpdateUserNameRequest(String token, String username, String name) {
        this.data = new Data(token, username, name);
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

    public String getName() {
        return data.name;
    }

    /** Carga de dados de {@code data} na requisição de atualização de nome. */
    public static final class Data {

        private final String token;
        private final String username;
        private final String name;

        public Data(String token, String username, String name) {
            this.token = token;
            this.username = username;
            this.name = name;
        }

        public String getToken() {
            return token;
        }

        public String getUsername() {
            return username;
        }

        public String getName() {
            return name;
        }
    }
}
