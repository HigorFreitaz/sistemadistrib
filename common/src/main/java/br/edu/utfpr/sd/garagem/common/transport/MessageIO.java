package br.edu.utfpr.sd.garagem.common.transport;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Fábrica dos leitores/escritores usados no framing do protocolo: um objeto
 * JSON por linha (newline-delimited JSON), sempre em UTF-8. Centralizar essa
 * decisão aqui evita que cliente e servidor divirjam na configuração do
 * encoding ou do auto-flush.
 */
public final class MessageIO {

    private MessageIO() {
    }

    /** Cria um leitor de linhas em UTF-8 a partir do stream de entrada do socket. */
    public static BufferedReader newReader(InputStream in) {
        return new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
    }

    /** Cria um escritor com auto-flush em UTF-8 a partir do stream de saída do socket. */
    public static PrintWriter newWriter(OutputStream out) {
        return new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), true);
    }
}
