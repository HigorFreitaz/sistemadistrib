package br.edu.utfpr.sd.garagem.common.protocol;

public final class VagaData {

    private final int floor;
    private final String spotName;
    private final String status;
    private final String licensePlate;
    private final String type;

    public VagaData(int floor, String spotName, String status, String licensePlate, String type) {
        this.floor = floor;
        this.spotName = spotName;
        this.status = status;
        this.licensePlate = licensePlate;
        this.type = type;
    }

    public int getFloor() { return floor; }
    public String getSpotName() { return spotName; }
    public String getStatus() { return status; }
    public String getLicensePlate() { return licensePlate; }
    public String getType() { return type; }
}
