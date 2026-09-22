package br.edu.utfpr.sd.garagem.common.protocol;

import br.edu.utfpr.sd.garagem.common.json.JsonSupport;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

/**
 * Envelope de resposta de qualquer operação do protocolo:
 * {@code { "statusCode": 0, "message": "", "data": {} } }.
 * {@code data} é mantido como {@link JsonElement} bruto porque seu formato
 * varia por operação (ex.: {@link TokenData} no login, nada no logout).
 */
public final class Response {

    private final int statusCode;
    private final String message;
    private final JsonElement data;

    public Response(int statusCode, String message, JsonElement data) {
        this.statusCode = statusCode;
        this.message = message;
        this.data = data == null ? JsonNull.INSTANCE : data;
    }

    /** Constrói uma resposta de sucesso, convertendo {@code data} para JSON. */
    public static Response ok(String message, Object data) {
        return success(StatusCode.OK, message, data);
    }

    /** Como {@link #ok}, mas para quando a operação criou um recurso novo (ex.: cadastro). */
    public static Response created(String message, Object data) {
        return success(StatusCode.CREATED, message, data);
    }

    private static Response success(int statusCode, String message, Object data) {
        JsonElement json = data == null ? JsonNull.INSTANCE : JsonSupport.GSON.toJsonTree(data);
        return new Response(statusCode, message, json);
    }

    /** Constrói uma resposta de erro, sem dado associado. */
    public static Response error(int statusCode, String message) {
        return new Response(statusCode, message, JsonNull.INSTANCE);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public JsonElement getData() {
        return data;
    }
}
