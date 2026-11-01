package br.edu.utfpr.sd.garagem.common.protocol;

public final class GetUsersRequest {

    private final String method = Methods.GET_USERS;
    private final Data data;

    public GetUsersRequest(String token) {
        this.data = new Data(token);
    }

    public String getMethod() { return method; }
    public Data getData() { return data; }
    public String getToken() { return data.token; }

    public static final class Data {
        private final String token;

        public Data(String token) {
            this.token = token;
        }

        public String getToken() { return token; }
    }
}
