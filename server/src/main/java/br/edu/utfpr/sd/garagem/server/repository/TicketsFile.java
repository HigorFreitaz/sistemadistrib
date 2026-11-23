package br.edu.utfpr.sd.garagem.server.repository;

import br.edu.utfpr.sd.garagem.common.model.Ticket;

import java.util.List;

final class TicketsFile {

    private List<Ticket> tickets;

    TicketsFile() {}

    TicketsFile(List<Ticket> tickets) { this.tickets = tickets; }

    List<Ticket> getTickets() { return tickets; }
}
