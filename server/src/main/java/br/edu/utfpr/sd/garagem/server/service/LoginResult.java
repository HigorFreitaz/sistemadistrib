package br.edu.utfpr.sd.garagem.server.service;

import br.edu.utfpr.sd.garagem.common.model.Session;

/**
 * Resultado de uma tentativa de login, distinguindo usuário inexistente de
 * senha incorreta — conforme o fluxo descrito em
 * {@code docs/Requisitos Funcionais e não funcionais.docx}.
 * <p>
 * Suposição a validar com a turma: revelar qual dos dois campos falhou
 * facilita a vida de quem está testando, mas permite a um atacante
 * descobrir quais usernames existem na base (enumeração de usuários). Os
 * documentos de referência pedem explicitamente mensagens distintas para
 * cada caso, então seguimos o que foi especificado.
 */
public final class LoginResult {

    /** Motivo do resultado do login. */
    public enum Status {
        SUCCESS,
        USER_NOT_FOUND,
        WRONG_PASSWORD
    }

    private final Status status;
    private final Session session;

    private LoginResult(Status status, Session session) {
        this.status = status;
        this.session = session;
    }

    public static LoginResult success(Session session) {
        return new LoginResult(Status.SUCCESS, session);
    }

    public static LoginResult userNotFound() {
        return new LoginResult(Status.USER_NOT_FOUND, null);
    }

    public static LoginResult wrongPassword() {
        return new LoginResult(Status.WRONG_PASSWORD, null);
    }

    public Status getStatus() {
        return status;
    }

    /** Sessão aberta, presente somente quando {@code status} é {@link Status#SUCCESS}. */
    public Session getSession() {
        return session;
    }
}
