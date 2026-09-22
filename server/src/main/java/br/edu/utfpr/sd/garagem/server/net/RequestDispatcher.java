package br.edu.utfpr.sd.garagem.server.net;

import br.edu.utfpr.sd.garagem.common.json.JsonSupport;
import br.edu.utfpr.sd.garagem.common.model.Session;
import br.edu.utfpr.sd.garagem.common.protocol.LoginRequest;
import br.edu.utfpr.sd.garagem.common.protocol.LogoutRequest;
import br.edu.utfpr.sd.garagem.common.protocol.Methods;
import br.edu.utfpr.sd.garagem.common.protocol.RegisterRequest;
import br.edu.utfpr.sd.garagem.common.protocol.Response;
import br.edu.utfpr.sd.garagem.common.protocol.StatusCode;
import br.edu.utfpr.sd.garagem.common.protocol.TokenData;
import br.edu.utfpr.sd.garagem.common.validation.PasswordValidator;
import br.edu.utfpr.sd.garagem.common.validation.UsernameValidator;
import br.edu.utfpr.sd.garagem.server.service.AuthService;
import br.edu.utfpr.sd.garagem.server.service.LoginResult;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
    public Response dispatch(String rawLine, ClientSession session) {
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
            case Methods.LOGIN -> handleLogin(envelope, session);
            case Methods.LOGOUT -> handleLogout(envelope, session);
            case Methods.REGISTER -> handleRegister(envelope);
            // TODO EP-2: registrar aqui os demais methods do protocolo (CRUD
            // de vagas/operacoes, CRUD admin), cada um delegando a um
            // servico proprio e reaproveitando authService.resolveSession
            // para autenticar a requisicao.
            default -> Response.error(StatusCode.NOT_FOUND, "Operacao nao suportada");
        };
    }

    private Response handleLogin(JsonObject envelope, ClientSession session) {
        LoginRequest request = JsonSupport.GSON.fromJson(envelope, LoginRequest.class);
        if (!credenciaisValidas(request.getUsername(), request.getPassword())) {
            return Response.error(StatusCode.BAD_REQUEST, "Usuario ou senha em formato invalido");
        }
        LoginResult result = authService.login(request.getUsername(), request.getPassword());
        // mensagens distintas por caso, conforme o fluxo documentado em
        // docs/Requisitos Funcionais e nao funcionais.docx (ver LoginResult)
        return switch (result.getStatus()) {
            case SUCCESS -> onLoginSuccess(result.getSession(), session);
            case USER_NOT_FOUND -> Response.error(StatusCode.UNAUTHORIZED, "Usuario nao encontrado");
            case WRONG_PASSWORD -> Response.error(StatusCode.UNAUTHORIZED, "Senha incorreta");
        };
    }

    private Response onLoginSuccess(Session session, ClientSession clientSession) {
        listener.onSessionCountChanged(authService.activeSessionCount());
        clientSession.bindToken(session.getToken());
        return Response.ok("Sucesso no Login", new TokenData(session.getToken()));
    }

    private Response handleLogout(JsonObject envelope, ClientSession session) {
        LogoutRequest request = JsonSupport.GSON.fromJson(envelope, LogoutRequest.class);
        if (request.getToken() == null || request.getToken().isBlank()) {
            return Response.error(StatusCode.BAD_REQUEST, "Token nao informado");
        }
        boolean removed = authService.logout(request.getToken());
        if (removed) {
            session.unbindToken();
            listener.onSessionCountChanged(authService.activeSessionCount());
            return Response.ok("Usuário deslogado com sucesso", null);
        }
        return Response.error(StatusCode.UNAUTHORIZED, "Token invalido ou sessao inexistente");
    }

    private Response handleRegister(JsonObject envelope) {
        RegisterRequest request = JsonSupport.GSON.fromJson(envelope, RegisterRequest.class);
        if (!credenciaisValidas(request.getUsername(), request.getPassword())) {
            return Response.error(StatusCode.BAD_REQUEST, "Usuario ou senha em formato invalido");
        }
        boolean created = authService.register(request.getUsername(), request.getPassword());
        return created
                ? Response.created("Usuário criado com sucesso", null)
                : Response.error(StatusCode.CONFLICT, "Usuario ja cadastrado");
    }

    private static boolean credenciaisValidas(String username, String password) {
        return UsernameValidator.isValid(username) && PasswordValidator.isValid(password);
    }
}
