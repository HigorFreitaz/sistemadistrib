package br.edu.utfpr.sd.garagem.server.log;

import java.util.regex.Pattern;

/**
 * Mascara valores sensíveis antes de uma linha do protocolo ir para o log,
 * conforme exigido para a GUI do servidor (senhas nunca aparecem em log).
 */
public final class LogMasking {

    private static final Pattern PASSWORD_FIELD =
            Pattern.compile("(\"password\"\\s*:\\s*\")(?:[^\"\\\\]|\\\\.)*(\")");

    private LogMasking() {
    }

    /** Substitui o valor do campo {@code password} por {@code ***} em uma linha JSON. */
    public static String maskPassword(String rawJson) {
        return PASSWORD_FIELD.matcher(rawJson).replaceAll("$1***$2");
    }
}
