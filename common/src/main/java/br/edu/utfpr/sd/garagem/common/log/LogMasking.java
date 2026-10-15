package br.edu.utfpr.sd.garagem.common.log;

import java.util.regex.Pattern;

/**
 * Mascara valores de campos de senha antes de uma linha do protocolo ir
 * para o log. Cobre os três campos: {@code password}, {@code oldPassword}
 * e {@code newPassword}.
 */
public final class LogMasking {

    private static final Pattern PASSWORD_FIELD = Pattern.compile(
            "(\"(?:password|oldPassword|newPassword)\"\\s*:\\s*\")(?:[^\"\\\\]|\\\\.)*(\")");

    private LogMasking() {
    }

    /** Substitui o valor de qualquer campo de senha por {@code ***}. */
    public static String maskPassword(String rawJson) {
        return PASSWORD_FIELD.matcher(rawJson).replaceAll("$1***$2");
    }
}
