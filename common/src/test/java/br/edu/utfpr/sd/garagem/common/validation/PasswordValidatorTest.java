package br.edu.utfpr.sd.garagem.common.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "Senha123",
            "abcXYZ12",
            "12345678",
            "Admin@123",
            "#.*&%$@!()-_=+AB",
            "aaaaaaaaaaaaaaaaaaaa"
    })
    void aceitaSenhasValidas(String password) {
        assertTrue(PasswordValidator.isValid(password));
    }

    @Test
    void rejeitaNulo() {
        assertFalse(PasswordValidator.isValid(null));
    }

    @Test
    void rejeitaVazia() {
        assertFalse(PasswordValidator.isValid(""));
    }

    @Test
    void rejeitaSeteCaracteres() {
        assertFalse(PasswordValidator.isValid("Senha12"));
    }

    @Test
    void aceitaOitoCaracteres() {
        assertTrue(PasswordValidator.isValid("Senha123"));
    }

    @Test
    void aceitaVinteCaracteres() {
        assertTrue(PasswordValidator.isValid("a".repeat(20)));
    }

    @Test
    void rejeitaVinteEUmCaracteres() {
        assertFalse(PasswordValidator.isValid("a".repeat(21)));
    }

    @Test
    void rejeitaEspaco() {
        assertFalse(PasswordValidator.isValid("Senha 123"));
    }

    @Test
    void rejeitaAcentuacao() {
        assertFalse(PasswordValidator.isValid("Senhaç123"));
    }

    @Test
    void rejeitaSimboloNaoPermitido() {
        assertFalse(PasswordValidator.isValid("Senha123~"));
    }

    @Test
    void rejeitaSimboloNaoPermitidoBarra() {
        assertFalse(PasswordValidator.isValid("Senha123/"));
    }

    @Test
    void hasValidLengthIgnoraCaracteresInvalidos() {
        assertTrue(PasswordValidator.hasValidLength("~~~~~~~~"));
        assertFalse(PasswordValidator.hasValidLength("~~~~~~"));
    }

    @Test
    void hasOnlyAllowedCharactersIgnoraTamanho() {
        assertTrue(PasswordValidator.hasOnlyAllowedCharacters("Ab1"));
        assertFalse(PasswordValidator.hasOnlyAllowedCharacters("Ab1~"));
        assertFalse(PasswordValidator.hasOnlyAllowedCharacters(""));
    }
}
