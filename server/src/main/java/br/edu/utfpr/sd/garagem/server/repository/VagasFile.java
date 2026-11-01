package br.edu.utfpr.sd.garagem.server.repository;

import br.edu.utfpr.sd.garagem.common.model.Vaga;

import java.util.List;

final class VagasFile {

    private List<Vaga> vagas;

    VagasFile() {}

    VagasFile(List<Vaga> vagas) { this.vagas = vagas; }

    List<Vaga> getVagas() { return vagas; }
}
