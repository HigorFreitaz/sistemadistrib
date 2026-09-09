package br.edu.utfpr.sd.garagem.server.service;

import br.edu.utfpr.sd.garagem.server.repository.JsonSessionRepository;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionServiceTest {

    @Test
    void logoutAllRemoveTodasAsSessoesAtivas() throws Exception {
        Path dir = Files.createTempDirectory("sdgaragem-sessionservice");
        SessionService sessions = new SessionService(new JsonSessionRepository(dir.resolve("sessoes.json")));
        sessions.login("usuario1");
        sessions.login("usuario2");
        assertEquals(2, sessions.activeSessionCount());

        sessions.logoutAll();

        assertEquals(0, sessions.activeSessionCount());
        assertTrue(sessions.findByToken("qualquer-token").isEmpty());
    }
}
