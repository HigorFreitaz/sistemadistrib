package br.edu.utfpr.sd.garagem.client.ui;

import br.edu.utfpr.sd.garagem.client.net.ConnectionException;
import br.edu.utfpr.sd.garagem.client.net.SocketConnector;
import br.edu.utfpr.sd.garagem.common.json.JsonSupport;
import br.edu.utfpr.sd.garagem.common.model.UserRole;
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
import br.edu.utfpr.sd.garagem.common.protocol.GetVehiclesRequest;
import br.edu.utfpr.sd.garagem.common.protocol.LogoutRequest;
import br.edu.utfpr.sd.garagem.common.protocol.RegisterRequest;
import br.edu.utfpr.sd.garagem.common.protocol.Response;
import br.edu.utfpr.sd.garagem.common.protocol.StatusCode;
import br.edu.utfpr.sd.garagem.common.protocol.UpdateUserNameRequest;
import br.edu.utfpr.sd.garagem.common.protocol.UpdateVagaRequest;
import br.edu.utfpr.sd.garagem.common.protocol.UpdateVehicleRequest;
import br.edu.utfpr.sd.garagem.common.protocol.UserData;
import br.edu.utfpr.sd.garagem.common.protocol.VagaData;
import br.edu.utfpr.sd.garagem.common.protocol.VehicleData;
import com.google.gson.JsonArray;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MainController {

    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

    // header
    @FXML private Label usernameLabel;
    @FXML private Label roleLabel;
    @FXML private Label statusLabel;
    @FXML private Button logoutButton;
    @FXML private Button profileButton;
    @FXML private ProgressIndicator progressIndicator;

    // tabs
    @FXML private javafx.scene.control.TabPane tabPane;

    // vagas tab
    @FXML private Tab vagasTab;
    @FXML private TableView<VagaData> vagasTable;
    @FXML private TableColumn<VagaData, String> vagaSpotNameCol;
    @FXML private TableColumn<VagaData, Number> vagaFloorCol;
    @FXML private TableColumn<VagaData, String> vagaTypeCol;
    @FXML private TableColumn<VagaData, String> vagaStatusCol;
    @FXML private TableColumn<VagaData, String> vagaPlateCol;
    @FXML private Button createVagaButton;
    @FXML private Button deleteVagaButton;
    @FXML private Button updateVagaButton;
    @FXML private javafx.scene.control.TextField findPlateField;
    @FXML private Label findVehicleResult;

    // veículos tab
    @FXML private Tab veiculosTab;
    @FXML private TableView<VehicleData> vehiclesTable;
    @FXML private TableColumn<VehicleData, String> vehicleBrandCol;
    @FXML private TableColumn<VehicleData, String> vehicleModelCol;
    @FXML private TableColumn<VehicleData, String> vehicleTypeCol;
    @FXML private TableColumn<VehicleData, String> vehiclePlateCol;
    @FXML private Button editVehicleButton;
    @FXML private Button deleteVehicleButton;

    // usuários tab (admin)
    @FXML private Tab usuariosTab;
    @FXML private TableView<UserData> usersTable;
    @FXML private TableColumn<UserData, String> userUsernameCol;
    @FXML private TableColumn<UserData, String> userNameCol;
    @FXML private TableColumn<UserData, String> userRoleCol;
    @FXML private Button editUserButton;
    @FXML private Button deleteUserAdminButton;
    @FXML private TextField userSearchField;

    private final ObservableList<UserData> allUsers = FXCollections.observableArrayList();

    private ClienteApp app;
    private String host;
    private int port;
    private String username;
    private String token;
    private UserRole role;
    private boolean loading;
    private Timeline heartbeat;
    private final AtomicBoolean heartbeatRunning = new AtomicBoolean(false);

    public void init(ClienteApp app, String host, int port, String username, String token) {
        this.app = app;
        this.host = host;
        this.port = port;
        this.username = username;
        this.token = token;
        usernameLabel.setText("Bem-vindo, " + username);
        setupTables();
        loadUserProfile();
        startHeartbeat();
    }

    @FXML
    private void initialize() {
        // tab de usuários fica oculta até saber que o role é admin
        tabPane.getTabs().remove(usuariosTab);
        vagasTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, v) -> {
                    updateVagaButton.setDisable(v == null);
                    if (deleteVagaButton.isVisible()) deleteVagaButton.setDisable(v == null);
                });
        vehiclesTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, v) -> {
                    editVehicleButton.setDisable(v == null);
                    deleteVehicleButton.setDisable(v == null);
                });
        usersTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, u) -> {
                    editUserButton.setDisable(u == null);
                    deleteUserAdminButton.setDisable(u == null);
                });

        FilteredList<UserData> filteredUsers = new FilteredList<>(allUsers, p -> true);
        userSearchField.textProperty().addListener((obs, old, text) ->
            filteredUsers.setPredicate(u -> {
                if (text == null || text.isBlank()) return true;
                String lower = text.toLowerCase();
                return (u.getUsername() != null && u.getUsername().toLowerCase().contains(lower))
                    || (u.getName() != null && u.getName().toLowerCase().contains(lower));
            })
        );
        usersTable.setItems(filteredUsers);
    }

    private void setupTables() {
        vagaSpotNameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSpotName()));
        vagaFloorCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().getFloor()));
        vagaTypeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        vagaStatusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));
        vagaPlateCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLicensePlate()));

        vehicleBrandCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBrand()));
        vehicleModelCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getModel()));
        vehicleTypeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        vehiclePlateCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getLicensePlate()));

        userUsernameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUsername()));
        userNameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        userRoleCol.setCellValueFactory(c -> {
            UserRole r = c.getValue().getRole();
            return new SimpleStringProperty(r == UserRole.ADMIN ? "admin" : "user");
        });

        vagasTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        vehiclesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadUserProfile() {
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws ConnectionException {
                try (SocketConnector connector = new SocketConnector(host, port)) {
                    connector.connect();
                    return connector.send(new GetUserRequest(token, username));
                }
            }
        };
        task.setOnSucceeded(event -> {
            Response response = task.getValue();
            if (response.getStatusCode() == StatusCode.OK) {
                try {
                    UserData data = JsonSupport.GSON.fromJson(response.getData(), UserData.class);
                    if (data.getName() != null && !data.getName().isBlank()) {
                        usernameLabel.setText("Bem-vindo, " + data.getName());
                    }
                    role = data.getRole();
                    applyRoleUI();
                    handleRefreshVagas();
                    if (role == UserRole.ADMIN) {
                        handleRefreshUsers();
                    } else {
                        handleRefreshVehicles();
                    }
                } catch (JsonSyntaxException e) {
                    LOGGER.log(Level.WARNING, "resposta de getuser em formato inesperado", e);
                }
            } else if (response.getStatusCode() == StatusCode.UNAUTHORIZED) {
                app.showLogin();
            }
        });
        task.setOnFailed(event -> LOGGER.log(Level.WARNING, "falha ao carregar perfil", task.getException()));
        TaskRunner.runInBackground(task, "main-load-profile-task");
    }

    private void applyRoleUI() {
        // botão de ação na vaga: visível para todos (texto diferente por papel)
        updateVagaButton.setVisible(true);
        updateVagaButton.setManaged(true);

        if (role == UserRole.ADMIN) {
            roleLabel.setText("Administrador");
            createVagaButton.setVisible(true);
            createVagaButton.setManaged(true);
            deleteVagaButton.setVisible(true);
            deleteVagaButton.setManaged(true);
            tabPane.getTabs().add(usuariosTab);
        } else {
            roleLabel.setText("Usuário");
            updateVagaButton.setText("Estacionar / Liberar");
        }
    }

    // ---- vagas ----

    @FXML
    private void handleRefreshVagas() {
        setLoading(true);
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws ConnectionException {
                try (SocketConnector connector = new SocketConnector(host, port)) {
                    connector.connect();
                    return connector.send(new GetVagasRequest(token));
                }
            }
        };
        task.setOnSucceeded(event -> {
            setLoading(false);
            Response response = task.getValue();
            if (response.getStatusCode() == StatusCode.OK) {
                try {
                    List<VagaData> vagas = JsonSupport.GSON.fromJson(response.getData(),
                            new TypeToken<List<VagaData>>() {}.getType());
                    vagasTable.setItems(FXCollections.observableArrayList(vagas != null ? vagas : List.of()));
                } catch (JsonSyntaxException e) {
                    LOGGER.log(Level.WARNING, "resposta de getvagas malformada", e);
                }
            } else if (response.getStatusCode() == StatusCode.UNAUTHORIZED) {
                app.showLogin();
            }
        });
        task.setOnFailed(event -> { setLoading(false); logWarning("falha ao carregar vagas", task.getException()); });
        TaskRunner.runInBackground(task, "main-get-vagas-task");
    }

    @FXML
    private void handleCreateVaga() {
        TextInputDialog spotDlg = new TextInputDialog();
        spotDlg.setHeaderText(null);
        spotDlg.setTitle("Nova vaga");
        spotDlg.setContentText("Nome da vaga:");
        Optional<String> spotName = spotDlg.showAndWait();
        if (spotName.isEmpty() || spotName.get().isBlank()) return;

        TextInputDialog floorDlg = new TextInputDialog("1");
        floorDlg.setHeaderText(null);
        floorDlg.setTitle("Andar");
        floorDlg.setContentText("Andar:");
        Optional<String> floorStr = floorDlg.showAndWait();
        if (floorStr.isEmpty()) return;
        int floor;
        try { floor = Integer.parseInt(floorStr.get().trim()); }
        catch (NumberFormatException e) { showError("Andar deve ser um número."); return; }

        ChoiceDialog<String> typeDlg = new ChoiceDialog<>("combustao", "combustao", "eletrico");
        typeDlg.setHeaderText(null);
        typeDlg.setTitle("Tipo");
        typeDlg.setContentText("Tipo da vaga:");
        Optional<String> type = typeDlg.showAndWait();
        if (type.isEmpty()) return;

        final int f = floor;
        setLoading(true);
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws ConnectionException {
                try (SocketConnector connector = new SocketConnector(host, port)) {
                    connector.connect();
                    return connector.send(new CreateVagaRequest(token, spotName.get().trim(), f, type.get()));
                }
            }
        };
        task.setOnSucceeded(event -> {
            setLoading(false);
            Response r = task.getValue();
            if (r.getStatusCode() == StatusCode.CREATED) { handleRefreshVagas(); }
            else showError(r.getMessage());
        });
        task.setOnFailed(event -> { setLoading(false); showError("Falha ao criar vaga."); });
        TaskRunner.runInBackground(task, "main-create-vaga-task");
    }

    @FXML
    private void handleUpdateVaga() {
        VagaData selected = vagasTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (role != UserRole.ADMIN) {
            handleEstacionar(selected);
            return;
        }

        // --- fluxo admin: editar status, placa, renomear ---
        ChoiceDialog<String> statusDlg = new ChoiceDialog<>(selected.getStatus(),
                "disponivel", "fora_de_servico", "ocupada");
        statusDlg.setHeaderText("Vaga: " + selected.getSpotName());
        statusDlg.setTitle("Alterar status");
        statusDlg.setContentText("Novo status:");
        Optional<String> newStatus = statusDlg.showAndWait();
        if (newStatus.isEmpty()) return;

        TextInputDialog plateDlg = new TextInputDialog(selected.getLicensePlate() != null ? selected.getLicensePlate() : "");
        plateDlg.setHeaderText(null);
        plateDlg.setTitle("Placa");
        plateDlg.setContentText("Placa do veículo (vazio = nenhum):");
        Optional<String> plate = plateDlg.showAndWait();

        setLoading(true);
        String plateValue = plate.map(p -> p.isBlank() ? null : p.trim()).orElse(selected.getLicensePlate());
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws ConnectionException {
                try (SocketConnector connector = new SocketConnector(host, port)) {
                    connector.connect();
                    return connector.send(new UpdateVagaRequest(token, selected.getSpotName(),
                            null, newStatus.get(), plateValue, null));
                }
            }
        };
        task.setOnSucceeded(event -> {
            setLoading(false);
            Response r = task.getValue();
            if (r.getStatusCode() == StatusCode.OK) handleRefreshVagas();
            else showError(r.getMessage());
        });
        task.setOnFailed(event -> { setLoading(false); showError("Falha ao atualizar vaga."); });
        TaskRunner.runInBackground(task, "main-update-vaga-task");
    }

    private void handleEstacionar(VagaData vaga) {
        if ("fora_de_servico".equals(vaga.getStatus())) {
            showError("Esta vaga está fora de serviço e não pode ser utilizada.");
            return;
        }

        final String newStatus;
        final String newPlate;

        if ("disponivel".equals(vaga.getStatus())) {
            TextInputDialog plateDlg = new TextInputDialog();
            plateDlg.setTitle("Estacionar");
            plateDlg.setHeaderText("Vaga: " + vaga.getSpotName() + " (disponível)");
            plateDlg.setContentText("Placa do seu veículo:");
            Optional<String> plate = plateDlg.showAndWait();
            if (plate.isEmpty() || plate.get().isBlank()) return;
            newStatus = "ocupada";
            newPlate = plate.get().trim();
        } else {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Liberar a vaga '" + vaga.getSpotName() + "'?", ButtonType.YES, ButtonType.NO);
            confirm.setHeaderText(null);
            if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
            newStatus = "disponivel";
            newPlate = null;
        }

        setLoading(true);
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws ConnectionException {
                try (SocketConnector connector = new SocketConnector(host, port)) {
                    connector.connect();
                    return connector.send(new UpdateVagaRequest(token, vaga.getSpotName(),
                            null, newStatus, newPlate, null));
                }
            }
        };
        task.setOnSucceeded(event -> {
            setLoading(false);
            Response r = task.getValue();
            if (r.getStatusCode() == StatusCode.OK) handleRefreshVagas();
            else showError(r.getMessage());
        });
        task.setOnFailed(event -> { setLoading(false); showError("Falha ao atualizar vaga."); });
        TaskRunner.runInBackground(task, "main-estacionar-task");
    }

    @FXML
    private void handleDeleteVaga() {
        VagaData selected = vagasTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if ("ocupada".equals(selected.getStatus())) {
            showError("A vaga '" + selected.getSpotName() + "' está ocupada. Libere-a antes de remover.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remover a vaga '" + selected.getSpotName() + "'?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        setLoading(true);
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws ConnectionException {
                try (SocketConnector connector = new SocketConnector(host, port)) {
                    connector.connect();
                    return connector.send(new DeleteVagaRequest(token, selected.getSpotName()));
                }
            }
        };
        task.setOnSucceeded(event -> {
            setLoading(false);
            Response r = task.getValue();
            if (r.getStatusCode() == StatusCode.OK) handleRefreshVagas();
            else showError(r.getMessage());
        });
        task.setOnFailed(event -> { setLoading(false); showError("Falha ao remover vaga."); });
        TaskRunner.runInBackground(task, "main-delete-vaga-task");
    }

    @FXML
    private void handleFindVehicle() {
        String plate = findPlateField.getText().trim();
        if (plate.isBlank()) return;
        findVehicleResult.setText("Buscando...");

        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws ConnectionException {
                try (SocketConnector connector = new SocketConnector(host, port)) {
                    connector.connect();
                    return connector.send(new FindVehicleRequest(token, plate));
                }
            }
        };
        task.setOnSucceeded(event -> {
            Response r = task.getValue();
            if (r.getStatusCode() == StatusCode.OK) {
                try {
                    FindVehicleData data = JsonSupport.GSON.fromJson(r.getData(), FindVehicleData.class);
                    findVehicleResult.setText("Andar " + data.getFloor() + " · Vaga " + data.getSpotName() + " · " + data.getStatus());
                } catch (JsonSyntaxException e) {
                    findVehicleResult.setText("Resposta inválida.");
                }
            } else {
                findVehicleResult.setText(r.getMessage());
            }
        });
        task.setOnFailed(event -> findVehicleResult.setText("Falha ao buscar."));
        TaskRunner.runInBackground(task, "main-find-vehicle-task");
    }

    // ---- veículos ----

    @FXML
    private void handleRefreshVehicles() {
        setLoading(true);
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws ConnectionException {
                try (SocketConnector connector = new SocketConnector(host, port)) {
                    connector.connect();
                    return connector.send(new GetVehiclesRequest(token));
                }
            }
        };
        task.setOnSucceeded(event -> {
            setLoading(false);
            Response r = task.getValue();
            if (r.getStatusCode() == StatusCode.OK) {
                try {
                    List<VehicleData> list = JsonSupport.GSON.fromJson(r.getData(),
                            new TypeToken<List<VehicleData>>() {}.getType());
                    vehiclesTable.setItems(FXCollections.observableArrayList(list != null ? list : List.of()));
                } catch (JsonSyntaxException e) {
                    LOGGER.log(Level.WARNING, "resposta de getvehicles malformada", e);
                }
            } else if (r.getStatusCode() == StatusCode.UNAUTHORIZED) {
                app.showLogin();
            }
        });
        task.setOnFailed(event -> { setLoading(false); logWarning("falha ao carregar veículos", task.getException()); });
        TaskRunner.runInBackground(task, "main-get-vehicles-task");
    }

    @FXML
    private void handleCreateVehicle() {
        TextInputDialog brandDlg = new TextInputDialog();
        brandDlg.setTitle("Cadastrar veículo"); brandDlg.setHeaderText(null);
        brandDlg.setContentText("Marca:");
        Optional<String> brand = brandDlg.showAndWait();
        if (brand.isEmpty() || brand.get().isBlank()) return;

        TextInputDialog modelDlg = new TextInputDialog();
        modelDlg.setTitle("Modelo"); modelDlg.setHeaderText(null);
        modelDlg.setContentText("Modelo:");
        Optional<String> model = modelDlg.showAndWait();
        if (model.isEmpty() || model.get().isBlank()) return;

        ChoiceDialog<String> typeDlg = new ChoiceDialog<>("combustao", "combustao", "eletrico");
        typeDlg.setTitle("Tipo"); typeDlg.setHeaderText(null);
        typeDlg.setContentText("Tipo:");
        Optional<String> type = typeDlg.showAndWait();
        if (type.isEmpty()) return;

        TextInputDialog plateDlg = new TextInputDialog();
        plateDlg.setTitle("Placa"); plateDlg.setHeaderText(null);
        plateDlg.setContentText("Placa:");
        Optional<String> plate = plateDlg.showAndWait();
        if (plate.isEmpty() || plate.get().isBlank()) return;

        setLoading(true);
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws ConnectionException {
                try (SocketConnector connector = new SocketConnector(host, port)) {
                    connector.connect();
                    return connector.send(new CreateVehicleRequest(token,
                            brand.get().trim(), model.get().trim(), type.get(), plate.get().trim()));
                }
            }
        };
        task.setOnSucceeded(event -> {
            setLoading(false);
            Response r = task.getValue();
            if (r.getStatusCode() == StatusCode.CREATED) handleRefreshVehicles();
            else showError(r.getMessage());
        });
        task.setOnFailed(event -> { setLoading(false); showError("Falha ao cadastrar veículo."); });
        TaskRunner.runInBackground(task, "main-create-vehicle-task");
    }

    @FXML
    private void handleEditVehicle() {
        VehicleData selected = vehiclesTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog brandDlg = new TextInputDialog(selected.getBrand());
        brandDlg.setTitle("Editar veículo"); brandDlg.setHeaderText(null);
        brandDlg.setContentText("Marca:");
        Optional<String> brand = brandDlg.showAndWait();
        if (brand.isEmpty()) return;

        TextInputDialog modelDlg = new TextInputDialog(selected.getModel());
        modelDlg.setTitle("Modelo"); modelDlg.setHeaderText(null);
        modelDlg.setContentText("Modelo:");
        Optional<String> model = modelDlg.showAndWait();
        if (model.isEmpty()) return;

        ChoiceDialog<String> typeDlg = new ChoiceDialog<>(selected.getType(), "combustao", "eletrico");
        typeDlg.setTitle("Tipo"); typeDlg.setHeaderText(null);
        typeDlg.setContentText("Tipo:");
        Optional<String> type = typeDlg.showAndWait();
        if (type.isEmpty()) return;

        setLoading(true);
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws ConnectionException {
                try (SocketConnector connector = new SocketConnector(host, port)) {
                    connector.connect();
                    return connector.send(new UpdateVehicleRequest(token, selected.getLicensePlate(),
                            brand.get().trim(), model.get().trim(), type.get()));
                }
            }
        };
        task.setOnSucceeded(event -> {
            setLoading(false);
            Response r = task.getValue();
            if (r.getStatusCode() == StatusCode.OK) handleRefreshVehicles();
            else showError(r.getMessage());
        });
        task.setOnFailed(event -> { setLoading(false); showError("Falha ao editar veículo."); });
        TaskRunner.runInBackground(task, "main-edit-vehicle-task");
    }

    @FXML
    private void handleDeleteVehicle() {
        VehicleData selected = vehiclesTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remover o veículo placa '" + selected.getLicensePlate() + "'?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        setLoading(true);
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws ConnectionException {
                try (SocketConnector connector = new SocketConnector(host, port)) {
                    connector.connect();
                    return connector.send(new DeleteVehicleRequest(token, selected.getLicensePlate()));
                }
            }
        };
        task.setOnSucceeded(event -> {
            setLoading(false);
            Response r = task.getValue();
            if (r.getStatusCode() == StatusCode.OK) handleRefreshVehicles();
            else showError(r.getMessage());
        });
        task.setOnFailed(event -> { setLoading(false); showError("Falha ao remover veículo."); });
        TaskRunner.runInBackground(task, "main-delete-vehicle-task");
    }

    // ---- usuários (admin) ----

    @FXML
    private void handleRefreshUsers() {
        setLoading(true);
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws ConnectionException {
                try (SocketConnector connector = new SocketConnector(host, port)) {
                    connector.connect();
                    return connector.send(new GetUsersRequest(token));
                }
            }
        };
        task.setOnSucceeded(event -> {
            setLoading(false);
            Response r = task.getValue();
            if (r.getStatusCode() == StatusCode.OK) {
                try {
                    List<UserData> users = JsonSupport.GSON.fromJson(r.getData(),
                            new TypeToken<List<UserData>>() {}.getType());
                    allUsers.setAll(users != null ? users : List.of());
                } catch (JsonSyntaxException e) {
                    LOGGER.log(Level.WARNING, "resposta de getusers malformada", e);
                }
            } else if (r.getStatusCode() == StatusCode.UNAUTHORIZED) {
                app.showLogin();
            }
        });
        task.setOnFailed(event -> { setLoading(false); logWarning("falha ao carregar usuários", task.getException()); });
        TaskRunner.runInBackground(task, "main-get-users-task");
    }

    @FXML
    private void handleCreateUser() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Criar usuário");
        dlg.setHeaderText(null);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16, 16, 10, 16));

        TextField nameField = new TextField();
        nameField.setPromptText("Nome completo");
        nameField.setPrefWidth(220);
        TextField usernameField = new TextField();
        usernameField.setPromptText("username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Senha");
        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("user", "admin");
        roleBox.setValue("user");
        roleBox.setPrefWidth(220);

        grid.add(new Label("Nome:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Username:"), 0, 1);
        grid.add(usernameField, 1, 1);
        grid.add(new Label("Senha:"), 0, 2);
        grid.add(passwordField, 1, 2);
        grid.add(new Label("Papel:"), 0, 3);
        grid.add(roleBox, 1, 3);

        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dlg.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        String name = nameField.getText().trim();
        String newUsername = usernameField.getText().trim();
        String password = passwordField.getText();
        String roleStr = roleBox.getValue();

        if (name.isBlank() || newUsername.isBlank() || password.isBlank()) {
            showError("Preencha todos os campos obrigatórios.");
            return;
        }

        setLoading(true);
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws ConnectionException {
                try (SocketConnector connector = new SocketConnector(host, port)) {
                    connector.connect();
                    return connector.send(new RegisterRequest(name, newUsername, password, roleStr, token));
                }
            }
        };
        task.setOnSucceeded(event -> {
            setLoading(false);
            Response r = task.getValue();
            if (r.getStatusCode() == StatusCode.CREATED) {
                handleRefreshUsers();
                statusLabel.setText("Usuário '" + newUsername + "' criado.");
            } else {
                showError(r.getMessage());
            }
        });
        task.setOnFailed(event -> { setLoading(false); showError("Falha ao criar usuário."); });
        TaskRunner.runInBackground(task, "main-create-user-task");
    }

    @FXML
    private void handleEditUser() {
        UserData selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog nameDlg = new TextInputDialog(selected.getName() != null ? selected.getName() : "");
        nameDlg.setTitle("Editar nome");
        nameDlg.setHeaderText("Usuário: " + selected.getUsername());
        nameDlg.setContentText("Novo nome:");
        Optional<String> newName = nameDlg.showAndWait();
        if (newName.isEmpty() || newName.get().isBlank()) return;

        setLoading(true);
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws ConnectionException {
                try (SocketConnector connector = new SocketConnector(host, port)) {
                    connector.connect();
                    return connector.send(new UpdateUserNameRequest(token, selected.getUsername(), newName.get().trim()));
                }
            }
        };
        task.setOnSucceeded(event -> {
            setLoading(false);
            Response r = task.getValue();
            if (r.getStatusCode() == StatusCode.OK) { handleRefreshUsers(); statusLabel.setText("Nome atualizado."); }
            else showError(r.getMessage());
        });
        task.setOnFailed(event -> { setLoading(false); showError("Falha ao editar usuário."); });
        TaskRunner.runInBackground(task, "main-edit-user-task");
    }

    @FXML
    private void handleDeleteUserAdmin() {
        UserData selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Deletar o usuário '" + selected.getUsername() + "' permanentemente?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        setLoading(true);
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws ConnectionException {
                try (SocketConnector connector = new SocketConnector(host, port)) {
                    connector.connect();
                    return connector.send(new DeleteUserRequest(token, selected.getUsername()));
                }
            }
        };
        task.setOnSucceeded(event -> {
            setLoading(false);
            Response r = task.getValue();
            if (r.getStatusCode() == StatusCode.OK) { handleRefreshUsers(); statusLabel.setText("Usuário deletado."); }
            else showError(r.getMessage());
        });
        task.setOnFailed(event -> { setLoading(false); showError("Falha ao deletar usuário."); });
        TaskRunner.runInBackground(task, "main-delete-user-task");
    }

    // ---- heartbeat & navegação ----

    private void startHeartbeat() {
        heartbeat = new Timeline(new KeyFrame(Duration.seconds(30), event -> pingServer()));
        heartbeat.setCycleCount(Timeline.INDEFINITE);
        heartbeat.play();
    }

    private void stopHeartbeat() {
        if (heartbeat != null) heartbeat.stop();
    }

    private void pingServer() {
        if (!heartbeatRunning.compareAndSet(false, true)) return;
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws ConnectionException {
                try (SocketConnector connector = new SocketConnector(host, port)) {
                    connector.connect();
                    return connector.send(new GetUserRequest(token, username));
                }
            }
        };
        task.setOnSucceeded(event -> {
            heartbeatRunning.set(false);
            if (task.getValue().getStatusCode() == StatusCode.UNAUTHORIZED) {
                stopHeartbeat();
                app.showLogin();
            }
        });
        task.setOnFailed(event -> {
            heartbeatRunning.set(false);
            if (task.getException() instanceof ConnectionException) {
                stopHeartbeat();
                Alert alert = new Alert(Alert.AlertType.WARNING,
                        "O servidor ficou inacessível. Você será redirecionado para o login.", ButtonType.OK);
                alert.setHeaderText(null);
                alert.showAndWait();
                app.showLogin();
            }
        });
        TaskRunner.runInBackground(task, "main-heartbeat-task");
    }

    @FXML
    private void handleOpenProfile() {
        stopHeartbeat();
        app.showProfile(host, port, username, token);
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Tem certeza que deseja sair?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        setLoading(true);
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws ConnectionException {
                try (SocketConnector connector = new SocketConnector(host, port)) {
                    connector.connect();
                    return connector.send(new LogoutRequest(token));
                }
            }
        };
        stopHeartbeat();
        task.setOnSucceeded(event -> { setLoading(false); app.showLogin(); });
        task.setOnFailed(event -> { setLoading(false); app.showLogin(); });
        TaskRunner.runInBackground(task, "logout-task");
    }

    // ---- helpers ----

    private void setLoading(boolean loading) {
        this.loading = loading;
        progressIndicator.setVisible(loading);
        progressIndicator.setManaged(loading);
        if (!loading) statusLabel.setText("");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void logWarning(String msg, Throwable t) {
        LOGGER.log(Level.WARNING, msg, t);
    }
}
