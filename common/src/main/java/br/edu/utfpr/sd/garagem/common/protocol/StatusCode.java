package br.edu.utfpr.sd.garagem.common.protocol;

/**
 * Códigos de status das respostas do protocolo, com semântica inspirada em
 * HTTP. A planilha de protocolo não fixa esses valores; centralizá-los aqui
 * torna a troca dos números uma mudança em um único lugar.
 */
public final class StatusCode {

    /** Requisição processada com sucesso. */
    public static final int OK = 200;

    /** Recurso criado com sucesso (ex.: cadastro de usuário). */
    public static final int CREATED = 201;

    /** Requisição malformada ou com dados que falharam na validação. */
    public static final int BAD_REQUEST = 400;

    /** Credenciais inválidas ou token inválido/expirado. */
    public static final int UNAUTHORIZED = 401;

    /**
     * Autenticado, mas sem permissão sobre o recurso pedido (ex.: token de
     * um usuário usado para operar sobre o cadastro de outro username).
     * Distinto de {@link #UNAUTHORIZED}, que é para token ausente/inválido.
     */
    public static final int FORBIDDEN = 403;

    /** Recurso ou operação não encontrada. */
    public static final int NOT_FOUND = 404;

    /** Conflito de estado (ex.: recurso já existe). */
    public static final int CONFLICT = 409;

    /** Erro inesperado no servidor. */
    public static final int INTERNAL_ERROR = 500;

    private StatusCode() {
    }
}
