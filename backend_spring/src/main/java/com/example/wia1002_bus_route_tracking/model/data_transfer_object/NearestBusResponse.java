package com.example.wia1002_bus_route_tracking.model.data_transfer_object;

public class NearestBusResponse {
    //DTO(imagine as struct, use to sned the entire BusStop database entity will carsh JSON converter, only extract what Flutter needed)

    private String stopName;
    private double latitude;
    private double longitude;
    private double distanceMeters;

    public NearestBusResponse(String stopName, double latitude, double longitude, double distanceMeters) {
        this.stopName = stopName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceMeters = distanceMeters;
    }

    public String getStopName() {
        return stopName;
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
