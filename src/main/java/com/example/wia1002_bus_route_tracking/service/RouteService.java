package com.example.wia1002_bus_route_tracking.service;

import com.example.wia1002_bus_route_tracking.model.entity.BusStop;
import com.example.wia1002_bus_route_tracking.repository.BusStopRepository;
import org.springframework.stereotype.Service;
import java.util.LinkedList;
import java.util.List;

@Service
public class RouteService {

    private final BusStopRepository busStopRepository;

    public RouteService(BusStopRepository busStopRepository) {
        this.busStopRepository = busStopRepository;
    }

    public LinkedList<BusStop> getRouteStops(String routeId) {
        List<BusStop> filteredStops = busStopRepository.findStopsByRouteId(routeId);
        return new LinkedList<>(filteredStops);
    }
}
