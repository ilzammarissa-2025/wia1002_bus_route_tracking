package com.example.wia1002_bus_route_tracking.service;

import java.util.List;

import com.example.wia1002_bus_route_tracking.model.data_transfer_object.AutocompleteRouteSuggestion;
import com.example.wia1002_bus_route_tracking.model.data_transfer_object.PopularRouteSuggestion;
import com.example.wia1002_bus_route_tracking.model.data_transfer_object.RouteSearchResult;

public interface SearchOperations {

    List<AutocompleteRouteSuggestion> getAutocompleteSuggestions(String query);

    List<RouteSearchResult> searchRoutes(String query);

    List<String> getSearchSuggestions(String query);

    List<PopularRouteSuggestion> getPopularRoutes();
}
