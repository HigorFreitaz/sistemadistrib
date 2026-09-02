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

    // TODO EP-2: adicionar aqui os demais methods do protocolo (cadastro de
    // usuario, CRUD de vagas/operacoes, CRUD admin) assim que forem
    // definidos por Nathan e Rafael na planilha de protocolo.

    private Methods() {
    }
}
