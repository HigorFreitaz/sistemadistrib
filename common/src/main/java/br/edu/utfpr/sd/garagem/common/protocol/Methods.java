package br.edu.utfpr.sd.garagem.common.protocol;

/**
 * Nomes das operações ({@code method}) do protocolo, sempre em minúsculas
 * conforme os requisitos não funcionais. Centralizado aqui para que o
 * dispatcher do servidor e as requisições do cliente nunca divirjam.
 */
public final class Methods {

    /** Autentica um usuário e abre uma sessão. */
    public static final String LOGIN = "login";

    /** Encerra a sessão associada a um token. */
    public static final String LOGOUT = "logout";

    /** Cadastra um novo usuário. */
    public static final String REGISTER = "register";

    /** Consulta os dados do próprio usuário autenticado. */
    public static final String GET_USER = "getuser";

    /** Atualiza o nome ({@code name}) do usuário autenticado. */
    public static final String UPDATE_USER_NAME = "updateusername";

    /** Atualiza a senha do usuário autenticado. */
    public static final String UPDATE_USER_PASSWORD = "updateuserpassword";

    /** Exclui o cadastro do usuário autenticado. */
    public static final String DELETE_USER = "deleteuser";

    // TODO EP-2: adicionar aqui os demais methods do protocolo (CRUD de
    // vagas/operacoes, CRUD admin) assim que forem definidos por Nathan e
    // Rafael na planilha de protocolo.

    private Methods() {
    }
}
