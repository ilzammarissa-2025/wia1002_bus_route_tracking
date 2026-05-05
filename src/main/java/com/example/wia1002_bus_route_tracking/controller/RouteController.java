package com.example.wia1002_bus_route_tracking.controller;

import java.util.LinkedList;

import org.springframework.web.bind.annotation.*;

import com.example.wia1002_bus_route_tracking.model.entity.BusStop;
import com.example.wia1002_bus_route_tracking.service.RouteService;

//@RestController annotation auto return JSON for Flutter

@RestController
@RequestMapping("/api/routes")
@CrossOrigin(origins = "*") // Allow CORS for all origins (for development only, consider restricting in production)

public class RouteController {
    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    //GET : Example: http://localhost:8080/api/routes/T818/stops
    @GetMapping("/{routeId}/stops")
    public LinkedList<BusStop> getRouteStops(@PathVariable String routeId) {
        return routeService.getRouteStops(routeId);
    }
}
