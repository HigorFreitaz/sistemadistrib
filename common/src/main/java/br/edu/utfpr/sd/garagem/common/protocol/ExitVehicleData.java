package br.edu.utfpr.sd.garagem.common.protocol;

public final class ExitVehicleData {

    private final String ticketId;
    private final String licensePlate;
    private final String spotName;
    private final int floor;
    private final String entryTime;
    private final String exitTime;
    private final int durationMinutes;
    private final double amount;

    public ExitVehicleData(String ticketId, String licensePlate, String spotName,
                           int floor, String entryTime, String exitTime,
                           int durationMinutes, double amount) {
        this.ticketId = ticketId;
        this.licensePlate = licensePlate;
        this.spotName = spotName;
        this.floor = floor;
        this.entryTime = entryTime;
        this.exitTime = exitTime;
        this.durationMinutes = durationMinutes;
        this.amount = amount;
    }

    public String getTicketId() { return ticketId; }
    public String getLicensePlate() { return licensePlate; }
    public String getSpotName() { return spotName; }
    public int getFloor() { return floor; }
    public String getEntryTime() { return entryTime; }
    public String getExitTime() { return exitTime; }
    public int getDurationMinutes() { return durationMinutes; }
    public double getAmount() { return amount; }
}
