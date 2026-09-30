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
        User admin = new User("Administrador", "admin", PasswordHasher.hash(DEFAULT_ADMIN_PASSWORD), UserRole.ADMIN);
        usersByUsername.put(admin.getUsername(), admin);
        persist();
        LOGGER.warning(() -> "Base de usuarios criada com o usuario administrador padrao 'admin' / senha '"
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

    /**
     * Substituição e persistência precisam ser atômicas entre si (mesmo
     * motivo de {@link #save}), para não perder uma escrita concorrente.
     */
    @Override
    public synchronized boolean updateName(String username, String name) {
        User existing = usersByUsername.get(username);
        if (existing == null) {
            return false;
        }
        User updated = new User(name, existing.getUsername(), existing.getPasswordHash(), existing.getRole());
        return replace(existing, updated);
    }

    @Override
    public synchronized boolean updatePasswordHash(String username, String passwordHash) {
        User existing = usersByUsername.get(username);
        if (existing == null) {
            return false;
        }
        User updated = new User(existing.getName(), existing.getUsername(), passwordHash, existing.getRole());
        return replace(existing, updated);
    }

    private boolean replace(User existing, User updated) {
        usersByUsername.put(updated.getUsername(), updated);
        try {
            persist();
            return true;
        } catch (IOException e) {
            usersByUsername.put(existing.getUsername(), existing);
            throw new IllegalStateException("nao foi possivel atualizar o usuario " + existing.getUsername(), e);
        }
    }

    @Override
    public synchronized boolean deleteByUsername(String username) {
        User removed = usersByUsername.remove(username);
        if (removed == null) {
            return false;
        }
        try {
            persist();
            return true;
        } catch (IOException e) {
            usersByUsername.put(username, removed);
            throw new IllegalStateException("nao foi possivel remover o usuario " + username, e);
        }
    }
}
