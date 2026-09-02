package br.edu.utfpr.sd.garagem.common.model;

import java.time.Instant;

/**
 * Sessão ativa de um usuário autenticado: associa um token (UUID v4) ao
 * username, com os instantes de criação e de último acesso.
 */
public final class Session {

    private String token;
    private String username;
    private Instant createdAt;
    private Instant lastAccessAt;

    public Session(String token, String username, Instant createdAt, Instant lastAccessAt) {
        this.token = token;
        this.username = username;
        this.createdAt = createdAt;
        this.lastAccessAt = lastAccessAt;
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastAccessAt() {
        return lastAccessAt;
    }

    /** Atualiza o instante do último acesso, chamado a cada validação de token. */
    public void touch() {
        this.lastAccessAt = Instant.now();
    }
}
