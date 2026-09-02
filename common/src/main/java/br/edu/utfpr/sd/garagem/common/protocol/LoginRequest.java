package br.edu.utfpr.sd.garagem.common.protocol;

/**
 * Requisição de login: {@code { "method": "login", "username": "", "password": "" } }.
 * Formato definido na planilha de protocolo (única operação fechada hoje).
 */
public final class LoginRequest {

    private final String method = Methods.LOGIN;
    private final String username;
    private final String password;

    public LoginRequest(String username, String password) {
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
