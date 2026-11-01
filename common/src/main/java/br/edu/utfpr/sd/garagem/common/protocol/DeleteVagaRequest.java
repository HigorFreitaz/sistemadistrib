package br.edu.utfpr.sd.garagem.common.protocol;

public final class DeleteVagaRequest {

    private final String method = Methods.DELETE_VAGA;
    private final Data data;

    public DeleteVagaRequest(String token, String spotName) {
        this.data = new Data(token, spotName);
    }

    public String getMethod() { return method; }
    public Data getData() { return data; }
    public String getToken() { return data.token; }
    public String getSpotName() { return data.spotName; }

    public static final class Data {
        private final String token;
        private final String spotName;
        public Data(String token, String spotName) { this.token = token; this.spotName = spotName; }
        public String getToken() { return token; }
        public String getSpotName() { return spotName; }
    }
}
