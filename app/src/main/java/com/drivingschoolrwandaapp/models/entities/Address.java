package com.drivingschoolrwandaapp.models.entities;
import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class Address implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("village")
    private String village;

    @SerializedName("cell")
    private String cell;

    @SerializedName("sector")
    private String sector;

    @SerializedName("district")
    private String district;

    @SerializedName("province")
    private String province;

    // Getters and Setters
    public int getId() { return id; }
    public String getVillage() { return village; }
    public String getCell() { return cell; }
    public String getSector() { return sector; }
    public String getDistrict() { return district; }
    public String getProvince() { return province; }

    public String getFullAddress() {
        return village + ", " + cell + ", " + sector + ", " + district + ", " + province;
    }
}