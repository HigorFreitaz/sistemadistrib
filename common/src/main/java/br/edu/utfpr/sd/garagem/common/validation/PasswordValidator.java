package br.edu.utfpr.sd.garagem.common.validation;

import java.util.regex.Pattern;

/**
 * Valida o formato de {@code password} conforme os requisitos não
 * funcionais: letras maiúsculas, minúsculas, números e os símbolos
 * {@code # . * & % $ @ ! ( ) - _ = +}, mínimo 8 e máximo 20 caracteres.
 * Nenhum outro caractere é aceito (sem espaços, sem acentuação, sem
 * símbolos fora dessa lista).
 * <p>
 * As duas regras (tamanho e conjunto de caracteres) são expostas
 * separadamente para permitir uma tela de cadastro com feedback granular
 * (ex.: um checklist que vai ficando verde regra por regra).
 * <p>
 * Suposição a validar com a turma: os documentos de referência não exigem
 * obrigatoriamente que todas as categorias de caractere (maiúscula,
 * minúscula, número, símbolo) estejam presentes — validamos apenas o
 * conjunto de caracteres permitido e o tamanho.
 */
public final class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 20;
    private static final Pattern CHARSET_PATTERN = Pattern.compile("^[A-Za-z0-9#.*&%$@!()\\-_=+]*$");

    private PasswordValidator() {
    }

    /** Retorna {@code true} se {@code password} atende a todas as regras de formato. */
    public static boolean isValid(String password) {
        return hasValidLength(password) && hasOnlyAllowedCharacters(password);
    }

    /** Retorna {@code true} se o tamanho de {@code password} está entre 8 e 20 caracteres. */
    public static boolean hasValidLength(String password) {
        return password != null && password.length() >= MIN_LENGTH && password.length() <= MAX_LENGTH;
    }

    /** Retorna {@code true} se {@code password} só contém caracteres permitidos (e não é vazia). */
    public static boolean hasOnlyAllowedCharacters(String password) {
        return password != null && !password.isEmpty() && CHARSET_PATTERN.matcher(password).matches();
    }
}
