package br.edu.utfpr.sd.garagem.server.repository;

import br.edu.utfpr.sd.garagem.common.model.Ticket;

import java.util.List;
import java.util.Optional;

public interface TicketRepository {
    void save(Ticket ticket);
    Optional<Ticket> findActiveByLicensePlate(String licensePlate);
    boolean update(Ticket ticket);
    List<Ticket> findAll();
}
