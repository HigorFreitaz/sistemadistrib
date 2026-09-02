package br.edu.utfpr.sd.garagem.common.validation;

import java.util.regex.Pattern;

/**
 * Valida o formato de {@code username} conforme os requisitos não
 * funcionais: somente letras minúsculas e números, símbolos {@code .} e
 * {@code _} liberados, entre 3 e 20 caracteres, sem acentuação nem espaços.
 * Usado tanto no cliente (feedback imediato) quanto no servidor
 * (verificação definitiva — o cliente nunca é confiável).
 */
public final class UsernameValidator {

    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9._]{3,20}$");

    private UsernameValidator() {
    }

    /** Retorna {@code true} se {@code username} atende a todas as regras de formato. */
    public static boolean isValid(String username) {
        return username != null && PATTERN.matcher(username).matches();
    }
}
