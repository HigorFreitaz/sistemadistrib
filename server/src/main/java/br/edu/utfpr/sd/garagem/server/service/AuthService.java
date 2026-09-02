package br.edu.utfpr.sd.garagem.server.service;

import br.edu.utfpr.sd.garagem.common.model.Session;
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

    /** Autentica usuário e senha; se válidos, abre uma nova sessão. */
    public Optional<Session> login(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(user -> PasswordHasher.matches(password, user.getPasswordHash()))
                .map(user -> sessionService.login(user.getUsername()));
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
