package br.edu.utfpr.sd.garagem.server.service;

/**
 * Resultado de uma tentativa de troca de senha, distinguindo usuário
 * inexistente de senha atual incorreta — mesmo padrão de {@link LoginResult}.
 */
public final class UpdatePasswordResult {

    /** Motivo do resultado da troca de senha. */
    public enum Status {
        SUCCESS,
        USER_NOT_FOUND,
        WRONG_OLD_PASSWORD
    }

    private final Status status;

    private UpdatePasswordResult(Status status) {
        this.status = status;
    }

    public static UpdatePasswordResult success() {
        return new UpdatePasswordResult(Status.SUCCESS);
    }

    public static UpdatePasswordResult userNotFound() {
        return new UpdatePasswordResult(Status.USER_NOT_FOUND);
    }

    public static UpdatePasswordResult wrongOldPassword() {
        return new UpdatePasswordResult(Status.WRONG_OLD_PASSWORD);
    }

    public Status getStatus() {
        return status;
    }
}
