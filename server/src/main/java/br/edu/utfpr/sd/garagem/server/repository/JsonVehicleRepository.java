package br.edu.utfpr.sd.garagem.server.repository;

import br.edu.utfpr.sd.garagem.common.json.JsonSupport;
import br.edu.utfpr.sd.garagem.common.model.Veiculo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class JsonVehicleRepository implements VehicleRepository {

    private final Path file;
    private final Map<String, Veiculo> veiculosByPlate = new ConcurrentHashMap<>();

    public JsonVehicleRepository(Path file) {
        this.file = file;
        load();
    }

    private void load() {
        try {
            Optional<String> content = AtomicJsonFileStore.read(file);
            if (content.isEmpty()) return;
            VeiculosFile f = JsonSupport.GSON.fromJson(content.get(), VeiculosFile.class);
            if (f != null && f.getVeiculos() != null) {
                f.getVeiculos().forEach(v -> veiculosByPlate.put(v.getLicensePlate(), v));
            }
        } catch (IOException e) {
            throw new IllegalStateException("nao foi possivel carregar veiculos de " + file, e);
        }
    }

    private void persist() throws IOException {
        VeiculosFile f = new VeiculosFile(new ArrayList<>(veiculosByPlate.values()));
        AtomicJsonFileStore.writeAtomic(file, JsonSupport.GSON.toJson(f));
    }

    @Override
    public synchronized boolean save(Veiculo veiculo) {
        if (veiculosByPlate.containsKey(veiculo.getLicensePlate())) return false;
        veiculosByPlate.put(veiculo.getLicensePlate(), veiculo);
        try { persist(); return true; }
        catch (IOException e) {
            veiculosByPlate.remove(veiculo.getLicensePlate());
            throw new IllegalStateException("falha ao salvar veiculo", e);
        }
    }

    @Override
    public Optional<Veiculo> findByLicensePlate(String licensePlate) {
        return Optional.ofNullable(veiculosByPlate.get(licensePlate));
    }

    @Override
    public List<Veiculo> findByOwner(String ownerUsername) {
        return veiculosByPlate.values().stream()
                .filter(v -> ownerUsername.equals(v.getOwnerUsername()))
                .collect(Collectors.toList());
    }

    @Override
    public synchronized boolean update(Veiculo veiculo) {
        if (!veiculosByPlate.containsKey(veiculo.getLicensePlate())) return false;
        veiculosByPlate.put(veiculo.getLicensePlate(), veiculo);
        try { persist(); return true; }
        catch (IOException e) { throw new IllegalStateException("falha ao atualizar veiculo", e); }
    }

    @Override
    public synchronized boolean deleteByLicensePlate(String licensePlate) {
        Veiculo removed = veiculosByPlate.remove(licensePlate);
        if (removed == null) return false;
        try { persist(); return true; }
        catch (IOException e) {
            veiculosByPlate.put(licensePlate, removed);
            throw new IllegalStateException("falha ao deletar veiculo", e);
        }
    }
}
