package br.edu.utfpr.sd.garagem.common.protocol;

public final class VehicleData {

    private final String brand;
    private final String model;
    private final String type;
    private final String licensePlate;

    public VehicleData(String brand, String model, String type, String licensePlate) {
        this.brand = brand;
        this.model = model;
        this.type = type;
        this.licensePlate = licensePlate;
    }

    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public String getType() { return type; }
    public String getLicensePlate() { return licensePlate; }
}
