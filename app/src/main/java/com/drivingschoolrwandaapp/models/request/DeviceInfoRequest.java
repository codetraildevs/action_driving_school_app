package com.drivingschoolrwandaapp.models.request;

public class DeviceInfoRequest {
    private String physicalAddress;
    private String manufacturer;
    private String model;
    private String name;

    public DeviceInfoRequest(String physicalAddress, String manufacturer,
                             String model, String name) {
        this.physicalAddress = physicalAddress;
        this.manufacturer = manufacturer;
        this.model = model;
        this.name = name;
    }

    // Getters and setters
    public String getPhysicalAddress() { return physicalAddress; }
    public void setPhysicalAddress(String physicalAddress) { this.physicalAddress = physicalAddress; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
