package br.edu.utfpr.sd.garagem.server.log;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LogMaskingTest {

    @Test
    void mascaraValorDaSenha() {
        String linha = "{\"method\":\"login\",\"username\":\"admin\",\"password\":\"Admin@123\"}";
        String mascarada = LogMasking.maskPassword(linha);
        assertFalse(mascarada.contains("Admin@123"));
        assertEquals("{\"method\":\"login\",\"username\":\"admin\",\"password\":\"***\"}", mascarada);
    }

    @Test
    void naoAlteraLinhaSemSenha() {
        String linha = "{\"statusCode\":200,\"message\":\"ok\",\"data\":{\"token\":\"abc\"}}";
        assertEquals(linha, LogMasking.maskPassword(linha));
    }
}
