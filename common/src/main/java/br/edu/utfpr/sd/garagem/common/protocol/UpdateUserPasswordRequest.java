package br.edu.utfpr.sd.garagem.common.protocol;

/**
 * Requisição de troca de senha do usuário autenticado:
 * {@code { "method": "updateuserpassword", "data": { "token": "", "username": "", "oldPassword": "", "newPassword": "" } } }.
 */
public final class UpdateUserPasswordRequest {

    private final String method = Methods.UPDATE_USER_PASSWORD;
    private final Data data;

    public UpdateUserPasswordRequest(String token, String username, String oldPassword, String newPassword) {
        this.data = new Data(token, username, oldPassword, newPassword);
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

    public String getOldPassword() {
        return data.oldPassword;
    }

    public String getNewPassword() {
        return data.newPassword;
    }

    /** Carga de dados de {@code data} na requisição de troca de senha. */
    public static final class Data {

        private final String token;
        private final String username;
        private final String oldPassword;
        private final String newPassword;

        public Data(String token, String username, String oldPassword, String newPassword) {
            this.token = token;
            this.username = username;
            this.oldPassword = oldPassword;
            this.newPassword = newPassword;
        }

        public String getToken() {
            return token;
        }

        public String getUsername() {
            return username;
        }

        public String getOldPassword() {
            return oldPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }
    }
}
