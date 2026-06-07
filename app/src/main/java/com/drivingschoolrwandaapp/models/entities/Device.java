package com.drivingschoolrwandaapp.models.entities;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Device implements Serializable {
    @SerializedName("physicalAddress")
    private String physicalAddress;

    @SerializedName("manufacturer")
    private String manufacturer;

    @SerializedName("model")
    private String model;

    @SerializedName("name")
    private String name;


    public String getPhysicalAddress() { return physicalAddress; }
    public String getManufacturer() { return manufacturer; }
    public String getModel() { return model; }
    public String getName() { return name; }

    public void setPhysicalAddress(String physicalAddress) { this.physicalAddress = physicalAddress; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public void setModel(String model) { this.model = model; }
    public void setName(String name) { this.name = name; }

}