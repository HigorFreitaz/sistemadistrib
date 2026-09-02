package br.edu.utfpr.sd.garagem.common.validation;

import java.util.regex.Pattern;

/**
 * Valida o formato de {@code password} conforme os requisitos não
 * funcionais: letras maiúsculas, minúsculas, números e os símbolos
 * {@code # . * & % $ @ ! ( ) - _ = +}. Nenhum outro caractere é aceito
 * (sem espaços, sem acentuação, sem símbolos fora dessa lista).
 * <p>
 * Suposição a validar com a turma: os documentos de referência não definem
 * tamanho mínimo/máximo nem exigência de conter obrigatoriamente todas as
 * categorias de caractere — validamos aqui apenas o conjunto de caracteres
 * permitido e a obrigatoriedade de não ser vazio/nulo.
 */
public final class PasswordValidator {

    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9#.*&%$@!()\\-_=+]+$");

    private PasswordValidator() {
    }

    /** Retorna {@code true} se {@code password} atende a todas as regras de formato. */
    public static boolean isValid(String password) {
        return password != null && !password.isEmpty() && PATTERN.matcher(password).matches();
    }
}
