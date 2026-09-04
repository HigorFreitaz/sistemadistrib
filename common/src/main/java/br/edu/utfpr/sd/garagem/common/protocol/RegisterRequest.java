package br.edu.utfpr.sd.garagem.common.protocol;

/**
 * Requisição de cadastro: {@code { "method": "register", "username": "", "password": "" } }.
 * Suposição a validar com Nathan e Rafael: o formato de cadastro ainda não
 * está na planilha; seguimos o mesmo padrão estrutural do login.
 */
public final class RegisterRequest {

    private final String method = Methods.REGISTER;
    private final String username;
    private final String password;

    public RegisterRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getMethod() {
        return method;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
