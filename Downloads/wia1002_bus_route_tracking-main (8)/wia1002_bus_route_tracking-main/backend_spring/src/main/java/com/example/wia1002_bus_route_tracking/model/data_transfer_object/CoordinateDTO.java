package com.example.wia1002_bus_route_tracking.model.data_transfer_object;

public class CoordinateDTO {
    private double latitude;
    private double longitude;

    public CoordinateDTO(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}