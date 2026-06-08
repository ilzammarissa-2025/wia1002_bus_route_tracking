package com.example.wia1002_bus_route_tracking.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.wia1002_bus_route_tracking.model.data_transfer_object.AutocompleteResponse;
import com.example.wia1002_bus_route_tracking.model.data_transfer_object.PopularRoutesResponse;
import com.example.wia1002_bus_route_tracking.model.data_transfer_object.SearchResponse;
import com.example.wia1002_bus_route_tracking.service.SearchOperations;

@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "*")
public class SearchController {

    private final SearchOperations searchService;

    public SearchController(SearchOperations searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/autocomplete")
    public AutocompleteResponse autocomplete(@RequestParam(name = "q", defaultValue = "") String query) {
        return new AutocompleteResponse(searchService.getAutocompleteSuggestions(query));
    }

    @GetMapping("/search")
    public SearchResponse search(@RequestParam(name = "q", defaultValue = "") String query) {
        return new SearchResponse(searchService.searchRoutes(query), searchService.getSearchSuggestions(query));
    }

    @GetMapping("/popular")
    public PopularRoutesResponse popular() {
        return new PopularRoutesResponse(searchService.getPopularRoutes());
    }
}
