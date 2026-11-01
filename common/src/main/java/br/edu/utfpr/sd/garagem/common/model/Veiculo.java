package br.edu.utfpr.sd.garagem.common.model;

public final class Veiculo {

    private String brand;
    private String model;
    private String type;
    private String licensePlate;
    private String ownerUsername;

    public Veiculo(String brand, String model, String type, String licensePlate, String ownerUsername) {
        this.brand = brand;
        this.model = model;
        this.type = type;
        this.licensePlate = licensePlate;
        this.ownerUsername = ownerUsername;
    }

    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public String getType() { return type; }
    public String getLicensePlate() { return licensePlate; }
    public String getOwnerUsername() { return ownerUsername; }

    public void setBrand(String brand) { this.brand = brand; }
    public void setModel(String model) { this.model = model; }
    public void setType(String type) { this.type = type; }
}
