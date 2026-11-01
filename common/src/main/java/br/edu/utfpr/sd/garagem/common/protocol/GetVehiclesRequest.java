package br.edu.utfpr.sd.garagem.common.protocol;

public final class GetVehiclesRequest {

    private final String method = Methods.GET_VEHICLES;
    private final Data data;

    public GetVehiclesRequest(String token) {
        this.data = new Data(token);
    }

    public String getMethod() { return method; }
    public Data getData() { return data; }
    public String getToken() { return data.token; }

    public static final class Data {
        private final String token;
        public Data(String token) { this.token = token; }
        public String getToken() { return token; }
    }
}
