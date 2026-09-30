package br.edu.utfpr.sd.garagem.common.protocol;

/**
 * Carga de dados devolvida em {@code data} na resposta de {@code getuser}:
 * {@code { "name": "", "username": "" } }.
 */
public final class UserData {

    private final String name;
    private final String username;

    public UserData(String name, String username) {
        this.name = name;
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }
}
