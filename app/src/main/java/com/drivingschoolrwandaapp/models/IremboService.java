package com.drivingschoolrwandaapp.models;

public class IremboService {
    private String name;
    private int iconResId;

    public IremboService(String name, int iconResId) {
        this.name = name;
        this.iconResId = iconResId;
    }

    public String getName() {
        return name;
    }

    public int getIconResId() {
        return iconResId;
    }
}
