package br.edu.utfpr.sd.garagem.server.repository;

import br.edu.utfpr.sd.garagem.common.model.User;

import java.util.List;

/** Estrutura raiz do arquivo {@code usuarios.json}. */
final class UsersFile {

    private List<User> users;

    UsersFile() {
    }

    UsersFile(List<User> users) {
        this.users = users;
    }

    List<User> getUsers() {
        return users;
    }
}
