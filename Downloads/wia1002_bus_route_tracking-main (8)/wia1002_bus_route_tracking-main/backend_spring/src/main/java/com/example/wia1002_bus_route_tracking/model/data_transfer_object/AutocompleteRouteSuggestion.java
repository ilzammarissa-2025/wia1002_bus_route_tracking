package com.example.wia1002_bus_route_tracking.model.data_transfer_object;

public record AutocompleteRouteSuggestion(
        String shortName,
        String displayText,
        String origin,
        String destination
) {
}
