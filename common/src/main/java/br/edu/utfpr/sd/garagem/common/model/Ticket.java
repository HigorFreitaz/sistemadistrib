package br.edu.utfpr.sd.garagem.common.model;

public final class Ticket {

    private String ticketId;
    private String licensePlate;
    private String spotName;
    private int floor;
    private String type;
    private String entryTime;
    private String exitTime;
    private Integer durationMinutes;
    private Double amount;
    private boolean isActive;

    public Ticket() {}

    public Ticket(String ticketId, String licensePlate, String spotName, int floor,
                  String type, String entryTime) {
        this.ticketId = ticketId;
        this.licensePlate = licensePlate;
        this.spotName = spotName;
        this.floor = floor;
        this.type = type;
        this.entryTime = entryTime;
        this.isActive = true;
    }

    public String getTicketId() { return ticketId; }
    public String getLicensePlate() { return licensePlate; }
    public String getSpotName() { return spotName; }
    public int getFloor() { return floor; }
    public String getType() { return type; }
    public String getEntryTime() { return entryTime; }
    public String getExitTime() { return exitTime; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public Double getAmount() { return amount; }
    public boolean isActive() { return isActive; }

    public void setExitTime(String exitTime) { this.exitTime = exitTime; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public void setAmount(Double amount) { this.amount = amount; }
    public void setActive(boolean active) { this.isActive = active; }
}
