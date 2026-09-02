package br.edu.utfpr.sd.garagem.server.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * Leitura/escrita de arquivos JSON de forma atômica: grava em um arquivo
 * temporário e move com {@code ATOMIC_MOVE}, para nunca deixar a base
 * corrompida caso o processo caia no meio da escrita.
 */
public final class AtomicJsonFileStore {

    private AtomicJsonFileStore() {
    }

    /** Lê o conteúdo do arquivo, se ele existir. */
    public static Optional<String> read(Path file) throws IOException {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        return Optional.of(Files.readString(file, StandardCharsets.UTF_8));
    }

    /** Grava o conteúdo no arquivo de forma atômica. */
    public static void writeAtomic(Path file, String content) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path tmp = Files.createTempFile(parent, file.getFileName().toString(), ".tmp");
        try {
            Files.writeString(tmp, content, StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
