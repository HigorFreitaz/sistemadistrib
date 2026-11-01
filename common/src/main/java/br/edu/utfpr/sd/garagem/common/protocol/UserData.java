package br.edu.utfpr.sd.garagem.common.protocol;

import br.edu.utfpr.sd.garagem.common.model.UserRole;

public final class UserData {

    private final String name;
    private final String username;
    private final UserRole role;

    public UserData(String name, String username, UserRole role) {
        this.name = name;
        this.username = username;
        this.role = role;
    }

    public String getName() { return name; }
    public String getUsername() { return username; }
    public UserRole getRole() { return role; }
}
