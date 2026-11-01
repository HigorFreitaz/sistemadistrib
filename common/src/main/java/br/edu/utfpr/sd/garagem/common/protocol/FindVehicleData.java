package br.edu.utfpr.sd.garagem.common.protocol;

public final class FindVehicleData {

    private final int floor;
    private final String spotName;
    private final String status;

    public FindVehicleData(int floor, String spotName, String status) {
        this.floor = floor;
        this.spotName = spotName;
        this.status = status;
    }

    public int getFloor() { return floor; }
    public String getSpotName() { return spotName; }
    public String getStatus() { return status; }
}
