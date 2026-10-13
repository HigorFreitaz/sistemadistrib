package br.edu.utfpr.sd.garagem.common.validation;

import java.util.regex.Pattern;

/**
 * Valida o formato de {@code password}: letras maiúsculas, minúsculas,
 * números e os símbolos {@code # . * & % $ @ ! ( ) - _ = +}, mínimo 8 e
 * máximo 20 caracteres, com ao menos 1 caractere de cada categoria
 * (maiúsculo, minúsculo, número e símbolo especial).
 */
public final class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 20;
    private static final Pattern CHARSET_PATTERN = Pattern.compile("^[A-Za-z0-9#.*&%$@!()\\-_=+]*$");
    private static final Pattern HAS_UPPER   = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWER   = Pattern.compile("[a-z]");
    private static final Pattern HAS_DIGIT   = Pattern.compile("[0-9]");
    private static final Pattern HAS_SPECIAL = Pattern.compile("[#.*&%$@!()" + "\\-_=+]");

    private PasswordValidator() {
    }

    /** Retorna {@code true} se {@code password} atende a todas as regras de formato. */
    public static boolean isValid(String password) {
        return hasValidLength(password)
                && hasOnlyAllowedCharacters(password)
                && hasUpperCase(password)
                && hasLowerCase(password)
                && hasDigit(password)
                && hasSpecialChar(password);
    }

    /** Retorna {@code true} se o tamanho de {@code password} está entre 8 e 20 caracteres. */
    public static boolean hasValidLength(String password) {
        return password != null && password.length() >= MIN_LENGTH && password.length() <= MAX_LENGTH;
    }

    /** Retorna {@code true} se {@code password} só contém caracteres permitidos (e não é vazia). */
    public static boolean hasOnlyAllowedCharacters(String password) {
        return password != null && !password.isEmpty() && CHARSET_PATTERN.matcher(password).matches();
    }

    public static boolean hasUpperCase(String password) {
        return password != null && HAS_UPPER.matcher(password).find();
    }

    public static boolean hasLowerCase(String password) {
        return password != null && HAS_LOWER.matcher(password).find();
    }

    public static boolean hasDigit(String password) {
        return password != null && HAS_DIGIT.matcher(password).find();
    }

    public static boolean hasSpecialChar(String password) {
        return password != null && HAS_SPECIAL.matcher(password).find();
    }
}
