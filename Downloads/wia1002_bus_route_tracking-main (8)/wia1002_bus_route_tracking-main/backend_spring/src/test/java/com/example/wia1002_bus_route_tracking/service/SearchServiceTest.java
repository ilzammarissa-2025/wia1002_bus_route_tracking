package com.example.wia1002_bus_route_tracking.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.wia1002_bus_route_tracking.model.data_transfer_object.AutocompleteRouteSuggestion;
import com.example.wia1002_bus_route_tracking.model.data_transfer_object.PopularRouteSuggestion;
import com.example.wia1002_bus_route_tracking.model.data_transfer_object.RouteSearchResult;
import com.example.wia1002_bus_route_tracking.model.entity.BusRoute;

class SearchServiceTest {

    @Test
    void autocompleteRanksRouteNumberMatchesBeforeWeakerMatches() {
        SearchService searchService = searchServiceWith(List.of(
                route("r1", "119", "City Centre - Area 19"),
                route("r2", "190", "Titiwangsa - Bukit Bintang"),
                route("r3", "851", "Section 19 - Hab Pasar Seni")
        ));

        List<AutocompleteRouteSuggestion> suggestions = searchService.getAutocompleteSuggestions("19");

        assertThat(suggestions)
                .extracting(AutocompleteRouteSuggestion::shortName)
                .containsExactly("190", "119", "851");
    }

    @Test
    void searchIsCaseInsensitive() {
        SearchService searchService = searchServiceWith(List.of(
                route("r1", "190", "Titiwangsa - Bukit Bintang"),
                route("r2", "851", "Hab Pasar Seni - Petaling Jaya")
        ));

        List<RouteSearchResult> results = searchService.searchRoutes("tItI");

        assertThat(results)
                .extracting(RouteSearchResult::shortName)
                .containsExactly("190");
    }

    @Test
    void blankAndTooShortQueriesReturnEmptyResults() {
        SearchService searchService = searchServiceWith(List.of(route("r1", "190", "Titiwangsa - Bukit Bintang")));

        assertThat(searchService.getAutocompleteSuggestions("")).isEmpty();
        assertThat(searchService.getAutocompleteSuggestions("1")).isEmpty();
        assertThat(searchService.searchRoutes("1")).isEmpty();
    }

    @Test
    void unknownQueryReturnsEmptyResults() {
        SearchService searchService = searchServiceWith(List.of(route("r1", "190", "Titiwangsa - Bukit Bintang")));

        assertThat(searchService.getAutocompleteSuggestions("zz")).isEmpty();
        assertThat(searchService.searchRoutes("zz")).isEmpty();
    }

    @Test
    void popularRoutesReturnsFirstEightValidRoutesSortedByShortName() {
        SearchService searchService = searchServiceWith(List.of(
                route("r10", "10", "Ten"),
                route("r2", "2", "Two"),
                route("r5", "5", "Five"),
                route("blank", " ", "Blank"),
                route("r1", "1", "One"),
                route("r8", "8", "Eight"),
                route("r4", "4", "Four"),
                route("r3", "3", "Three"),
                route("r9", "9", "Nine"),
                route("r6", "6", "Six"),
                route("r7", "7", "Seven")
        ));

        List<PopularRouteSuggestion> popular = searchService.getPopularRoutes();

        assertThat(popular)
                .extracting(PopularRouteSuggestion::shortName)
                .containsExactly("1", "2", "3", "4", "5", "6", "7", "8");
    }

    private static SearchService searchServiceWith(List<BusRoute> routes) {
        return new SearchService(() -> routes);
    }

    private static BusRoute route(String routeId, String shortName, String longName) {
        BusRoute route = new BusRoute();
        route.setRouteId(routeId);
        route.setRouteShortName(shortName);
        route.setRouteLongName(longName);
        return route;
    }
}
