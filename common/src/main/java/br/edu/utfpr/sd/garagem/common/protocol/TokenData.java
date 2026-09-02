package br.edu.utfpr.sd.garagem.common.protocol;

/**
 * Carga de dados devolvida em {@code data} na resposta de login:
 * {@code { "token": "" } }.
 */
public final class TokenData {

    private final String token;

    public TokenData(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
