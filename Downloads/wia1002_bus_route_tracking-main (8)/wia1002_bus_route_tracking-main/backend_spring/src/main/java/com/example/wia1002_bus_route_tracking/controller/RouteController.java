package com.example.wia1002_bus_route_tracking.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.wia1002_bus_route_tracking.model.entity.BusStop;
import com.example.wia1002_bus_route_tracking.model.data_transfer_object.CoordinateDTO;
import com.example.wia1002_bus_route_tracking.model.entity.BusRoute;
import com.example.wia1002_bus_route_tracking.repository.BusStopRepository;
import com.example.wia1002_bus_route_tracking.repository.BusRouteRepository;

@RestController
@RequestMapping("/api/routes")
@CrossOrigin(origins = "*") 
public class RouteController {
    
    private final BusStopRepository busStopRepository;
    private final BusRouteRepository busRouteRepository;

    // Inject repositories directly to implement the fallback logic
    public RouteController(BusStopRepository busStopRepository, BusRouteRepository busRouteRepository) {
        this.busStopRepository = busStopRepository;
        this.busRouteRepository = busRouteRepository;
    }

    @GetMapping("/{routeId}/stops")
    public List<BusStop> getRouteStops(@PathVariable String routeId) {
        String actualRouteId = routeId;

        // 1. Intelligent ID Resolution (Same logic as the Arrivals board!)
        Optional<BusRoute> routeOpt = busRouteRepository.findByRouteShortName(routeId);
        
        if (!routeOpt.isPresent()) {
            // Fallback to searching the internal GTFS ID strings
            routeOpt = busRouteRepository.findFirstByRouteIdContaining(routeId);
        }

        if (routeOpt.isPresent()) {
            actualRouteId = routeOpt.get().getRouteId();
        }

        // 2. Fetch the stops using the guaranteed internal ID
        return busStopRepository.findStopsByRouteId(actualRouteId);
    }

    @GetMapping("/{routeId}/path")
public ResponseEntity<List<CoordinateDTO>> getRoutePath(@PathVariable String routeId) {

    // Resolve short name or partial match to find correct internal GTFS ID
    Optional<BusRoute> routeOpt = busRouteRepository.findByRouteShortName(routeId);
    if (!routeOpt.isPresent()) {
        routeOpt = busRouteRepository.findFirstByRouteIdContaining(routeId);
    }

    if (!routeOpt.isPresent()) {
        return ResponseEntity.notFound().build();
    }

    BusRoute route = routeOpt.get();
    List<CoordinateDTO> pathPoints = new ArrayList<>();

    // Extract raw coordinates directly from the JTS LineString entity
    if (route.getRoutePath() != null) {
        for (org.locationtech.jts.geom.Coordinate coord : route.getRoutePath().getCoordinates()) {
            // JTS uses Coordinate(X, Y) which maps to Coordinate(Lon, Lat)
            pathPoints.add(new CoordinateDTO(coord.y, coord.x));
        }
    }

    return ResponseEntity.ok(pathPoints);
    }
}