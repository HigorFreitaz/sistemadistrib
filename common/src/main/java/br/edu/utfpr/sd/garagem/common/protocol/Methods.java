package br.edu.utfpr.sd.garagem.common.protocol;

public final class Methods {

    public static final String LOGIN = "login";
    public static final String LOGOUT = "logout";
    public static final String REGISTER = "register";
    public static final String GET_USER = "getuser";
    public static final String UPDATE_USER_NAME = "updateusername";
    public static final String UPDATE_USER_PASSWORD = "updateuserpassword";
    public static final String DELETE_USER = "deleteuser";

    // EP-2: vagas
    public static final String CREATE_VAGA = "createvaga";
    public static final String GET_VAGAS = "getvagas";
    public static final String UPDATE_VAGA = "updatevaga";
    public static final String DELETE_VAGA = "deletevaga";

    // EP-2: veículos
    public static final String CREATE_VEHICLE = "createvehicle";
    public static final String GET_VEHICLES = "getvehicles";
    public static final String GET_VEHICLE = "getvehicle";
    public static final String UPDATE_VEHICLE = "updatevehicle";
    public static final String DELETE_VEHICLE = "deletevehicle";
    public static final String FIND_VEHICLE = "findvehicle";

    private Methods() {}
}
