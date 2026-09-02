package br.edu.utfpr.sd.garagem.server.repository;

import br.edu.utfpr.sd.garagem.common.json.JsonSupport;
import br.edu.utfpr.sd.garagem.common.model.Session;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositório de sessões persistido em um arquivo JSON, com escrita
 * atômica. A cada login ou logout, o conjunto completo de sessões ativas é
 * regravado no arquivo.
 */
public final class JsonSessionRepository implements SessionRepository {

    private final Path file;

    public JsonSessionRepository(Path file) {
        this.file = file;
    }

    @Override
    public Map<String, Session> loadAll() {
        try {
            Optional<String> content = AtomicJsonFileStore.read(file);
            Map<String, Session> result = new ConcurrentHashMap<>();
            if (content.isEmpty()) {
                return result;
            }
            SessionsFile sessionsFile = JsonSupport.GSON.fromJson(content.get(), SessionsFile.class);
            if (sessionsFile != null && sessionsFile.getSessions() != null) {
                sessionsFile.getSessions().forEach(session -> result.put(session.getToken(), session));
            }
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("nao foi possivel carregar as sessoes em " + file, e);
        }
    }

    @Override
    public void saveAll(Collection<Session> sessions) {
        try {
            SessionsFile sessionsFile = new SessionsFile(new ArrayList<>(sessions));
            AtomicJsonFileStore.writeAtomic(file, JsonSupport.GSON.toJson(sessionsFile));
        } catch (IOException e) {
            throw new IllegalStateException("nao foi possivel gravar as sessoes em " + file, e);
        }
    }
}
