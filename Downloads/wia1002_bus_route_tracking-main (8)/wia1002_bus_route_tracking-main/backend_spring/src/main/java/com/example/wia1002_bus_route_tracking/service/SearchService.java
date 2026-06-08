package com.example.wia1002_bus_route_tracking.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.example.wia1002_bus_route_tracking.model.data_transfer_object.AutocompleteRouteSuggestion;
import com.example.wia1002_bus_route_tracking.model.data_transfer_object.PopularRouteSuggestion;
import com.example.wia1002_bus_route_tracking.model.data_transfer_object.RouteSearchResult;
import com.example.wia1002_bus_route_tracking.model.entity.BusRoute;
import com.example.wia1002_bus_route_tracking.repository.BusRouteRepository;

@Service
public class SearchService implements SearchOperations {

    private static final int MIN_QUERY_LENGTH = 2;
    private static final int AUTOCOMPLETE_LIMIT = 8;
    private static final int SEARCH_LIMIT = 10;
    private static final int POPULAR_LIMIT = 8;
    private static final Pattern ROUTE_NAME_SPLITTER = Pattern.compile("\\s*(?:->| - | to )\\s*", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private final Supplier<List<BusRoute>> routeSupplier;

    public SearchService(BusRouteRepository busRouteRepository) {
        this(busRouteRepository::findAll);
    }

    SearchService(Supplier<List<BusRoute>> routeSupplier) {
        this.routeSupplier = routeSupplier;
    }

    @Override
    public List<AutocompleteRouteSuggestion> getAutocompleteSuggestions(String query) {
        return rankedRoutes(query).stream()
                .limit(AUTOCOMPLETE_LIMIT)
                .map(match -> toAutocompleteSuggestion(match.route()))
                .toList();
    }

    @Override
    public List<RouteSearchResult> searchRoutes(String query) {
        return rankedRoutes(query).stream()
                .limit(SEARCH_LIMIT)
                .map(match -> new RouteSearchResult(
                        match.route().shortName(),
                        displayLongName(match.route()),
                        match.score()))
                .toList();
    }

    @Override
    public List<String> getSearchSuggestions(String query) {
        return List.of();
    }

    @Override
    public List<PopularRouteSuggestion> getPopularRoutes() {
        return buildSearchIndex().stream()
                .sorted((left, right) -> compareRouteShortNames(left.shortName(), right.shortName()))
                .limit(POPULAR_LIMIT)
                .map(route -> new PopularRouteSuggestion(route.shortName(), displayLongName(route)))
                .toList();
    }

    private List<RouteMatch> rankedRoutes(String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.length() < MIN_QUERY_LENGTH) {
            return List.of();
        }

        return buildSearchIndex().stream()
                .map(route -> matchRoute(route, normalizedQuery))
                .flatMap(Optional::stream)
                .sorted(Comparator.<RouteMatch>comparingInt(RouteMatch::score).reversed()
                        .thenComparing((left, right) -> compareRouteShortNames(
                                left.route().shortName(),
                                right.route().shortName())))
                .toList();
    }

    private List<RouteSearchEntry> buildSearchIndex() {
        return routeSupplier.get().stream()
                .filter(route -> hasText(route.getRouteShortName()))
                .map(route -> {
                    String shortName = clean(route.getRouteShortName());
                    String longName = clean(route.getRouteLongName());
                    return new RouteSearchEntry(shortName, longName, normalize(shortName), normalize(longName));
                })
                .toList();
    }

    private Optional<RouteMatch> matchRoute(RouteSearchEntry route, String normalizedQuery) {
        int score = score(route, normalizedQuery);
        if (score <= 0) {
            return Optional.empty();
        }
        return Optional.of(new RouteMatch(route, score));
    }

    private int score(RouteSearchEntry route, String normalizedQuery) {
        String shortName = route.normalizedShortName();
        String longName = route.normalizedLongName();

        if (shortName.equals(normalizedQuery)) {
            return 100;
        }
        if (shortName.startsWith(normalizedQuery)) {
            return Math.max(90 - (shortName.length() - normalizedQuery.length()), 80);
        }
        int shortNameIndex = shortName.indexOf(normalizedQuery);
        if (shortNameIndex >= 0) {
            return Math.max(75 - shortNameIndex, 70);
        }
        if (longName.startsWith(normalizedQuery)) {
            return 65;
        }
        int longNameIndex = longName.indexOf(normalizedQuery);
        if (longNameIndex >= 0) {
            return Math.max(55 - longNameIndex, 45);
        }
        return 0;
    }

    private AutocompleteRouteSuggestion toAutocompleteSuggestion(RouteSearchEntry route) {
        RouteNameParts parts = splitRouteName(route);
        return new AutocompleteRouteSuggestion(
                route.shortName(),
                displayText(route),
                parts.origin(),
                parts.destination());
    }

    private RouteNameParts splitRouteName(RouteSearchEntry route) {
        String longName = displayLongName(route);
        String[] parts = ROUTE_NAME_SPLITTER.split(longName, 2);
        if (parts.length == 2 && hasText(parts[0]) && hasText(parts[1])) {
            return new RouteNameParts(clean(parts[0]), clean(parts[1]));
        }
        return new RouteNameParts("Route " + route.shortName(), longName);
    }

    private String displayText(RouteSearchEntry route) {
        return route.shortName() + " - " + displayLongName(route);
    }

    private String displayLongName(RouteSearchEntry route) {
        if (hasText(route.longName())) {
            return route.longName();
        }
        return "Route " + route.shortName();
    }

    private static String normalize(String value) {
        return SPACES.matcher(clean(value).toLowerCase(Locale.ROOT)).replaceAll(" ");
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static int compareRouteShortNames(String left, String right) {
        String cleanLeft = clean(left);
        String cleanRight = clean(right);
        boolean leftNumeric = isNumeric(cleanLeft);
        boolean rightNumeric = isNumeric(cleanRight);

        if (leftNumeric && rightNumeric) {
            String strippedLeft = stripLeadingZeroes(cleanLeft);
            String strippedRight = stripLeadingZeroes(cleanRight);
            int lengthCompare = Integer.compare(strippedLeft.length(), strippedRight.length());
            if (lengthCompare != 0) {
                return lengthCompare;
            }
            int numericCompare = strippedLeft.compareTo(strippedRight);
            if (numericCompare != 0) {
                return numericCompare;
            }
        }

        return cleanLeft.compareToIgnoreCase(cleanRight);
    }

    private static boolean isNumeric(String value) {
        return !value.isEmpty() && value.chars().allMatch(Character::isDigit);
    }

    private static String stripLeadingZeroes(String value) {
        String stripped = value.replaceFirst("^0+", "");
        return stripped.isEmpty() ? "0" : stripped;
    }

    private record RouteSearchEntry(
            String shortName,
            String longName,
            String normalizedShortName,
            String normalizedLongName
    ) {
    }

    private record RouteMatch(RouteSearchEntry route, int score) {
    }

    private record RouteNameParts(String origin, String destination) {
    }
}
