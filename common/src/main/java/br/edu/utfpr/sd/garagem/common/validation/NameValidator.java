package br.edu.utfpr.sd.garagem.common.validation;

import java.util.regex.Pattern;

/**
 * Valida o formato de {@code name} (nome da pessoa usuária) conforme os
 * requisitos não funcionais: somente letras e espaços, sem símbolos
 * especiais, não vazio, entre 1 e 60 caracteres.
 * <p>
 * Decisão de projeto: os documentos de referência não deixam explícito se
 * "letras" inclui acentuação. Como {@code name} é um nome de pessoa em
 * português, usamos {@code \p{L}} (letras Unicode) em vez de {@code A-Za-z},
 * para não rejeitar nomes com acento (ex.: "José", "Núñez").
 * <p>
 * As duas regras (tamanho e conjunto de caracteres) são expostas
 * separadamente para permitir o mesmo padrão de checklist usado em
 * {@link UsernameValidator} e {@link PasswordValidator}.
 */
public final class NameValidator {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 60;
    private static final Pattern CHARSET_PATTERN = Pattern.compile("^[\\p{L} ]*$");

    private NameValidator() {
    }

    /** Retorna {@code true} se {@code name} atende a todas as regras de formato. */
    public static boolean isValid(String name) {
        return hasValidLength(name) && hasOnlyAllowedCharacters(name);
    }

    /** Retorna {@code true} se o tamanho de {@code name} está entre 1 e 60 caracteres. */
    public static boolean hasValidLength(String name) {
        return name != null && name.length() >= MIN_LENGTH && name.length() <= MAX_LENGTH;
    }

    /** Retorna {@code true} se {@code name} só contém letras e espaços (e não é vazio). */
    public static boolean hasOnlyAllowedCharacters(String name) {
        return name != null && !name.isEmpty() && CHARSET_PATTERN.matcher(name).matches();
    }
}
