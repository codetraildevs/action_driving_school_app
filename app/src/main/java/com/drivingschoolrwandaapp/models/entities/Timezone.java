package com.drivingschoolrwandaapp.models.entities;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Timezone implements Serializable {
    @SerializedName("id")
    private int id;

    @SerializedName("timezoneName")
    private String timezoneName;

    @SerializedName("utcOffset")
    private String utcOffset;

    @SerializedName("countryName")
    private String countryName;

    // Getters and Setters
    public int getId() { return id; }
    public String getTimezoneName() { return timezoneName; }
    public String getUtcOffset() { return utcOffset; }
    public String getCountryName() { return countryName; }
}

