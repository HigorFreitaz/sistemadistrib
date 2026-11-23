package br.edu.utfpr.sd.garagem.common.protocol;

public final class EnterVehicleData {

    private final String ticketId;
    private final String licensePlate;
    private final String spotName;
    private final int floor;
    private final String type;
    private final String entryTime;

    public EnterVehicleData(String ticketId, String licensePlate, String spotName,
                            int floor, String type, String entryTime) {
        this.ticketId = ticketId;
        this.licensePlate = licensePlate;
        this.spotName = spotName;
        this.floor = floor;
        this.type = type;
        this.entryTime = entryTime;
    }

    public String getTicketId() { return ticketId; }
    public String getLicensePlate() { return licensePlate; }
    public String getSpotName() { return spotName; }
    public int getFloor() { return floor; }
    public String getType() { return type; }
    public String getEntryTime() { return entryTime; }
}
