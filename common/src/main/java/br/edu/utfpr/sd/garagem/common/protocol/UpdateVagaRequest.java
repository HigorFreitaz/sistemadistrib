package br.edu.utfpr.sd.garagem.common.protocol;

public final class UpdateVagaRequest {

    private final String method = Methods.UPDATE_VAGA;
    private final Data data;

    public UpdateVagaRequest(String token, String spotName, String newSpotName,
                             String status, String licensePlate, Integer floor) {
        this.data = new Data(token, spotName, newSpotName, status, licensePlate, floor);
    }

    public String getMethod() { return method; }
    public Data getData() { return data; }
    public String getToken() { return data.token; }
    public String getSpotName() { return data.spotName; }
    public String getNewSpotName() { return data.newSpotName; }
    public String getStatus() { return data.status; }
    public String getLicensePlate() { return data.licensePlate; }
    public Integer getFloor() { return data.floor; }

    public static final class Data {
        private final String token;
        private final String spotName;
        private final String newSpotName;
        private final String status;
        private final String licensePlate;
        private final Integer floor;

        public Data(String token, String spotName, String newSpotName,
                    String status, String licensePlate, Integer floor) {
            this.token = token;
            this.spotName = spotName;
            this.newSpotName = newSpotName;
            this.status = status;
            this.licensePlate = licensePlate;
            this.floor = floor;
        }

        public String getToken() { return token; }
        public String getSpotName() { return spotName; }
        public String getNewSpotName() { return newSpotName; }
        public String getStatus() { return status; }
        public String getLicensePlate() { return licensePlate; }
        public Integer getFloor() { return floor; }
    }
}
