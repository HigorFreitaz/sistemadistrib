package br.edu.utfpr.sd.garagem.server.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuração de porta do servidor: lida de {@code server.properties} no
 * diretório de trabalho, com valor padrão 5555, sobrescrevível pelo primeiro
 * argumento de linha de comando.
 */
public final class ServerProperties {

    private static final Logger LOGGER = Logger.getLogger(ServerProperties.class.getName());
    private static final int DEFAULT_PORT = 5555;
    private static final String FILE_NAME = "server.properties";

    private final int port;

    private ServerProperties(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    /** Resolve a porta a partir do arquivo de propriedades e, por fim, dos argumentos de linha de comando. */
    public static ServerProperties load(String[] args) {
        int port = readFromFile().orElse(DEFAULT_PORT);
        port = readFromArgs(args).orElse(port);
        return new ServerProperties(port);
    }

    private static Optional<Integer> readFromFile() {
        Path path = Path.of(FILE_NAME);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            properties.load(in);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "nao foi possivel ler " + FILE_NAME + ", usando porta padrao", e);
            return Optional.empty();
        }
        return parsePort(properties.getProperty("port"));
    }

    private static Optional<Integer> readFromArgs(String[] args) {
        if (args == null || args.length == 0) {
            return Optional.empty();
        }
        return parsePort(args[0]);
    }

    private static Optional<Integer> parsePort(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            LOGGER.warning(() -> "valor de porta invalido ignorado: " + value);
            return Optional.empty();
        }
    }
}
