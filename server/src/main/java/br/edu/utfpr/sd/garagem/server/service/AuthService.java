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
    public boolean register(String username, String password) {
        User user = new User(username, PasswordHasher.hash(password), UserRole.CLIENTE);
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

    /** Quantidade de sessões ativas, para exibição na GUI do servidor. */
    public int activeSessionCount() {
        return sessionService.activeSessionCount();
    }
}
