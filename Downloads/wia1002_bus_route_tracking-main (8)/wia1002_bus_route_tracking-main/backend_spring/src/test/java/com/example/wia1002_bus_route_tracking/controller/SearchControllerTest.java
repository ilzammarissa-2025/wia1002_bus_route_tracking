package com.example.wia1002_bus_route_tracking.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.wia1002_bus_route_tracking.model.data_transfer_object.AutocompleteRouteSuggestion;
import com.example.wia1002_bus_route_tracking.model.data_transfer_object.PopularRouteSuggestion;
import com.example.wia1002_bus_route_tracking.model.data_transfer_object.RouteSearchResult;
import com.example.wia1002_bus_route_tracking.service.SearchOperations;

class SearchControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SearchController(new StubSearchOperations())).build();
    }

    @Test
    void autocompleteReturnsFrontendExpectedShape() throws Exception {
        mockMvc.perform(get("/api/search/autocomplete").param("q", "19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions[0].shortName").value("190"))
                .andExpect(jsonPath("$.suggestions[0].displayText").value("190 - Titiwangsa - Bukit Bintang"))
                .andExpect(jsonPath("$.suggestions[0].origin").value("Titiwangsa"))
                .andExpect(jsonPath("$.suggestions[0].destination").value("Bukit Bintang"));
    }

    @Test
    void searchReturnsFrontendExpectedShape() throws Exception {
        mockMvc.perform(get("/api/search/search").param("q", "190"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions[0].shortName").value("190"))
                .andExpect(jsonPath("$.suggestions[0].longName").value("Titiwangsa - Bukit Bintang"))
                .andExpect(jsonPath("$.suggestions[0].matchScore").value(100))
                .andExpect(jsonPath("$.searchSuggestions").isArray());
    }

    @Test
    void popularReturnsFrontendExpectedShape() throws Exception {
        mockMvc.perform(get("/api/search/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.popular[0].shortName").value("190"))
                .andExpect(jsonPath("$.popular[0].longName").value("Titiwangsa - Bukit Bintang"));
    }

    private static class StubSearchOperations implements SearchOperations {

        @Override
        public List<AutocompleteRouteSuggestion> getAutocompleteSuggestions(String query) {
            return List.of(new AutocompleteRouteSuggestion(
                    "190",
                    "190 - Titiwangsa - Bukit Bintang",
                    "Titiwangsa",
                    "Bukit Bintang"));
        }

        @Override
        public List<RouteSearchResult> searchRoutes(String query) {
            return List.of(new RouteSearchResult("190", "Titiwangsa - Bukit Bintang", 100));
        }

        @Override
        public List<String> getSearchSuggestions(String query) {
            return List.of();
        }

        @Override
        public List<PopularRouteSuggestion> getPopularRoutes() {
            return List.of(new PopularRouteSuggestion("190", "Titiwangsa - Bukit Bintang"));
        }
    }
}
