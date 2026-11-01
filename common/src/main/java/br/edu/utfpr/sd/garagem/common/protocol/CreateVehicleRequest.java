package br.edu.utfpr.sd.garagem.common.protocol;

public final class CreateVehicleRequest {

    private final String method = Methods.CREATE_VEHICLE;
    private final Data data;

    public CreateVehicleRequest(String token, String brand, String model, String type, String licensePlate) {
        this.data = new Data(token, brand, model, type, licensePlate);
    }

    public String getMethod() { return method; }
    public Data getData() { return data; }
    public String getToken() { return data.token; }
    public String getBrand() { return data.brand; }
    public String getModel() { return data.model; }
    public String getType() { return data.type; }
    public String getLicensePlate() { return data.licensePlate; }

    public static final class Data {
        private final String token;
        private final String brand;
        private final String model;
        private final String type;
        private final String licensePlate;

        public Data(String token, String brand, String model, String type, String licensePlate) {
            this.token = token;
            this.brand = brand;
            this.model = model;
            this.type = type;
            this.licensePlate = licensePlate;
        }

        public String getToken() { return token; }
        public String getBrand() { return brand; }
        public String getModel() { return model; }
        public String getType() { return type; }
        public String getLicensePlate() { return licensePlate; }
    }
}
