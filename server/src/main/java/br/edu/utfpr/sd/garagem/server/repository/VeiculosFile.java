package br.edu.utfpr.sd.garagem.server.repository;

import br.edu.utfpr.sd.garagem.common.model.Veiculo;

import java.util.List;

final class VeiculosFile {

    private List<Veiculo> veiculos;

    VeiculosFile() {}

    VeiculosFile(List<Veiculo> veiculos) { this.veiculos = veiculos; }

    List<Veiculo> getVeiculos() { return veiculos; }
}
