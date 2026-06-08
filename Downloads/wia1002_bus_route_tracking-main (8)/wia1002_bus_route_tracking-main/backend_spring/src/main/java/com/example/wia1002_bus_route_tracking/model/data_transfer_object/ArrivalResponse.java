package com.example.wia1002_bus_route_tracking.model.data_transfer_object;

public class ArrivalResponse {
    private String routeShortName;
    private String eta;
    private boolean isLive;

    public ArrivalResponse(String routeShortName, String eta, boolean isLive) {
        this.routeShortName = routeShortName;
        this.eta = eta;
        this.isLive = isLive;
    }

    public String getRouteShortName() {
        return routeShortName;
    }

    public String getEta() {
        return eta;
    }

    public boolean getIsLive() {
        return isLive;
    }
}
