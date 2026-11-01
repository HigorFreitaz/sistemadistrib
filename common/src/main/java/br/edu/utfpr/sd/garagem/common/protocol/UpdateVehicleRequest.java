package br.edu.utfpr.sd.garagem.common.protocol;

public final class UpdateVehicleRequest {

    private final String method = Methods.UPDATE_VEHICLE;
    private final Data data;

    public UpdateVehicleRequest(String token, String licensePlate, String brand, String model, String type) {
        this.data = new Data(token, licensePlate, brand, model, type);
    }

    public String getMethod() { return method; }
    public Data getData() { return data; }
    public String getToken() { return data.token; }
    public String getLicensePlate() { return data.licensePlate; }
    public String getBrand() { return data.brand; }
    public String getModel() { return data.model; }
    public String getType() { return data.type; }

    public static final class Data {
        private final String token;
        private final String licensePlate;
        private final String brand;
        private final String model;
        private final String type;

        public Data(String token, String licensePlate, String brand, String model, String type) {
            this.token = token;
            this.licensePlate = licensePlate;
            this.brand = brand;
            this.model = model;
            this.type = type;
        }

        public String getToken() { return token; }
        public String getLicensePlate() { return licensePlate; }
        public String getBrand() { return brand; }
        public String getModel() { return model; }
        public String getType() { return type; }
    }
}
