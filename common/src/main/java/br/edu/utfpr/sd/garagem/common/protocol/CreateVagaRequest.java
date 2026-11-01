package br.edu.utfpr.sd.garagem.common.protocol;

public final class CreateVagaRequest {

    private final String method = Methods.CREATE_VAGA;
    private final Data data;

    public CreateVagaRequest(String token, String spotName, int floor, String type) {
        this.data = new Data(token, spotName, floor, type);
    }

    public String getMethod() { return method; }
    public Data getData() { return data; }
    public String getToken() { return data.token; }
    public String getSpotName() { return data.spotName; }
    public int getFloor() { return data.floor; }
    public String getType() { return data.type; }

    public static final class Data {
        private final String token;
        private final String spotName;
        private final int floor;
        private final String type;

        public Data(String token, String spotName, int floor, String type) {
            this.token = token;
            this.spotName = spotName;
            this.floor = floor;
            this.type = type;
        }

        public String getToken() { return token; }
        public String getSpotName() { return spotName; }
        public int getFloor() { return floor; }
        public String getType() { return type; }
    }
}
