package br.edu.utfpr.sd.garagem.server.service;

import br.edu.utfpr.sd.garagem.common.model.Session;
import br.edu.utfpr.sd.garagem.server.repository.SessionRepository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerencia o ciclo de vida das sessões em memória (thread-safe) e delega a
 * persistência ao {@link SessionRepository} a cada mutação.
 * <p>
 * A criação/remoção de sessão é uma região crítica isolada de propósito
 * (bloco {@code synchronized}): é o mesmo padrão de isolamento que a EP-2
 * vai precisar para a contagem de vagas disponíveis nunca ficar negativa.
 */
public final class SessionService {

    private final SessionRepository repository;
    private final Map<String, Session> sessionsByToken;
    private final Map<String, String> tokenByUsername = new ConcurrentHashMap<>();
    private final Object lock = new Object();

    public SessionService(SessionRepository repository) {
        this.repository = repository;
        this.sessionsByToken = new ConcurrentHashMap<>(repository.loadAll());
        sessionsByToken.values().forEach(session -> tokenByUsername.put(session.getUsername(), session.getToken()));
    }

    /**
     * Abre uma nova sessão para o usuário, invalidando qualquer sessão
     * anterior dele. Suposição a validar com a turma: um usuário não deve
     * acumular sessões simultâneas, então logar de novo derruba a sessão
     * anterior.
     */
    public Session login(String username) {
        synchronized (lock) {
            String previousToken = tokenByUsername.remove(username);
            if (previousToken != null) {
                sessionsByToken.remove(previousToken);
            }
            Instant now = Instant.now();
            Session session = new Session(UUID.randomUUID().toString(), username, now, now);
            sessionsByToken.put(session.getToken(), session);
            tokenByUsername.put(username, session.getToken());
            repository.saveAll(sessionsByToken.values());
            return session;
        }
    }

    /**
     * Resolve a sessão associada a um token, atualizando o instante de
     * último acesso em memória. Suposição a validar com a turma: esse
     * instante só é persistido em disco quando a sessão é criada ou
     * encerrada, para não gravar o arquivo a cada requisição autenticada.
     */
    public Optional<Session> findByToken(String token) {
        Session session = sessionsByToken.get(token);
        if (session == null) {
            return Optional.empty();
        }
        session.touch();
        return Optional.of(session);
    }

    /** Encerra a sessão associada ao token, se existir. */
    public boolean logout(String token) {
        synchronized (lock) {
            Session removed = sessionsByToken.remove(token);
            if (removed == null) {
                return false;
            }
            tokenByUsername.remove(removed.getUsername(), token);
            repository.saveAll(sessionsByToken.values());
            return true;
        }
    }

    /** Quantidade de sessões ativas no momento, para exibição na GUI do servidor. */
    public int activeSessionCount() {
        return sessionsByToken.size();
    }
}
