package br.edu.utfpr.sd.garagem.common.validation;

import java.util.regex.Pattern;

/**
 * Valida o formato de {@code username} conforme os requisitos não
 * funcionais: somente letras minúsculas e números, símbolos {@code .} e
 * {@code _} liberados, entre 3 e 20 caracteres, sem acentuação nem espaços.
 * Usado tanto no cliente (feedback imediato) quanto no servidor
 * (verificação definitiva — o cliente nunca é confiável).
 * <p>
 * As duas regras (tamanho e conjunto de caracteres) são expostas
 * separadamente para permitir uma tela de cadastro com feedback granular
 * (ex.: um checklist que vai ficando verde regra por regra).
 */
public final class UsernameValidator {

    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 20;
    private static final Pattern CHARSET_PATTERN = Pattern.compile("^[a-z0-9._]*$");

    private UsernameValidator() {
    }

    /** Retorna {@code true} se {@code username} atende a todas as regras de formato. */
    public static boolean isValid(String username) {
        return hasValidLength(username) && hasOnlyAllowedCharacters(username);
    }

    /** Retorna {@code true} se o tamanho de {@code username} está entre 3 e 20 caracteres. */
    public static boolean hasValidLength(String username) {
        return username != null && username.length() >= MIN_LENGTH && username.length() <= MAX_LENGTH;
    }

    /** Retorna {@code true} se {@code username} só contém caracteres permitidos (e não é vazio). */
    public static boolean hasOnlyAllowedCharacters(String username) {
        return username != null && !username.isEmpty() && CHARSET_PATTERN.matcher(username).matches();
    }
}
