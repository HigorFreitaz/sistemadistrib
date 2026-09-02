package br.edu.utfpr.sd.garagem.common.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsernameValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "abc",
            "a12",
            "usuario.teste_01",
            "aaaaaaaaaaaaaaaaaaaa",
            "a.b_c"
    })
    void aceitaUsernamesValidos(String username) {
        assertTrue(UsernameValidator.isValid(username));
    }

    @Test
    void rejeitaNulo() {
        assertFalse(UsernameValidator.isValid(null));
    }

    @Test
    void rejeitaVazio() {
        assertFalse(UsernameValidator.isValid(""));
    }

    @Test
    void rejeitaDoisCaracteres() {
        assertFalse(UsernameValidator.isValid("ab"));
    }

    @Test
    void aceitaTresCaracteres() {
        assertTrue(UsernameValidator.isValid("abc"));
    }

    @Test
    void aceitaVinteCaracteres() {
        assertTrue(UsernameValidator.isValid("a".repeat(20)));
    }

    @Test
    void rejeitaVinteEUmCaracteres() {
        assertFalse(UsernameValidator.isValid("a".repeat(21)));
    }

    @Test
    void rejeitaLetraMaiuscula() {
        assertFalse(UsernameValidator.isValid("Usuario"));
    }

    @Test
    void rejeitaAcentuacao() {
        assertFalse(UsernameValidator.isValid("usuário"));
    }

    @Test
    void rejeitaEspaco() {
        assertFalse(UsernameValidator.isValid("usu ario"));
    }

    @Test
    void rejeitaSimboloNaoPermitido() {
        assertFalse(UsernameValidator.isValid("usuario-01"));
    }
}
