package br.edu.utfpr.sd.garagem.server.log;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LogMaskingTest {

    @Test
    void mascaraValorDaSenha() {
        String linha = "{\"method\":\"login\",\"data\":{\"username\":\"admin\",\"password\":\"Admin@123\"}}";
        String mascarada = LogMasking.maskPassword(linha);
        assertFalse(mascarada.contains("Admin@123"));
        assertEquals("{\"method\":\"login\",\"data\":{\"username\":\"admin\",\"password\":\"***\"}}", mascarada);
    }

    @Test
    void naoAlteraLinhaSemSenha() {
        String linha = "{\"statusCode\":200,\"message\":\"ok\",\"data\":{\"token\":\"abc\"}}";
        assertEquals(linha, LogMasking.maskPassword(linha));
    }

    @Test
    void mascaraSenhaAntigaENovaNaTrocaDeSenha() {
        String linha = "{\"method\":\"updateuserpassword\",\"data\":{\"token\":\"abc\",\"username\":\"admin\","
                + "\"oldPassword\":\"Antiga@123\",\"newPassword\":\"Nova@1234\"}}";
        String mascarada = LogMasking.maskPassword(linha);
        assertFalse(mascarada.contains("Antiga@123"));
        assertFalse(mascarada.contains("Nova@1234"));
        assertEquals("{\"method\":\"updateuserpassword\",\"data\":{\"token\":\"abc\",\"username\":\"admin\","
                + "\"oldPassword\":\"***\",\"newPassword\":\"***\"}}", mascarada);
    }
}
