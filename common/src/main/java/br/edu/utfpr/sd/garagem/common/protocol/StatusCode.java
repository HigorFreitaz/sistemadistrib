package br.edu.utfpr.sd.garagem.common.protocol;

/**
 * Códigos de status das respostas do protocolo, com semântica inspirada em
 * HTTP. A planilha de protocolo não fixa esses valores; centralizá-los aqui
 * torna a troca dos números uma mudança em um único lugar.
 */
public final class StatusCode {

    /** Requisição processada com sucesso. */
    public static final int OK = 200;

    /** Requisição malformada ou com dados que falharam na validação. */
    public static final int BAD_REQUEST = 400;

    /** Credenciais inválidas ou token inválido/expirado. */
    public static final int UNAUTHORIZED = 401;

    /** Recurso ou operação não encontrada. */
    public static final int NOT_FOUND = 404;

    /** Conflito de estado (ex.: recurso já existe). */
    public static final int CONFLICT = 409;

    /** Erro inesperado no servidor. */
    public static final int INTERNAL_ERROR = 500;

    /** Servidor indisponivel (ex.: operador encerrou a sessao do cliente). */
    public static final int SERVICE_UNAVAILABLE = 503;

    private StatusCode() {
    }
}
