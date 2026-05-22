package com.example.wia1002_bus_route_tracking.controller;

//export below functions to flutter
import org.springframework.web.bind.annotation.*;
import com.example.wia1002_bus_route_tracking.service.SpatialLogicService;
import com.example.wia1002_bus_route_tracking.model.data_transfer_object.NearestBusResponse;
import com.example.wia1002_bus_route_tracking.model.entity.BusPosition;
import com.example.wia1002_bus_route_tracking.repository.BusPositionRepository;

import java.util.List;

@RestController
@RequestMapping("/api/bus")
@CrossOrigin(origins = "*") // Allows Flutter to call this API during development

public class BusController {
    private final SpatialLogicService spatialLogicService;
    private final BusPositionRepository busPositionRepository;

    public BusController(SpatialLogicService spatialLogicService, BusPositionRepository busPositionRepository) {
        this.spatialLogicService = spatialLogicService;
        this.busPositionRepository = busPositionRepository;
    }

    //find nearest stops
    @GetMapping("/nearest")
    public List<NearestBusResponse> getNearestStops(
        @RequestParam double lat,
        @RequestParam double lon,
        @RequestParam (defaultValue = "1000") double radius
    ) {
        return spatialLogicService.getNearestStops(lat, lon, radius);
    }

    @GetMapping("/arrivals")
    public List<SpatialLogicService.BusArrival> getArrivals(
        @RequestParam String routeId,
        @RequestParam String stopId
    ) {
        //fetch all live buses currently driving to specific route
        List<BusPosition> liveBuses = busPositionRepository.findByRoute_RouteId(routeId);

        //pass them to your 
        return spatialLogicService.getFastestBusArrivals(routeId, stopId, liveBuses);
    }
    
}
