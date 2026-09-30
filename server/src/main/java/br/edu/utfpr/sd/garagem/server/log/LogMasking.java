package br.edu.utfpr.sd.garagem.server.log;

import java.util.regex.Pattern;

/**
 * Mascara valores sensíveis antes de uma linha do protocolo ir para o log,
 * conforme exigido para a GUI do servidor (senhas nunca aparecem em log).
 * Cobre os três campos de senha do protocolo: {@code password} (login e
 * cadastro), {@code oldPassword} e {@code newPassword} (troca de senha).
 */
public final class LogMasking {

    private static final Pattern PASSWORD_FIELD = Pattern.compile(
            "(\"(?:password|oldPassword|newPassword)\"\\s*:\\s*\")(?:[^\"\\\\]|\\\\.)*(\")");

    private LogMasking() {
    }

    /** Substitui o valor de qualquer campo de senha por {@code ***} em uma linha JSON. */
    public static String maskPassword(String rawJson) {
        return PASSWORD_FIELD.matcher(rawJson).replaceAll("$1***$2");
    }
}
