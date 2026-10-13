package br.edu.utfpr.sd.garagem.server.net;

import br.edu.utfpr.sd.garagem.common.json.JsonSupport;
import br.edu.utfpr.sd.garagem.common.model.Session;
import br.edu.utfpr.sd.garagem.common.model.User;
import br.edu.utfpr.sd.garagem.common.protocol.DeleteUserRequest;
import br.edu.utfpr.sd.garagem.common.protocol.GetUserRequest;
import br.edu.utfpr.sd.garagem.common.protocol.LoginRequest;
import br.edu.utfpr.sd.garagem.common.protocol.LogoutRequest;
import br.edu.utfpr.sd.garagem.common.protocol.Methods;
import br.edu.utfpr.sd.garagem.common.protocol.RegisterRequest;
import br.edu.utfpr.sd.garagem.common.protocol.Response;
import br.edu.utfpr.sd.garagem.common.protocol.StatusCode;
import br.edu.utfpr.sd.garagem.common.protocol.TokenData;
import br.edu.utfpr.sd.garagem.common.protocol.UpdateUserNameRequest;
import br.edu.utfpr.sd.garagem.common.protocol.UpdateUserPasswordRequest;
import br.edu.utfpr.sd.garagem.common.protocol.UserData;
import br.edu.utfpr.sd.garagem.common.validation.NameValidator;
import br.edu.utfpr.sd.garagem.common.validation.PasswordValidator;
import br.edu.utfpr.sd.garagem.common.validation.UsernameValidator;
import br.edu.utfpr.sd.garagem.server.service.AuthService;
import br.edu.utfpr.sd.garagem.server.service.LoginResult;
import br.edu.utfpr.sd.garagem.server.service.UpdatePasswordResult;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Optional;

/**
 * Interpreta cada linha JSON recebida de um cliente e produz a resposta do
 * protocolo, roteando por {@code method}. Único ponto do servidor que
 * conhece o formato das mensagens de cada operação.
 */
public final class RequestDispatcher {

    private final AuthService authService;
    private final ServerEventListener listener;

    public RequestDispatcher(AuthService authService, ServerEventListener listener) {
        this.authService = authService;
        this.listener = listener;
    }

    /** Processa uma linha recebida do cliente e devolve a resposta a ser enviada. */
    public Response dispatch(String rawLine) {
        JsonObject envelope;
        try {
            envelope = JsonParser.parseString(rawLine).getAsJsonObject();
        } catch (RuntimeException e) {
            return Response.error(StatusCode.BAD_REQUEST, "Requisicao malformada");
        }
        JsonElement methodElement = envelope.get("method");
        if (methodElement == null || !methodElement.isJsonPrimitive()) {
            return Response.error(StatusCode.BAD_REQUEST, "Campo method ausente");
        }
        String method = methodElement.getAsString();
        return switch (method) {
            case Methods.LOGIN -> handleLogin(envelope);
            case Methods.LOGOUT -> handleLogout(envelope);
            case Methods.REGISTER -> handleRegister(envelope);
            case Methods.GET_USER -> handleGetUser(envelope);
            case Methods.UPDATE_USER_NAME -> handleUpdateUserName(envelope);
            case Methods.UPDATE_USER_PASSWORD -> handleUpdateUserPassword(envelope);
            case Methods.DELETE_USER -> handleDeleteUser(envelope);
            // TODO EP-2: registrar aqui os demais methods do protocolo (CRUD
            // de vagas/operacoes, CRUD admin), cada um delegando a um
            // servico proprio e reaproveitando authService.resolveSession
            // para autenticar a requisicao.
            default -> Response.error(StatusCode.NOT_FOUND, "Operacao nao suportada");
        };
    }

    private Response handleLogin(JsonObject envelope) {
        LoginRequest request = JsonSupport.GSON.fromJson(envelope, LoginRequest.class);
        if (!credenciaisValidas(request.getUsername(), request.getPassword())) {
            return Response.error(StatusCode.BAD_REQUEST, "Usuario ou senhas inválidos");
        }
        LoginResult result = authService.login(request.getUsername(), request.getPassword());
        return switch (result.getStatus()) {
            case SUCCESS -> onLoginSuccess(result.getSession());
            case USER_NOT_FOUND, WRONG_PASSWORD -> Response.error(StatusCode.UNAUTHORIZED, "Usuario ou senhas inválidos");
        };
    }

    private Response onLoginSuccess(Session session) {
        listener.onSessionCountChanged(authService.activeSessionCount());
        return Response.ok("Sucesso no Login", new TokenData(session.getToken()));
    }

    private Response handleLogout(JsonObject envelope) {
        LogoutRequest request = JsonSupport.GSON.fromJson(envelope, LogoutRequest.class);
        if (request.getToken() == null || request.getToken().isBlank()) {
            return Response.error(StatusCode.BAD_REQUEST, "Token de autenticação não fornecido.");
        }
        authService.logout(request.getToken());
        listener.onSessionCountChanged(authService.activeSessionCount());
        return Response.ok("Usuário deslogado com sucesso", null);
    }

    private Response handleRegister(JsonObject envelope) {
        RegisterRequest request = JsonSupport.GSON.fromJson(envelope, RegisterRequest.class);
        Response invalido = validarCadastro(request.getName(), request.getUsername(), request.getPassword());
        if (invalido != null) {
            return invalido;
        }
        boolean created = authService.register(request.getName(), request.getUsername(), request.getPassword());
        return created
                ? Response.created("Usuário criado com sucesso", null)
                : Response.error(StatusCode.CONFLICT, "O username já está em uso.");
    }

    private Response handleGetUser(JsonObject envelope) {
        GetUserRequest request = JsonSupport.GSON.fromJson(envelope, GetUserRequest.class);
        Response semAcesso = verificarAcesso(request.getToken(), request.getUsername());
        if (semAcesso != null) {
            return semAcesso;
        }
        Optional<User> user = authService.getUserData(request.getUsername());
        if (user.isEmpty()) {
            return Response.error(StatusCode.NOT_FOUND, "Usuário não encontrado");
        }
        return Response.ok("Usuário encontrado com sucesso",
                new UserData(user.get().getName(), user.get().getUsername()));
    }

    private Response handleUpdateUserName(JsonObject envelope) {
        UpdateUserNameRequest request = JsonSupport.GSON.fromJson(envelope, UpdateUserNameRequest.class);
        Response semAcesso = verificarAcesso(request.getToken(), request.getUsername());
        if (semAcesso != null) {
            return semAcesso;
        }
        if (!NameValidator.isValid(request.getName())) {
            return Response.error(StatusCode.BAD_REQUEST, "Nome do usuário fora do padrão");
        }
        boolean updated = authService.updateName(request.getUsername(), request.getName());
        return updated
                ? Response.ok("Nome do usuário atualizado com sucesso", null)
                : Response.error(StatusCode.NOT_FOUND, "Usuário não encontrado");
    }

    private Response handleUpdateUserPassword(JsonObject envelope) {
        UpdateUserPasswordRequest request = JsonSupport.GSON.fromJson(envelope, UpdateUserPasswordRequest.class);
        Response semAcesso = verificarAcesso(request.getToken(), request.getUsername());
        if (semAcesso != null) {
            return semAcesso;
        }
        if (!PasswordValidator.isValid(request.getNewPassword())) {
            return Response.error(StatusCode.BAD_REQUEST, "Senha fora do padrão");
        }
        UpdatePasswordResult result = authService.updatePassword(
                request.getUsername(), request.getOldPassword(), request.getNewPassword());
        return switch (result.getStatus()) {
            case SUCCESS -> Response.ok("Senha atualizada com sucesso", null);
            case WRONG_OLD_PASSWORD -> Response.error(StatusCode.UNAUTHORIZED, "Senha atual incorreta");
            case USER_NOT_FOUND -> Response.error(StatusCode.NOT_FOUND, "Usuário não encontrado");
        };
    }

    private Response handleDeleteUser(JsonObject envelope) {
        DeleteUserRequest request = JsonSupport.GSON.fromJson(envelope, DeleteUserRequest.class);
        if (request.getToken() == null || request.getToken().isBlank()) {
            return Response.error(StatusCode.BAD_REQUEST, "Token de autenticação não fornecido.");
        }
        Optional<Session> session = authService.resolveSession(request.getToken());
        if (session.isEmpty()) {
            return Response.error(StatusCode.UNAUTHORIZED, "Sessão expirada ou encerrada.");
        }
        if (!session.get().getUsername().equals(request.getUsername())) {
            return Response.error(StatusCode.UNAUTHORIZED, "Operação não autorizada");
        }
        boolean deleted = authService.deleteUser(request.getUsername(), request.getToken());
        listener.onSessionCountChanged(authService.activeSessionCount());
        return deleted
                ? Response.ok("Usuário deletado com sucesso", null)
                : Response.error(StatusCode.NOT_FOUND, "Usuário não encontrado");
    }

    /**
     * Resolve a sessão do token e confere que ela pertence ao
     * {@code username} informado, comum às quatro operações autenticadas
     * sobre o próprio cadastro. Devolve {@code null} quando o acesso é
     * válido; caso contrário, já devolve a resposta de erro pronta (401
     * para token ausente/inválido/expirado, 403 para token válido mas de
     * outro usuário).
     */
    private Response verificarAcesso(String token, String username) {
        if (token == null || token.isBlank()) {
            return Response.error(StatusCode.BAD_REQUEST, "Token de autenticação não fornecido.");
        }
        Optional<Session> session = authService.resolveSession(token);
        if (session.isEmpty()) {
            return Response.error(StatusCode.UNAUTHORIZED, "Sessão expirada ou encerrada.");
        }
        if (!session.get().getUsername().equals(username)) {
            return Response.error(StatusCode.FORBIDDEN, "Sessão expirada ou encerrada.");
        }
        return null;
    }

    private Response validarCadastro(String name, String username, String password) {
        if (!NameValidator.isValid(name)) {
            return Response.error(StatusCode.BAD_REQUEST, "O campo name não está no padrão esperado.");
        }
        if (!UsernameValidator.isValid(username)) {
            return Response.error(StatusCode.BAD_REQUEST, "O campo username não está no padrão esperado.");
        }
        if (!PasswordValidator.isValid(password)) {
            return Response.error(StatusCode.BAD_REQUEST, "O campo password não está no padrão esperado.");
        }
        return null;
    }

    private static boolean credenciaisValidas(String username, String password) {
        return UsernameValidator.isValid(username) && PasswordValidator.isValid(password);
    }
}
