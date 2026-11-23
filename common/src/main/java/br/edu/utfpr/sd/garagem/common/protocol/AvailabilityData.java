package br.edu.utfpr.sd.garagem.common.protocol;

public final class AvailabilityData {

    private final int floor;
    private final int totalSpots;
    private final int availableSpots;
    private final int availableEletrico;
    private final int availableCombustao;

    public AvailabilityData(int floor, int totalSpots, int availableSpots,
                            int availableEletrico, int availableCombustao) {
        this.floor = floor;
        this.totalSpots = totalSpots;
        this.availableSpots = availableSpots;
        this.availableEletrico = availableEletrico;
        this.availableCombustao = availableCombustao;
    }

    public int getFloor() { return floor; }
    public int getTotalSpots() { return totalSpots; }
    public int getAvailableSpots() { return availableSpots; }
    public int getAvailableEletrico() { return availableEletrico; }
    public int getAvailableCombustao() { return availableCombustao; }
}
