package br.edu.utfpr.sd.garagem.common.model;

public final class Vaga {

    private String spotName;
    private String status;
    private int floor;
    private String type;
    private String licensePlate;

    public Vaga(String spotName, String status, int floor, String type, String licensePlate) {
        this.spotName = spotName;
        this.status = status;
        this.floor = floor;
        this.type = type;
        this.licensePlate = licensePlate;
    }

    public String getSpotName() { return spotName; }
    public String getStatus() { return status; }
    public int getFloor() { return floor; }
    public String getType() { return type; }
    public String getLicensePlate() { return licensePlate; }

    public void setStatus(String status) { this.status = status; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
    public void setSpotName(String spotName) { this.spotName = spotName; }
    public void setFloor(int floor) { this.floor = floor; }
    public void setType(String type) { this.type = type; }
}
