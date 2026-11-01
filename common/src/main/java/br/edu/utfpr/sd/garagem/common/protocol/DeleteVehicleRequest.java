package br.edu.utfpr.sd.garagem.common.protocol;

public final class DeleteVehicleRequest {

    private final String method = Methods.DELETE_VEHICLE;
    private final Data data;

    public DeleteVehicleRequest(String token, String licensePlate) {
        this.data = new Data(token, licensePlate);
    }

    public String getMethod() { return method; }
    public Data getData() { return data; }
    public String getToken() { return data.token; }
    public String getLicensePlate() { return data.licensePlate; }

    public static final class Data {
        private final String token;
        private final String licensePlate;
        public Data(String token, String licensePlate) { this.token = token; this.licensePlate = licensePlate; }
        public String getToken() { return token; }
        public String getLicensePlate() { return licensePlate; }
    }
}
