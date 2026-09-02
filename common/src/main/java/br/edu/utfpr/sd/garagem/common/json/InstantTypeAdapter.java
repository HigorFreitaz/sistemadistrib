package br.edu.utfpr.sd.garagem.common.json;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;

/**
 * Adaptador Gson para {@link Instant}, serializado como string ISO-8601 (UTC).
 * O Gson não sabe lidar com tipos de {@code java.time} nativamente.
 */
public final class InstantTypeAdapter extends TypeAdapter<Instant> {

    @Override
    public void write(JsonWriter out, Instant value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.value(value.toString());
    }

    @Override
    public Instant read(JsonReader in) throws IOException {
        String value = in.nextString();
        return Instant.parse(value);
    }
}
