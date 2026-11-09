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
import java.util.Set;
import java.util.stream.Collectors;

public final class RequestDispatcher {

    private static final Set<String> TIPOS_VALIDOS = Set.of("combustao", "eletrico");

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
            return Response.error(StatusCode.UNAUTHORIZED, "Usuario ou senhas inválidos");
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

        // role informado sem token (combinação incompleta) → rejeitar
        if (request.getRole() != null && (request.getToken() == null || request.getToken().isBlank())) {
            return Response.error(StatusCode.BAD_REQUEST, "Campos obrigatórios ausentes ou inválidos.");
        }

        if (request.getRole() != null && request.getToken() != null) {
            Optional<User> requestor = authService.resolveUser(request.getToken());
            if (requestor.isEmpty()) return Response.error(StatusCode.UNAUTHORIZED, "Sessão expirada ou encerrada.");
            if (requestor.get().getRole() != UserRole.ADMIN) {
                return Response.error(StatusCode.UNAUTHORIZED, "Apenas administradores podem criar usuários com papel.");
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
        // usuário comum só pode consultar o próprio perfil
        if (requestor.get().getRole() != UserRole.ADMIN
                && !requestor.get().getUsername().equals(request.getUsername())) {
            return Response.error(StatusCode.UNAUTHORIZED, "Operação não autorizada.");
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
            return Response.error(StatusCode.UNAUTHORIZED, "Sessão expirada ou encerrada.");
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
            return Response.error(StatusCode.UNAUTHORIZED, "Operação não autorizada.");
        }
        // não pode deletar usuário com veículo estacionado
        boolean temVagaOcupada = vehicleRepository.findByOwner(request.getUsername()).stream()
                .anyMatch(v -> vagaRepository.findAll().stream()
                        .anyMatch(vaga -> v.getLicensePlate().equals(vaga.getLicensePlate())));
        if (temVagaOcupada) {
            return Response.error(StatusCode.CONFLICT, "Usuário possui veículo estacionado.");
        }
        // cascade: remove veículos do usuário
        vehicleRepository.findByOwner(request.getUsername())
                .forEach(v -> vehicleRepository.deleteByLicensePlate(v.getLicensePlate()));
        // remove usuário e invalida a sessão DELE
        boolean deleted = authService.deleteUser(request.getUsername());
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
        if (request.getSpotName() == null || request.getSpotName().isBlank()
                || request.getSpotName().length() > 4) {
            return Response.error(StatusCode.BAD_REQUEST, "Campos obrigatórios ausentes ou inválidos.");
        }
        if (!TIPOS_VALIDOS.contains(request.getType())) {
            return Response.error(StatusCode.BAD_REQUEST, "Tipo de vaga inválido. Use: combustao ou eletrico.");
        }
        Vaga vaga = new Vaga(request.getSpotName(), "disponivel", request.getFloor(), request.getType(), null);
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
        if (request.getToken() == null || request.getToken().isBlank()) {
            return Response.error(StatusCode.BAD_REQUEST, "Token de autenticação não fornecido.");
        }
        Optional<User> userOpt = authService.resolveUser(request.getToken());
        if (userOpt.isEmpty()) return Response.error(StatusCode.UNAUTHORIZED, "Sessão expirada ou encerrada.");
        User caller = userOpt.get();
        boolean isAdmin = caller.getRole() == UserRole.ADMIN;

        if (request.getSpotName() == null || request.getSpotName().isBlank()) {
            return Response.error(StatusCode.BAD_REQUEST, "spotName é obrigatório.");
        }
        Optional<Vaga> vagaOpt = vagaRepository.findBySpotName(request.getSpotName());
        if (vagaOpt.isEmpty()) return Response.error(StatusCode.NOT_FOUND, "Vaga não encontrada.");
        Vaga vaga = vagaOpt.get();

        // renomear — admin only
        String newSpotName = request.getNewSpotName();
        if (newSpotName != null && !newSpotName.isBlank() && !newSpotName.equals(request.getSpotName())) {
            if (!isAdmin) return Response.error(StatusCode.UNAUTHORIZED, "Apenas administradores podem realizar esta operação.");
            if (!"disponivel".equals(vaga.getStatus()) && !"fora_de_servico".equals(vaga.getStatus())) {
                return Response.error(StatusCode.CONFLICT, "Não é possível renomear uma vaga ocupada.");
            }
            if (vagaRepository.findBySpotName(newSpotName).isPresent()) {
                return Response.error(StatusCode.CONFLICT, "Já existe uma vaga com esse nome.");
            }
            vagaRepository.deleteBySpotName(vaga.getSpotName());
            vaga.setSpotName(newSpotName);
            vagaRepository.save(vaga);
            return Response.ok("Vaga atualizada com sucesso", null);
        }

        String newStatus = request.getStatus();
        String newPlate = request.getLicensePlate();

        // usuário estacionando — plate fornecida + status ocupada
        if (!isAdmin && newPlate != null && !newPlate.isBlank() && "ocupada".equals(newStatus)) {
            if (!"disponivel".equals(vaga.getStatus())) {
                return Response.error(StatusCode.CONFLICT, "Vaga não está disponível.");
            }
            Optional<Veiculo> veiculoParaEstacionar = vehicleRepository.findByLicensePlate(newPlate);
            if (veiculoParaEstacionar.isEmpty()) {
                return Response.error(StatusCode.NOT_FOUND, "Veículo não encontrado.");
            }
            if (!vaga.getType().equals(veiculoParaEstacionar.get().getType())) {
                return Response.error(StatusCode.CONFLICT, "Tipo do veículo incompatível com o tipo da vaga.");
            }
            vaga.setLicensePlate(newPlate);
            vaga.setStatus("ocupada");
            vagaRepository.update(vaga);
            return Response.ok("Vaga atualizada com sucesso", null);
        }

        // usuário liberando — status disponivel (ou omitido), vaga estava ocupada
        if (!isAdmin && "ocupada".equals(vaga.getStatus())
                && ("disponivel".equals(newStatus) || newStatus == null)) {
            String currentPlate = vaga.getLicensePlate();
            if (currentPlate != null) {
                Optional<Veiculo> veiculoOpt = vehicleRepository.findByLicensePlate(currentPlate);
                if (veiculoOpt.isEmpty() || !veiculoOpt.get().getOwnerUsername().equals(caller.getUsername())) {
                    return Response.error(StatusCode.UNAUTHORIZED, "Operação não autorizada.");
                }
            }
            vaga.setLicensePlate(null);
            vaga.setStatus("disponivel");
            vagaRepository.update(vaga);
            return Response.ok("Vaga atualizada com sucesso", null);
        }

        // demais operações — apenas admin
        if (!isAdmin) return Response.error(StatusCode.UNAUTHORIZED, "Apenas administradores podem realizar esta operação.");

        // floor só pode ser alterado quando a vaga não está ocupada
        if (request.getFloor() != null && "ocupada".equals(vaga.getStatus())) {
            return Response.error(StatusCode.CONFLICT, "Não é possível alterar o andar de uma vaga ocupada.");
        }
        // fora_de_servico só pode ser definido a partir de disponivel
        if ("fora_de_servico".equals(newStatus) && !"disponivel".equals(vaga.getStatus())) {
            return Response.error(StatusCode.CONFLICT, "A vaga precisa estar disponível para ir a fora de serviço.");
        }

        if (newStatus != null) vaga.setStatus(newStatus);
        // ao liberar uma vaga, a placa e sempre removida independente do que foi enviado
        if ("disponivel".equals(vaga.getStatus())) {
            vaga.setLicensePlate(null);
        } else if (newPlate != null) {
            vaga.setLicensePlate(newPlate.isBlank() ? null : newPlate);
        }
        if (request.getFloor() != null) vaga.setFloor(request.getFloor());
        vagaRepository.update(vaga);
        return Response.ok("Vaga atualizada com sucesso", null);
    }

    private Response handleDeleteVaga(JsonObject envelope) {
        DeleteVagaRequest request = JsonSupport.GSON.fromJson(envelope, DeleteVagaRequest.class);
        Response adminCheck = verificarAdmin(request.getToken());
        if (adminCheck != null) return adminCheck;
        Optional<Vaga> vagaOpt = vagaRepository.findBySpotName(request.getSpotName());
        if (vagaOpt.isEmpty()) return Response.error(StatusCode.NOT_FOUND, "Vaga não encontrada.");
        if ("ocupada".equals(vagaOpt.get().getStatus())) {
            return Response.error(StatusCode.CONFLICT, "Vaga ocupada não pode ser removida.");
        }
        vagaRepository.deleteBySpotName(request.getSpotName());
        return Response.ok("Vaga removida com sucesso", null);
    }

    // ---- veiculos ----

    private Response handleCreateVehicle(JsonObject envelope) {
        CreateVehicleRequest request = JsonSupport.GSON.fromJson(envelope, CreateVehicleRequest.class);
        Optional<User> user = authService.resolveUser(request.getToken());
        if (user.isEmpty()) return Response.error(StatusCode.UNAUTHORIZED, "Sessão expirada ou encerrada.");
        if (user.get().getRole() == UserRole.ADMIN) {
            return Response.error(StatusCode.UNAUTHORIZED, "Apenas usuários comuns podem utilizar este endpoint.");
        }
        if (request.getBrand() == null || request.getBrand().isBlank()
                || request.getType() == null || request.getLicensePlate() == null
                || request.getLicensePlate().isBlank()) {
            return Response.error(StatusCode.BAD_REQUEST, "Campos obrigatórios ausentes ou inválidos.");
        }
        if (request.getLicensePlate().length() != 7) {
            return Response.error(StatusCode.BAD_REQUEST, "Campos obrigatórios ausentes ou inválidos.");
        }
        if (request.getBrand().length() > 20) {
            return Response.error(StatusCode.BAD_REQUEST, "Campos obrigatórios ausentes ou inválidos.");
        }
        if (request.getModel() != null && request.getModel().length() > 20) {
            return Response.error(StatusCode.BAD_REQUEST, "Campos obrigatórios ausentes ou inválidos.");
        }
        if (!TIPOS_VALIDOS.contains(request.getType())) {
            return Response.error(StatusCode.BAD_REQUEST, "Tipo de veículo inválido. Use: combustao ou eletrico.");
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
        if (user.get().getRole() == UserRole.ADMIN) {
            return Response.error(StatusCode.UNAUTHORIZED, "Apenas usuários comuns podem utilizar este endpoint.");
        }
        List<VehicleData> vehicles = vehicleRepository.findByOwner(user.get().getUsername()).stream()
                .map(v -> new VehicleData(v.getBrand(), v.getModel(), v.getType(), v.getLicensePlate()))
                .collect(Collectors.toList());
        return Response.ok("Veículos obtidos com sucesso", vehicles);
    }

    private Response handleGetVehicle(JsonObject envelope) {
        GetVehicleRequest request = JsonSupport.GSON.fromJson(envelope, GetVehicleRequest.class);
        Optional<User> user = authService.resolveUser(request.getToken());
        if (user.isEmpty()) return Response.error(StatusCode.UNAUTHORIZED, "Sessão expirada ou encerrada.");
        if (request.getLicensePlate() == null || request.getLicensePlate().isBlank()) {
            return Response.error(StatusCode.BAD_REQUEST, "Placa não fornecida.");
        }
        Optional<Veiculo> veiculo = vehicleRepository.findByLicensePlate(request.getLicensePlate());
        if (veiculo.isEmpty()) return Response.error(StatusCode.NOT_FOUND, "Veículo não encontrado.");
        // admin pode ver qualquer veículo; user só o próprio
        if (user.get().getRole() != UserRole.ADMIN
                && !user.get().getUsername().equals(veiculo.get().getOwnerUsername())) {
            return Response.error(StatusCode.NOT_FOUND, "Veículo não encontrado.");
        }
        Veiculo v = veiculo.get();
        return Response.ok("Veículo encontrado com sucesso",
                new VehicleData(v.getBrand(), v.getModel(), v.getType(), v.getLicensePlate()));
    }

    private Response handleUpdateVehicle(JsonObject envelope) {
        UpdateVehicleRequest request = JsonSupport.GSON.fromJson(envelope, UpdateVehicleRequest.class);
        Optional<User> user = authService.resolveUser(request.getToken());
        if (user.isEmpty()) return Response.error(StatusCode.UNAUTHORIZED, "Sessão expirada ou encerrada.");
        if (user.get().getRole() == UserRole.ADMIN) {
            return Response.error(StatusCode.UNAUTHORIZED, "Apenas usuários comuns podem utilizar este endpoint.");
        }
        // Verifica quais campos foram enviados explicitamente no JSON (null explícito vs campo ausente)
        JsonObject dataNode = envelope.has("data") ? envelope.getAsJsonObject("data") : envelope;
        boolean brandEnviada = dataNode != null && dataNode.has("brand");
        boolean modelEnviado = dataNode != null && dataNode.has("model");
        // brand enviada como null ou vazia → 400 (campo obrigatório, não pode ser removido)
        if (brandEnviada && (request.getBrand() == null || request.getBrand().isBlank())) {
            return Response.error(StatusCode.BAD_REQUEST, "Campos obrigatórios ausentes ou inválidos.");
        }
        Optional<Veiculo> veiculoOpt = vehicleRepository.findByLicensePlate(request.getLicensePlate());
        if (veiculoOpt.isEmpty()) return Response.error(StatusCode.NOT_FOUND, "Veículo não encontrado.");
        Veiculo veiculo = veiculoOpt.get();
        if (!user.get().getUsername().equals(veiculo.getOwnerUsername())) {
            return Response.error(StatusCode.NOT_FOUND, "Veículo não encontrado.");
        }
        // Verifica se type foi enviado explicitamente no JSON
        boolean typeEnviado = dataNode != null && dataNode.has("type");
        // type enviado como null ou valor inválido → 400
        if (typeEnviado && (request.getType() == null || !TIPOS_VALIDOS.contains(request.getType()))) {
            return Response.error(StatusCode.BAD_REQUEST, "Campos obrigatórios ausentes ou inválidos.");
        }
        // Apenas mudança de type é bloqueada quando estacionado; brand/model são sempre permitidos
        if (typeEnviado) {
            boolean estaEstacionado = vagaRepository.findAll().stream()
                    .anyMatch(v -> veiculo.getLicensePlate().equals(v.getLicensePlate()));
            if (estaEstacionado) {
                return Response.error(StatusCode.CONFLICT, "Veículo associado a uma vaga. Libere a vaga antes desta operação.");
            }
        }
        if (brandEnviada && request.getBrand() != null) veiculo.setBrand(request.getBrand());
        if (modelEnviado) veiculo.setModel(request.getModel()); // null explícito = limpar modelo
        if (typeEnviado) veiculo.setType(request.getType());
        vehicleRepository.update(veiculo);
        return Response.ok("Veículo atualizado com sucesso", null);
    }

    private Response handleDeleteVehicle(JsonObject envelope) {
        DeleteVehicleRequest request = JsonSupport.GSON.fromJson(envelope, DeleteVehicleRequest.class);
        Optional<User> user = authService.resolveUser(request.getToken());
        if (user.isEmpty()) return Response.error(StatusCode.UNAUTHORIZED, "Sessão expirada ou encerrada.");
        Optional<Veiculo> veiculo = vehicleRepository.findByLicensePlate(request.getLicensePlate());
        if (veiculo.isEmpty()) return Response.error(StatusCode.NOT_FOUND, "Veículo não encontrado.");
        // admin pode deletar qualquer veículo; user só o próprio
        if (user.get().getRole() != UserRole.ADMIN
                && !user.get().getUsername().equals(veiculo.get().getOwnerUsername())) {
            return Response.error(StatusCode.NOT_FOUND, "Veículo não encontrado.");
        }
        // não pode excluir veículo estacionado
        boolean estaEstacionado = vagaRepository.findAll().stream()
                .anyMatch(v -> request.getLicensePlate().equals(v.getLicensePlate()));
        if (estaEstacionado) {
            return Response.error(StatusCode.CONFLICT, "Veículo associado a uma vaga. Libere a vaga antes desta operação.");
        }
        vehicleRepository.deleteByLicensePlate(request.getLicensePlate());
        return Response.ok("Veículo removido com sucesso", null);
    }

    private Response handleFindVehicle(JsonObject envelope) {
        FindVehicleRequest request = JsonSupport.GSON.fromJson(envelope, FindVehicleRequest.class);
        Optional<User> caller = authService.resolveUser(request.getToken());
        if (caller.isEmpty()) {
            if (request.getToken() == null || request.getToken().isBlank())
                return Response.error(StatusCode.BAD_REQUEST, "Token de autenticação não fornecido.");
            return Response.error(StatusCode.UNAUTHORIZED, "Sessão expirada ou encerrada.");
        }
        if (request.getLicensePlate() == null || request.getLicensePlate().isBlank()) {
            return Response.error(StatusCode.BAD_REQUEST, "Placa não fornecida.");
        }
        // usuário comum só pode localizar o próprio veículo
        if (caller.get().getRole() != UserRole.ADMIN) {
            Optional<Veiculo> veiculoCaller = vehicleRepository.findByLicensePlate(request.getLicensePlate());
            if (veiculoCaller.isEmpty()
                    || !veiculoCaller.get().getOwnerUsername().equals(caller.get().getUsername())) {
                return Response.error(StatusCode.NOT_FOUND, "Veículo não encontrado em nenhuma vaga.");
            }
        }
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
            return Response.error(StatusCode.UNAUTHORIZED, "Sessão expirada ou encerrada.");
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
            return Response.error(StatusCode.UNAUTHORIZED, "Sessão expirada ou encerrada.");
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
