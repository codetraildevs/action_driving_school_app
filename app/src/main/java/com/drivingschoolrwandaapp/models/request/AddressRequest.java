package com.drivingschoolrwandaapp.models.request;

public class AddressRequest {
    private String village;
    private String cell;
    private String sector;
    private String district;
    private String province;

    public AddressRequest(String village, String cell, String sector,
                          String district, String province) {
        this.village = village;
        this.cell = cell;
        this.sector = sector;
        this.district = district;
        this.province = province;
    }

    // Getters and setters
    public String getVillage() { return village; }
    public void setVillage(String village) { this.village = village; }

    public String getCell() { return cell; }
    public void setCell(String cell) { this.cell = cell; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
}