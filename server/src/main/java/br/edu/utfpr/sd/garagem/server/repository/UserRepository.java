package br.edu.utfpr.sd.garagem.server.repository;

import br.edu.utfpr.sd.garagem.common.model.User;

import java.util.Optional;

/** Acesso à base de usuários cadastrados. */
public interface UserRepository {

    /** Busca um usuário pelo username, se existir. */
    Optional<User> findByUsername(String username);

    /**
     * Cadastra um novo usuário, se o username ainda não existir.
     * @return {@code true} se o cadastro foi criado; {@code false} se o username já estava em uso.
     */
    boolean save(User user);

    /**
     * Atualiza o campo {@code name} de um usuário existente.
     * @return {@code true} se o usuário existia e foi atualizado; {@code false} caso contrário.
     */
    boolean updateName(String username, String name);

    /**
     * Atualiza o hash de senha de um usuário existente.
     * @return {@code true} se o usuário existia e foi atualizado; {@code false} caso contrário.
     */
    boolean updatePasswordHash(String username, String passwordHash);

    /**
     * Remove um usuário existente pelo username.
     * @return {@code true} se o usuário existia e foi removido; {@code false} caso contrário.
     */
    boolean deleteByUsername(String username);
}
