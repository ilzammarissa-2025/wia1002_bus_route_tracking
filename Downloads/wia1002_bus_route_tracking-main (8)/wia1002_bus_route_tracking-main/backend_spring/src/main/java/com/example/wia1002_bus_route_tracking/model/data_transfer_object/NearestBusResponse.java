package com.example.wia1002_bus_route_tracking.model.data_transfer_object;

public class NearestBusResponse {
    private String stopName;
    private double latitude;
    private double longitude;
    private double distanceMeters;
    private String stopId;

    public NearestBusResponse(String stopId, String stopName, double latitude, double longitude, double distanceMeters) {
        this.stopId = stopId;
        this.stopName = stopName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceMeters = distanceMeters;
    }

    public String getStopName() {
        return stopName;
    }

    public String getStopId() {
        return stopId;
    }

    public void setStopid(String stopId) {
        this.stopId = stopId;
    }

    public void setStopName(String stopName) {
        this.stopName = stopName;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }
}