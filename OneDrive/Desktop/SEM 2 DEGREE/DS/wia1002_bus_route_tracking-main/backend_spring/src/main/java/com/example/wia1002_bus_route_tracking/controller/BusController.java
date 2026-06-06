package com.example.wia1002_bus_route_tracking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.wia1002_bus_route_tracking.service.SpatialLogicService;
import com.example.wia1002_bus_route_tracking.model.data_transfer_object.NearestBusResponse;
import com.example.wia1002_bus_route_tracking.model.data_transfer_object.ArrivalResponse;
import com.example.wia1002_bus_route_tracking.model.entity.BusPosition;
import com.example.wia1002_bus_route_tracking.model.entity.BusRoute;
import com.example.wia1002_bus_route_tracking.model.entity.StopTime;
import com.example.wia1002_bus_route_tracking.repository.BusPositionRepository;
import com.example.wia1002_bus_route_tracking.repository.BusRouteRepository;
import com.example.wia1002_bus_route_tracking.repository.StopTimeRepository;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.time.ZoneId; 

@RestController
@RequestMapping("/api/bus")
@CrossOrigin(origins = "*") 
public class BusController {

    private final SpatialLogicService spatialLogicService;
    private final BusPositionRepository busPositionRepository;
    private final BusRouteRepository busRouteRepository;
    private final StopTimeRepository stopTimeRepository;

    public BusController(SpatialLogicService spatialLogicService,
                         BusPositionRepository busPositionRepository,
                         BusRouteRepository busRouteRepository,
                         StopTimeRepository stopTimeRepository) {
        this.spatialLogicService = spatialLogicService;
        this.busPositionRepository = busPositionRepository;
        this.busRouteRepository = busRouteRepository;
        this.stopTimeRepository = stopTimeRepository;
    }

    @GetMapping("/nearest")
    public List<NearestBusResponse> getNearestStops(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "1000") double radius
    ) {
        return spatialLogicService.getNearestStops(lat, lon, radius);
    }

    @GetMapping("/locations") 
    public List<BusPosition> getAllLiveBuses() {
        return busPositionRepository.findAll();
    }

    // Unified Arrivals Endpoint
    @GetMapping("/arrivals")
    public ResponseEntity<List<ArrivalResponse>> getArrivals(
            @RequestParam String routeId, 
            @RequestParam String stopId
    ) {
        List<ArrivalResponse> finalArrivals = new ArrayList<>();
        String actualRouteId = routeId;

        Optional<BusRoute> routeOpt = busRouteRepository.findByRouteShortName(routeId);
        if (!routeOpt.isPresent()) {
            routeOpt = busRouteRepository.findFirstByRouteIdContaining(routeId);
        }
        if (routeOpt.isPresent()) {
            actualRouteId = routeOpt.get().getRouteId();
        }

        List<BusPosition> liveBuses = busPositionRepository.findByRoute_RouteId(actualRouteId);
        if (liveBuses.isEmpty() && !actualRouteId.equals(routeId)) {
            List<BusPosition> fallbackBuses = busPositionRepository.findByRoute_RouteId(routeId);
            if (!fallbackBuses.isEmpty()) {
                liveBuses = fallbackBuses;
                actualRouteId = routeId; 
            }
        }

        List<SpatialLogicService.BusArrival> liveArrivals = spatialLogicService.getFastestBusArrivals(actualRouteId, stopId, liveBuses);

        // DECISION: Live vs Scheduled
        if (liveArrivals != null && !liveArrivals.isEmpty()) {
            for (SpatialLogicService.BusArrival bus : liveArrivals) {
                String etaString = String.format("%.0f", bus.etaMin);
                finalArrivals.add(new ArrivalResponse(bus.busId, etaString, true));
            }
        } else {
            LocalTime now = LocalTime.now(ZoneId.of("Asia/Kuala_Lumpur"));// just in case the server use GMT+8.
            List<StopTime> scheduledTimes = stopTimeRepository.findUpcomingSchedules(stopId, actualRouteId, now);

            scheduledTimes.stream().forEach(schedule -> {
                String shortName = busRouteRepository.findById(schedule.getRouteId())
                        .map(route -> route.getRouteShortName())
                        .orElse(schedule.getRouteId());
                        
                finalArrivals.add(new ArrivalResponse(shortName, schedule.getArrivalTime().toString(), false));
            });
        }

        return ResponseEntity.ok(finalArrivals);
    }
}