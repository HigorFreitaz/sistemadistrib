package br.edu.utfpr.sd.garagem.server.service;

import br.edu.utfpr.sd.garagem.common.model.Session;
import br.edu.utfpr.sd.garagem.common.model.User;
import br.edu.utfpr.sd.garagem.common.model.UserRole;
import br.edu.utfpr.sd.garagem.server.repository.UserRepository;
import br.edu.utfpr.sd.garagem.server.security.PasswordHasher;

import java.util.Optional;

/**
 * Serviço de autenticação: valida credenciais contra a base de usuários e
 * abre sessões; também é o ponto central de verificação de token,
 * reutilizável por qualquer operação autenticada futura (EP-2).
 */
public final class AuthService {

    private final UserRepository userRepository;
    private final SessionService sessionService;

    public AuthService(UserRepository userRepository, SessionService sessionService) {
        this.userRepository = userRepository;
        this.sessionService = sessionService;
    }

    /**
     * Autentica usuário e senha; se válidos, abre uma nova sessão.
     * Distingue usuário inexistente de senha incorreta (ver {@link LoginResult}).
     */
    public LoginResult login(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            return LoginResult.userNotFound();
        }
        if (!PasswordHasher.matches(password, user.get().getPasswordHash())) {
            return LoginResult.wrongPassword();
        }
        return LoginResult.success(sessionService.login(user.get().getUsername()));
    }

    /**
     * Cadastra um novo usuário com o papel padrão {@link UserRole#CLIENTE}.
     * @return {@code true} se o cadastro foi criado; {@code false} se o username já estava em uso.
     */
    public boolean register(String name, String username, String password) {
        User user = new User(name, username, PasswordHasher.hash(password), UserRole.CLIENTE);
        return userRepository.save(user);
    }

    /** Encerra a sessão associada ao token, se existir. */
    public boolean logout(String token) {
        return sessionService.logout(token);
    }

    /** Resolve a sessão associada a um token, para autenticar operações futuras. */
    public Optional<Session> resolveSession(String token) {
        return sessionService.findByToken(token);
    }

    /** Busca os dados de um usuário cadastrado, para a operação {@code getuser}. */
    public Optional<User> getUserData(String username) {
        return userRepository.findByUsername(username);
    }

    /** Atualiza o {@code name} de um usuário existente. */
    public boolean updateName(String username, String name) {
        return userRepository.updateName(username, name);
    }

    /**
     * Troca a senha de um usuário, verificando antes se {@code oldPassword}
     * confere com a senha atual — ver {@link UpdatePasswordResult}.
     */
    public UpdatePasswordResult updatePassword(String username, String oldPassword, String newPassword) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            return UpdatePasswordResult.userNotFound();
        }
        if (!PasswordHasher.matches(oldPassword, user.get().getPasswordHash())) {
            return UpdatePasswordResult.wrongOldPassword();
        }
        userRepository.updatePasswordHash(username, PasswordHasher.hash(newPassword));
        return UpdatePasswordResult.success();
    }

    /**
     * Remove o cadastro de um usuário e encerra sua sessão (ver
     * {@link SessionService#logout}) — o usuário não existe mais, então o
     * token dele não pode continuar valendo.
     * @return {@code true} se o usuário existia e foi removido.
     */
    public boolean deleteUser(String username, String token) {
        boolean deleted = userRepository.deleteByUsername(username);
        if (deleted) {
            sessionService.logout(token);
        }
        return deleted;
    }

    /** Quantidade de sessões ativas, para exibição na GUI do servidor. */
    public int activeSessionCount() {
        return sessionService.activeSessionCount();
    }

    /** Derruba todas as sessões ativas de uma vez, a pedido do operador do servidor. */
    public void logoutAllSessions() {
        sessionService.logoutAll();
    }
}
