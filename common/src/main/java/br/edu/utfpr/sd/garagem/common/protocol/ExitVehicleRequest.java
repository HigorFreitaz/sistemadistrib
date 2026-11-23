package br.edu.utfpr.sd.garagem.common.protocol;

public final class ExitVehicleRequest {

    private final String method = Methods.EXIT_VEHICLE;
    private final Data data;

    public ExitVehicleRequest(String token, String licensePlate, int durationMinutes) {
        this.data = new Data(token, licensePlate, durationMinutes);
    }

    public String getMethod() { return method; }
    public Data getData() { return data; }
    public String getToken() { return data.token; }
    public String getLicensePlate() { return data.licensePlate; }
    public int getDurationMinutes() { return data.durationMinutes; }

    public static final class Data {
        private final String token;
        private final String licensePlate;
        private final int durationMinutes;

        public Data(String token, String licensePlate, int durationMinutes) {
            this.token = token;
            this.licensePlate = licensePlate;
            this.durationMinutes = durationMinutes;
        }
    }
}
