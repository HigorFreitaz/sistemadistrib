package br.edu.utfpr.sd.garagem.server.repository;

import br.edu.utfpr.sd.garagem.common.model.Vaga;

import java.util.List;
import java.util.Optional;

public interface VagaRepository {
    boolean save(Vaga vaga);
    Optional<Vaga> findBySpotName(String spotName);
    List<Vaga> findAll();
    boolean update(Vaga vaga);
    boolean deleteBySpotName(String spotName);
}
