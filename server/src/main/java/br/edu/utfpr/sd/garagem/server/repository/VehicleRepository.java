package br.edu.utfpr.sd.garagem.server.repository;

import br.edu.utfpr.sd.garagem.common.model.Veiculo;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository {
    boolean save(Veiculo veiculo);
    Optional<Veiculo> findByLicensePlate(String licensePlate);
    List<Veiculo> findByOwner(String ownerUsername);
    boolean update(Veiculo veiculo);
    boolean deleteByLicensePlate(String licensePlate);
}
