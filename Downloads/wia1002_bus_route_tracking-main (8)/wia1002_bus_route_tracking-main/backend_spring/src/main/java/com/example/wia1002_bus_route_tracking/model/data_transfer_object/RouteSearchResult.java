package com.example.wia1002_bus_route_tracking.model.data_transfer_object;

public record RouteSearchResult(
        String shortName,
        String longName,
        int matchScore
) {
}
