package br.edu.utfpr.sd.garagem.server.repository;

import br.edu.utfpr.sd.garagem.common.json.JsonSupport;
import br.edu.utfpr.sd.garagem.common.model.Ticket;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public final class JsonTicketRepository implements TicketRepository {

    private final Path file;
    private final List<Ticket> tickets = new CopyOnWriteArrayList<>();

    public JsonTicketRepository(Path file) {
        this.file = file;
        load();
    }

    private void load() {
        try {
            Optional<String> content = AtomicJsonFileStore.read(file);
            if (content.isEmpty()) return;
            TicketsFile f = JsonSupport.GSON.fromJson(content.get(), TicketsFile.class);
            if (f != null && f.getTickets() != null) {
                tickets.addAll(f.getTickets());
            }
        } catch (IOException e) {
            throw new IllegalStateException("nao foi possivel carregar tickets de " + file, e);
        }
    }

    private synchronized void persist() throws IOException {
        AtomicJsonFileStore.writeAtomic(file, JsonSupport.GSON.toJson(new TicketsFile(new ArrayList<>(tickets))));
    }

    @Override
    public synchronized void save(Ticket ticket) {
        tickets.add(ticket);
        try { persist(); }
        catch (IOException e) {
            tickets.remove(ticket);
            throw new IllegalStateException("falha ao salvar ticket", e);
        }
    }

    @Override
    public Optional<Ticket> findActiveByLicensePlate(String licensePlate) {
        return tickets.stream()
                .filter(t -> t.isActive() && licensePlate.equals(t.getLicensePlate()))
                .findFirst();
    }

    @Override
    public synchronized boolean update(Ticket ticket) {
        for (int i = 0; i < tickets.size(); i++) {
            if (tickets.get(i).getTicketId().equals(ticket.getTicketId())) {
                tickets.set(i, ticket);
                try { persist(); return true; }
                catch (IOException e) { throw new IllegalStateException("falha ao atualizar ticket", e); }
            }
        }
        return false;
    }

    @Override
    public List<Ticket> findAll() {
        return new ArrayList<>(tickets);
    }
}
