package br.edu.utfpr.sd.garagem.common.model;

/**
 * Usuário cadastrado no sistema. A senha nunca é mantida em texto puro:
 * {@code passwordHash} guarda o resultado do PBKDF2 aplicado pelo servidor,
 * no formato {@code iteracoes:saltBase64:hashBase64}.
 */
public final class User {

    private String name;
    private String username;
    private String passwordHash;
    private UserRole role;

    public User(String name, String username, String passwordHash, UserRole role) {
        this.name = name;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }
}
