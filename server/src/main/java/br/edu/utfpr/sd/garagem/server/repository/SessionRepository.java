package br.edu.utfpr.sd.garagem.server.repository;

import br.edu.utfpr.sd.garagem.common.model.Session;

import java.util.Collection;
import java.util.Map;

/** Acesso à base de sessões ativas, persistida em arquivo. */
public interface SessionRepository {

    /** Carrega todas as sessões persistidas, indexadas por token. */
    Map<String, Session> loadAll();

    /** Substitui o conteúdo persistido pelo conjunto de sessões informado. */
    void saveAll(Collection<Session> sessions);
}
