package br.edu.utfpr.sd.garagem.server.repository;

import br.edu.utfpr.sd.garagem.common.json.JsonSupport;
import br.edu.utfpr.sd.garagem.common.model.Vaga;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class JsonVagaRepository implements VagaRepository {

    private final Path file;
    private final Map<String, Vaga> vagasBySpotName = new ConcurrentHashMap<>();

    public JsonVagaRepository(Path file) {
        this.file = file;
        load();
    }

    private void load() {
        try {
            Optional<String> content = AtomicJsonFileStore.read(file);
            if (content.isEmpty()) return;
            VagasFile f = JsonSupport.GSON.fromJson(content.get(), VagasFile.class);
            if (f != null && f.getVagas() != null) {
                f.getVagas().forEach(v -> vagasBySpotName.put(v.getSpotName(), v));
            }
        } catch (IOException e) {
            throw new IllegalStateException("nao foi possivel carregar vagas de " + file, e);
        }
    }

    private void persist() throws IOException {
        VagasFile f = new VagasFile(new ArrayList<>(vagasBySpotName.values()));
        AtomicJsonFileStore.writeAtomic(file, JsonSupport.GSON.toJson(f));
    }

    @Override
    public synchronized boolean save(Vaga vaga) {
        if (vagasBySpotName.containsKey(vaga.getSpotName())) return false;
        vagasBySpotName.put(vaga.getSpotName(), vaga);
        try { persist(); return true; }
        catch (IOException e) {
            vagasBySpotName.remove(vaga.getSpotName());
            throw new IllegalStateException("falha ao salvar vaga", e);
        }
    }

    @Override
    public Optional<Vaga> findBySpotName(String spotName) {
        return Optional.ofNullable(vagasBySpotName.get(spotName));
    }

    @Override
    public List<Vaga> findAll() {
        return new ArrayList<>(vagasBySpotName.values());
    }

    @Override
    public synchronized boolean update(Vaga vaga) {
        if (!vagasBySpotName.containsKey(vaga.getSpotName())) return false;
        vagasBySpotName.put(vaga.getSpotName(), vaga);
        try { persist(); return true; }
        catch (IOException e) { throw new IllegalStateException("falha ao atualizar vaga", e); }
    }

    @Override
    public synchronized boolean deleteBySpotName(String spotName) {
        Vaga removed = vagasBySpotName.remove(spotName);
        if (removed == null) return false;
        try { persist(); return true; }
        catch (IOException e) {
            vagasBySpotName.put(spotName, removed);
            throw new IllegalStateException("falha ao deletar vaga", e);
        }
    }
}
