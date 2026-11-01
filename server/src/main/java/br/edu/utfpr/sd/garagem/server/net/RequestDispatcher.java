package br.edu.utfpr.sd.garagem.server.net;

import br.edu.utfpr.sd.garagem.common.json.JsonSupport;
import br.edu.utfpr.sd.garagem.common.model.Session;
import br.edu.utfpr.sd.garagem.common.model.User;
import br.edu.utfpr.sd.garagem.common.model.UserRole;
import br.edu.utfpr.sd.garagem.common.model.Vaga;
import br.edu.utfpr.sd.garagem.common.model.Veiculo;
import br.edu.utfpr.sd.garagem.common.protocol.CreateVagaRequest;
import br.edu.utfpr.sd.garagem.common.protocol.CreateVehicleRequest;
import br.edu.utfpr.sd.garagem.common.protocol.DeleteUserRequest;
import br.edu.utfpr.sd.garagem.common.protocol.DeleteVagaRequest;
import br.edu.utfpr.sd.garagem.common.protocol.DeleteVehicleRequest;
import br.edu.utfpr.sd.garagem.common.protocol.FindVehicleData;
import br.edu.utfpr.sd.garagem.common.protocol.FindVehicleRequest;
import br.edu.utfpr.sd.garagem.common.protocol.GetUserRequest;
import br.edu.utfpr.sd.garagem.common.protocol.GetUsersRequest;
import br.edu.utfpr.sd.garagem.common.protocol.GetVagasRequest;
import br.edu.utfpr.sd.garagem.common.protocol.GetVehicleRequest;
import br.edu.utfpr.sd.garagem.common.protocol.GetVehiclesRequest;
import br.edu.utfpr.sd.garagem.common.protocol.LoginRequest;
import br.edu.utfpr.sd.garagem.common.protocol.LogoutRequest;
import br.edu.utfpr.sd.garagem.common.protocol.Methods;
import br.edu.utfpr.sd.garagem.common.protocol.RegisterRequest;
import br.edu.utfpr.sd.garagem.common.protocol.Response;
import br.edu.utfpr.sd.garagem.common.protocol.StatusCode;
import br.edu.utfpr.sd.garagem.common.protocol.TokenData;
import br.edu.utfpr.sd.garagem.common.protocol.UpdateUserNameRequest;
import br.edu.utfpr.sd.garagem.common.protocol.UpdateUserPasswordRequest;
import br.edu.utfpr.sd.garagem.common.protocol.UpdateVagaRequest;
import br.edu.utfpr.sd.garagem.common.protocol.UpdateVehicleRequest;
import br.edu.utfpr.sd.garagem.common.protocol.UserData;
import br.edu.utfpr.sd.garagem.common.protocol.VagaData;
import br.edu.utfpr.sd.garagem.common.protocol.VehicleData;
import br.edu.utfpr.sd.garagem.common.validation.NameValidator;
import br.edu.utfpr.sd.garagem.common.validation.PasswordValidator;
import br.edu.utfpr.sd.garagem.common.validation.UsernameValidator;
import br.edu.utfpr.sd.garagem.server.repository.VagaRepository;
import br.edu.utfpr.sd.garagem.server.repository.VehicleRepository;
import br.edu.utfpr.sd.garagem.server.service.AuthService;
import br.edu.utfpr.sd.garagem.server.service.LoginResult;
import br.edu.utfpr.sd.garagem.server.service.UpdatePasswordResult;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class RequestDispatcher {

    private final AuthService authService;
    private final VagaRepository vagaRepository;
    private final VehicleRepository vehicleRepository;
    private final ServerEventListener listener;

    public RequestDispatcher(AuthService authService, VagaRepository vagaRepository,
                             VehicleRepository vehicleRepository, ServerEventListener listener) {
        this.authService = authService;
        this.vagaRepository = vagaRepository;
        this.vehicleRepository = vehicleRepository;
        this.listener = listener;
    }

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
            case Methods.GET_USERS -> handleGetUsers(envelope);
            case Methods.UPDATE_USER_NAME -> handleUpdateUserName(envelope);
            case Methods.UPDATE_USER_PASSWORD -> handleUpdateUserPassword(envelope);
            case Methods.DELETE_USER -> handleDeleteUser(envelope);
            case Methods.CREATE_VAGA -> handleCreateVaga(envelope);
            case Methods.GET_VAGAS -> handleGetVagas(envelope);
            case Methods.UPDATE_VAGA -> handleUpdateVaga(envelope);
            case Methods.DELETE_VAGA -> handleDeleteVaga(envelope);
            case Methods.CREATE_VEHICLE -> handleCreateVehicle(envelope);
            case Methods.GET_VEHICLES -> handleGetVehicles(envelope);
            case Methods.GET_VEHICLE -> handleGetVehicle(envelope);
            case Methods.UPDATE_VEHICLE -> handleUpdateVehicle(envelope);
            case Methods.DELETE_VEHICLE -> handleDeleteVehicle(envelope);
            case Methods.FIND_VEHICLE -> handleFindVehicle(envelope);
            default -> Response.error(StatusCode.NOT_FOUND, "Operacao nao suportada");
        };
    }

    // ---- autenticacao ----

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
        boolean loggedOut = authService.logout(request.getToken());
        listener.onSessionCountChanged(authService.activeSessionCount());
        return loggedOut
                ? Response.ok("Usuário deslogado com sucesso", null)
                : Response.error(StatusCode.UNAUTHORIZED, "Sessão não encontrada ou já encerrada.");
    }

    private Response handleRegister(JsonObject envelope) {
        RegisterRequest request = JsonSupport.GSON.fromJson(envelope, RegisterRequest.class);
        Response invalido = validarCadastro(request.getName(), request.getUsername(), request.getPassword());
        if (invalido != null) return invalido;

        if (request.getRole() != null && request.getToken() != null) {
            Optional<User> requestor = authService.resolveUser(request.getToken());
            if (requestor.isEmpty()) return Response.error(StatusCode.UNAUTHORIZED, "Sessão expirada ou encerrada.");
            if (requestor.get().getRole() != UserRole.ADMIN) {
                return Response.error(StatusCode.FORBIDDEN, "Apenas administradores podem criar usuários com papel.");
            }
            UserRole role = "admin".equals(request.getRole()) ? UserRole.ADMIN : UserRole.USER;
            boolean created = authService.registerWithRole(request.getName(), request.getUsername(), request.getPassword(), role);
            return created
                    ? Response.created("Usuário criado com sucesso", null)
                    : Response.error(StatusCode.CONFLICT, "O username já está em uso.");
        }

        boolean created = authService.register(request.getName(), request.getUsername(), request.getPassword());
        return created
                ? Response.created("Usuário criado com sucesso", null)
                : Response.error(StatusCode.CONFLICT, "O username já está em uso.");
    }

    private Response handleGetUser(JsonObject envelope) {
        GetUserRequest request = JsonSupport.GSON.fromJson(envelope, GetUserRequest.class);
        if (request.getToken() == null || request.getToken().isBlank()) {
            return Response.error(StatusCode.BAD_REQUEST, "Token de autenticação não fornecido.");
        }
        Optional<User> requestor = authService.resolveUser(request.getToken());
        if (requestor.isEmpty()) return Response.error(StatusCode.UNAUTHORIZED, "Sessão expirada ou encerrada.");
        if (requestor.get().getRole() != UserRole.ADMIN
                && !requestor.get().getUsername().equals(request.getUsername())) {
            return Response.error(StatusCode.FORBIDDEN, "Operação não autorizada.");
        }
        Optional<User> user = authService.getUserData(request.getUsername());
        if (user.isEmpty()) return Response.error(StatusCode.NOT_FOUND, "Usuário não encontrado");
        return Response.ok("Usuário encontrado com sucesso",
                new UserData(user.get().getName(), user.get().getUsername(), user.get().getRole()));
    }

    private Response handleGetUsers(JsonObject envelope) {
        GetUsersRequest request = JsonSupport.GSON.fromJson(envelope, GetUsersRequest.class);
        Response adminCheck = verificarAdmin(request.getToken());
        if (adminCheck != null) return adminCheck;
        List<UserData> users = authService.getAllUsers().stream()
                .map(u -> new UserData(u.getName(), u.getUsername(), u.getRole()))
                .collect(Collectors.toList());
        return Response.ok("Usuários obtidos com sucesso", users);
    }

    private Response handleUpdateUserName(JsonObject envelope) {
        UpdateUserNameRequest request = JsonSupport.GSON.fromJson(envelope, UpdateUserNameRequest.class);
        Optional<User> requestor = authService.resolveUser(request.getToken());
        if (requestor.isEmpty()) return Response.error(StatusCode.UNAUTHORIZED, "Sessão expirada ou encerrada.");
        if (requestor.get().getRole() != UserRole.ADMIN
                && !requestor.get().getUsername().equals(request.getUsername())) {
            return Response.error(StatusCode.FORBIDDEN, "Operação não autorizada.");
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
        if (semAcesso != null) return semAcesso;
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
        Optional<User> requestor = authService.resolveUser(request.getToken());
        if (requestor.isEmpty()) return Response.error(StatusCode.UNAUTHORIZED, "Sessão expirada ou encerrada.");
        if (requestor.get().getRole() != UserRole.ADMIN
                && !requestor.get().getUsername().equals(request.getUsername())) {
            return Response.error(StatusCode.FORBIDDEN, "Operação não autorizada.");
        }
        boolean deleted = authService.deleteUser(request.getUsername(), request.getToken());
        listener.onSessionCountChanged(authService.activeSessionCount());
        return deleted
                ? Response.ok("Usuário deletado com sucesso", null)
                : Response.error(StatusCode.NOT_FOUND, "Usuário não encontrado");
    }

    // ---- vagas ----

    private Response handleCreateVaga(JsonObject envelope) {
        CreateVagaRequest request = JsonSupport.GSON.fromJson(envelope, CreateVagaRequest.class);
        Response adminCheck = verificarAdmin(request.getToken());
        if (adminCheck != null) return adminCheck;
        if (request.getSpotName() == null || request.getSpotName().isBlank()) {
            return Response.error(StatusCode.BAD_REQUEST, "Campos obrigatórios ausentes ou inválidos.");
        }
        String status = (request.getStatus() != null && !request.getStatus().isBlank())
                ? request.getStatus() : "fora de servico";
        Vaga vaga = new Vaga(request.getSpotName(), status, request.getFloor(), request.getType(), null);
        boolean created = vagaRepository.save(vaga);
        return created
                ? Response.created("Vaga criada com sucesso", null)
                : Response.error(StatusCode.CONFLICT, "Já existe uma vaga com esse nome.");
    }

    private Response handleGetVagas(JsonObject envelope) {
        GetVagasRequest request = JsonSupport.GSON.fromJson(envelope, GetVagasRequest.class);
        Response authCheck = verificarToken(request.getToken());
        if (authCheck != null) return authCheck;
        List<VagaData> vagas = vagaRepository.findAll().stream()
                .map(v -> new VagaData(v.getFloor(), v.getSpotName(), v.getStatus(), v.getLicensePlate(), v.getType()))
                .collect(Collectors.toList());
        return Response.ok("Vagas obtidas com sucesso", vagas);
    }

    private Response handleUpdateVaga(JsonObject envelope) {
        UpdateVagaRequest request = JsonSupport.GSON.fromJson(envelope, UpdateVagaRequest.class);
        Response authCheck = verificarAdmin(request.getToken());
        if (authCheck != null) return authCheck;
        if (request.getSpotName() == null || request.getSpotName().isBlank()) {
            return Response.error(StatusCode.BAD_REQUEST, "spotName é obrigatório.");
        }
        Optional<Vaga> vagaOpt = vagaRepository.findBySpotName(request.getSpotName());
        if (vagaOpt.isEmpty()) return Response.error(StatusCode.NOT_FOUND, "Vaga não encontrada.");
        Vaga vaga = vagaOpt.get();

        if (request.getNewSpotName() != null && !request.getNewSpotName().isBlank()
                && !request.getNewSpotName().equals(request.getSpotName())) {
            vagaRepository.deleteBySpotName(vaga.getSpotName());
            vaga.setSpotName(request.getNewSpotName());
            vagaRepository.save(vaga);
            return Response.ok("Vaga atualizada com sucesso", null);
        }
        if (request.getStatus() != null) vaga.setStatus(request.getStatus());
        if (request.getLicensePlate() != null) vaga.setLicensePlate(request.getLicensePlate());
        if (request.getFloor() != null) vaga.setFloor(request.getFloor());
        vagaRepository.update(vaga);
        return Response.ok("Vaga atualizada com sucesso", null);
    }

    private Response handleDeleteVaga(JsonObject envelope) {
        DeleteVagaRequest request = JsonSupport.GSON.fromJson(envelope, DeleteVagaRequest.class);
        Response adminCheck = verificarAdmin(request.getToken());
        if (adminCheck != null) return adminCheck;
        boolean deleted = vagaRepository.deleteBySpotName(request.getSpotName());
        return deleted
                ? Response.ok("Vaga removida com sucesso", null)
                : Response.error(StatusCode.NOT_FOUND, "Vaga não encontrada.");
    }

    // ---- veiculos ----

    private Response handleCreateVehicle(JsonObject envelope) {
        CreateVehicleRequest request = JsonSupport.GSON.fromJson(envelope, CreateVehicleRequest.class);
        Optional<User> user = authService.resolveUser(request.getToken());
        if (user.isEmpty()) return Response.error(StatusCode.UNAUTHORIZED, "Sessão expirada ou encerrada.");
        if (request.getBrand() == null || request.getModel() == null
                || request.getType() == null || request.getLicensePlate() == null) {
            return Response.error(StatusCode.BAD_REQUEST, "Campos obrigatórios ausentes ou inválidos.");
        }
        Veiculo veiculo = new Veiculo(request.getBrand(), request.getModel(), request.getType(),
                request.getLicensePlate(), user.get().getUsername());
        boolean created = vehicleRepository.save(veiculo);
        return created
                ? Response.created("Veículo criado com sucesso", null)
                : Response.error(StatusCode.CONFLICT, "Já existe um veículo com essa placa.");
    }

    private Response handleGetVehicles(JsonObject envelope) {
        GetVehiclesRequest request = JsonSupport.GSON.fromJson(envelope, GetVehiclesRequest.class);
        Optional<User> user = authService.resolveUser(request.getToken());
        if (user.isEmpty()) return Response.error(StatusCode.UNAUTHORIZED, "Sessão expirada ou encerrada.");
        List<VehicleData> vehicles = vehicleRepository.findByOwner(user.get().getUsername()).stream()
                .map(v -> new VehicleData(v.getBrand(), v.getModel(), v.getType(), v.getLicensePlate()))
                .collect(Collectors.toList());
        return Response.ok("Veículos obtidos com sucesso", vehicles);
    }

    private Response handleGetVehicle(JsonObject envelope) {
        GetVehicleRequest request = JsonSupport.GSON.fromJson(envelope, GetVehicleRequest.class);
        Optional<User> user = authService.resolveUser(request.getToken());
        if (user.isEmpty()) return Response.error(StatusCode.UNAUTHORIZED, "Sessão expirada ou encerrada.");
        Optional<Veiculo> veiculo = vehicleRepository.findByLicensePlate(request.getLicensePlate());
        if (veiculo.isEmpty()) return Response.error(StatusCode.NOT_FOUND, "Veículo não encontrado.");
        if (user.get().getRole() != UserRole.ADMIN
                && !user.get().getUsername().equals(veiculo.get().getOwnerUsername())) {
            return Response.error(StatusCode.FORBIDDEN, "Operação não autorizada.");
        }
        Veiculo v = veiculo.get();
        return Response.ok("Veículo encontrado com sucesso",
                new VehicleData(v.getBrand(), v.getModel(), v.getType(), v.getLicensePlate()));
    }

    private Response handleUpdateVehicle(JsonObject envelope) {
        UpdateVehicleRequest request = JsonSupport.GSON.fromJson(envelope, UpdateVehicleRequest.class);
        Optional<User> user = authService.resolveUser(request.getToken());
        if (user.isEmpty()) return Response.error(StatusCode.UNAUTHORIZED, "Sessão expirada ou encerrada.");
        Optional<Veiculo> veiculoOpt = vehicleRepository.findByLicensePlate(request.getLicensePlate());
        if (veiculoOpt.isEmpty()) return Response.error(StatusCode.NOT_FOUND, "Veículo não encontrado.");
        Veiculo veiculo = veiculoOpt.get();
        if (!user.get().getUsername().equals(veiculo.getOwnerUsername())) {
            return Response.error(StatusCode.FORBIDDEN, "Operação não autorizada.");
        }
        if (request.getBrand() != null) veiculo.setBrand(request.getBrand());
        if (request.getModel() != null) veiculo.setModel(request.getModel());
        if (request.getType() != null) veiculo.setType(request.getType());
        vehicleRepository.update(veiculo);
        return Response.ok("Veículo atualizado com sucesso", null);
    }

    private Response handleDeleteVehicle(JsonObject envelope) {
        DeleteVehicleRequest request = JsonSupport.GSON.fromJson(envelope, DeleteVehicleRequest.class);
        Optional<User> user = authService.resolveUser(request.getToken());
        if (user.isEmpty()) return Response.error(StatusCode.UNAUTHORIZED, "Sessão expirada ou encerrada.");
        Optional<Veiculo> veiculo = vehicleRepository.findByLicensePlate(request.getLicensePlate());
        if (veiculo.isEmpty()) return Response.error(StatusCode.NOT_FOUND, "Veículo não encontrado.");
        if (user.get().getRole() != UserRole.ADMIN
                && !user.get().getUsername().equals(veiculo.get().getOwnerUsername())) {
            return Response.error(StatusCode.FORBIDDEN, "Operação não autorizada.");
        }
        vehicleRepository.deleteByLicensePlate(request.getLicensePlate());
        return Response.ok("Veículo removido com sucesso", null);
    }

    private Response handleFindVehicle(JsonObject envelope) {
        FindVehicleRequest request = JsonSupport.GSON.fromJson(envelope, FindVehicleRequest.class);
        Response authCheck = verificarToken(request.getToken());
        if (authCheck != null) return authCheck;
        Optional<Vaga> vaga = vagaRepository.findAll().stream()
                .filter(v -> request.getLicensePlate().equals(v.getLicensePlate()))
                .findFirst();
        if (vaga.isEmpty()) return Response.error(StatusCode.NOT_FOUND, "Veículo não encontrado em nenhuma vaga.");
        return Response.ok("Veiculo localizado com sucesso",
                new FindVehicleData(vaga.get().getFloor(), vaga.get().getSpotName(), vaga.get().getStatus()));
    }

    // ---- helpers ----

    private Response verificarAcesso(String token, String username) {
        if (token == null || token.isBlank()) {
            return Response.error(StatusCode.BAD_REQUEST, "Token de autenticação não fornecido.");
        }
        Optional<Session> session = authService.resolveSession(token);
        if (session.isEmpty()) return Response.error(StatusCode.UNAUTHORIZED, "Sessão expirada ou encerrada.");
        if (!session.get().getUsername().equals(username)) {
            return Response.error(StatusCode.FORBIDDEN, "Sessão expirada ou encerrada.");
        }
        return null;
    }

    private Response verificarToken(String token) {
        if (token == null || token.isBlank()) {
            return Response.error(StatusCode.BAD_REQUEST, "Token de autenticação não fornecido.");
        }
        Optional<Session> session = authService.resolveSession(token);
        if (session.isEmpty()) return Response.error(StatusCode.UNAUTHORIZED, "Sessão expirada ou encerrada.");
        return null;
    }

    private Response verificarAdmin(String token) {
        if (token == null || token.isBlank()) {
            return Response.error(StatusCode.BAD_REQUEST, "Token de autenticação não fornecido.");
        }
        Optional<User> user = authService.resolveUser(token);
        if (user.isEmpty()) return Response.error(StatusCode.UNAUTHORIZED, "Sessão expirada ou encerrada.");
        if (user.get().getRole() != UserRole.ADMIN) {
            return Response.error(StatusCode.FORBIDDEN, "Apenas administradores podem realizar esta operação.");
        }
        return null;
    }

    private Response validarCadastro(String name, String username, String password) {
        if (!NameValidator.isValid(name)) return Response.error(StatusCode.BAD_REQUEST, "O campo name não está no padrão esperado.");
        if (!UsernameValidator.isValid(username)) return Response.error(StatusCode.BAD_REQUEST, "O campo username não está no padrão esperado.");
        if (!PasswordValidator.isValid(password)) return Response.error(StatusCode.BAD_REQUEST, "O campo password não está no padrão esperado.");
        return null;
    }

    private static boolean credenciaisValidas(String username, String password) {
        return UsernameValidator.isValid(username) && PasswordValidator.isValid(password);
    }
}
