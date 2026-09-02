package br.edu.utfpr.sd.garagem.server.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void senhaCorretaValidaContraOHash() {
        String hash = PasswordHasher.hash("Admin@123");
        assertTrue(PasswordHasher.matches("Admin@123", hash));
    }

    @Test
    void senhaIncorretaNaoValidaContraOHash() {
        String hash = PasswordHasher.hash("Admin@123");
        assertFalse(PasswordHasher.matches("outraSenha", hash));
    }

    @Test
    void hashesDaMesmaSenhaSaoDiferentesPorCausaDoSaltAleatorio() {
        String hash1 = PasswordHasher.hash("Admin@123");
        String hash2 = PasswordHasher.hash("Admin@123");
        assertNotEquals(hash1, hash2);
    }
}
