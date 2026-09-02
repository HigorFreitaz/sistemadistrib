package br.edu.utfpr.sd.garagem.server.repository;

import br.edu.utfpr.sd.garagem.common.model.User;

import java.util.Optional;

/** Acesso à base de usuários cadastrados. */
public interface UserRepository {

    /** Busca um usuário pelo username, se existir. */
    Optional<User> findByUsername(String username);
}
