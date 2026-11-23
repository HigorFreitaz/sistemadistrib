package br.edu.utfpr.sd.garagem.common.protocol;

public final class FindVehicleData {

    private final int floor;
    private final String spotName;
    private final String status;
    private final String ticketId;
    private final String entryTime;

    public FindVehicleData(int floor, String spotName, String status,
                           String ticketId, String entryTime) {
        this.floor = floor;
        this.spotName = spotName;
        this.status = status;
        this.ticketId = ticketId;
        this.entryTime = entryTime;
    }

    public int getFloor() { return floor; }
    public String getSpotName() { return spotName; }
    public String getStatus() { return status; }
    public String getTicketId() { return ticketId; }
    public String getEntryTime() { return entryTime; }
}
