package br.edu.utfpr.sd.garagem.server.repository;

import br.edu.utfpr.sd.garagem.common.model.Session;

import java.util.List;

/** Estrutura raiz do arquivo {@code sessoes.json}. */
final class SessionsFile {

    private List<Session> sessions;

    SessionsFile() {
    }

    SessionsFile(List<Session> sessions) {
        this.sessions = sessions;
    }

    List<Session> getSessions() {
        return sessions;
    }
}
