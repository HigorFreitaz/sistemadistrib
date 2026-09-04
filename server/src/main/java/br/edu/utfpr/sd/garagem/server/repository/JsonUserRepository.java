package br.edu.utfpr.sd.garagem.server.repository;

import br.edu.utfpr.sd.garagem.common.json.JsonSupport;
import br.edu.utfpr.sd.garagem.common.model.User;
import br.edu.utfpr.sd.garagem.common.model.UserRole;
import br.edu.utfpr.sd.garagem.server.security.PasswordHasher;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Repositório de usuários persistido em um arquivo JSON, com escrita
 * atômica. Se a base ainda não existir, é criada com um usuário
 * administrador padrão (ver {@link #seedAdmin()}).
 */
public final class JsonUserRepository implements UserRepository {

    private static final Logger LOGGER = Logger.getLogger(JsonUserRepository.class.getName());
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@123";

    private final Path file;
    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();

    public JsonUserRepository(Path file) {
        this.file = file;
        load();
    }

    private void load() {
        try {
            Optional<String> content = AtomicJsonFileStore.read(file);
            if (content.isEmpty()) {
                seedAdmin();
                return;
            }
            UsersFile usersFile = JsonSupport.GSON.fromJson(content.get(), UsersFile.class);
            if (usersFile != null && usersFile.getUsers() != null) {
                usersFile.getUsers().forEach(user -> usersByUsername.put(user.getUsername(), user));
            }
        } catch (IOException e) {
            throw new IllegalStateException("nao foi possivel carregar a base de usuarios em " + file, e);
        }
    }

    private void seedAdmin() throws IOException {
        User admin = new User("admin", PasswordHasher.hash(DEFAULT_ADMIN_PASSWORD), UserRole.ADMIN);
        usersByUsername.put(admin.getUsername(), admin);
        persist();
        LOGGER.warning(() -> "base de usuarios criada com o usuario administrador padrao 'admin' / senha '"
                + DEFAULT_ADMIN_PASSWORD + "' -- TROQUE ESSA SENHA");
    }

    private void persist() throws IOException {
        UsersFile usersFile = new UsersFile(new ArrayList<>(usersByUsername.values()));
        AtomicJsonFileStore.writeAtomic(file, JsonSupport.GSON.toJson(usersFile));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(username));
    }

    /**
     * Verificação de existência e inserção precisam ser atômicas entre si
     * (região crítica), senão duas requisições de cadastro concorrentes
     * para o mesmo username poderiam passar as duas.
     */
    @Override
    public synchronized boolean save(User user) {
        if (usersByUsername.containsKey(user.getUsername())) {
            return false;
        }
        usersByUsername.put(user.getUsername(), user);
        try {
            persist();
            return true;
        } catch (IOException e) {
            usersByUsername.remove(user.getUsername());
            throw new IllegalStateException("nao foi possivel salvar o usuario " + user.getUsername(), e);
        }
    }
}
