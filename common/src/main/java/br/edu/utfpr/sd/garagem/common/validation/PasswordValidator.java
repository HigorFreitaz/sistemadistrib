package br.edu.utfpr.sd.garagem.common.validation;

import java.util.regex.Pattern;

/**
 * Valida o formato de {@code password} conforme os requisitos não
 * funcionais: letras maiúsculas, minúsculas, números e os símbolos
 * {@code # . * & % $ @ ! ( ) - _ = +}, mínimo 8 e máximo 20 caracteres.
 * Nenhum outro caractere é aceito (sem espaços, sem acentuação, sem
 * símbolos fora dessa lista).
 * <p>
 * Suposição a validar com a turma: os documentos de referência não exigem
 * obrigatoriamente que todas as categorias de caractere (maiúscula,
 * minúscula, número, símbolo) estejam presentes — validamos apenas o
 * conjunto de caracteres permitido e o tamanho.
 */
public final class PasswordValidator {

    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9#.*&%$@!()\\-_=+]{8,20}$");

    private PasswordValidator() {
    }

    /** Retorna {@code true} se {@code password} atende a todas as regras de formato. */
    public static boolean isValid(String password) {
        return password != null && PATTERN.matcher(password).matches();
    }
}
