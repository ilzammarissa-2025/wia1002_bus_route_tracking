package com.example.wia1002_bus_route_tracking.model.data_transfer_object;

import java.util.List;

public record AutocompleteResponse(List<AutocompleteRouteSuggestion> suggestions) {
}
