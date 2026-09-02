package br.edu.utfpr.sd.garagem.common.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.Instant;

/**
 * Ponto único de configuração do Gson usado por cliente e servidor.
 * Centralizar aqui garante que ambos os lados serializem/desserializem o
 * protocolo exatamente da mesma forma.
 */
public final class JsonSupport {

    /**
     * Instância compartilhada do Gson.
     * O escape de HTML é desabilitado porque símbolos como {@code &} são
     * permitidos em senhas (ver requisitos não funcionais) e o Gson, por
     * padrão, os converte em sequências unicode, corrompendo o valor.
     */
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantTypeAdapter())
            .disableHtmlEscaping()
            .create();

    private JsonSupport() {
    }
}
