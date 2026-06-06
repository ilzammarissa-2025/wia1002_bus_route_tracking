package com.example.wia1002_bus_route_tracking.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import org.springframework.stereotype.Service;

import com.example.wia1002_bus_route_tracking.model.data_transfer_object.NearestBusResponse;
import com.example.wia1002_bus_route_tracking.model.entity.BusPosition;
import com.example.wia1002_bus_route_tracking.model.entity.BusStop;
import com.example.wia1002_bus_route_tracking.repository.BusRouteRepository;
import com.example.wia1002_bus_route_tracking.repository.BusStopRepository;

@Service
public class SpatialLogicService {
    private final BusStopRepository busStopRepository;
    private final BusRouteRepository busRouteRepository;

    public SpatialLogicService(BusStopRepository busStopRepository, BusRouteRepository busRouteRepository) {
        this.busStopRepository = busStopRepository;
        this.busRouteRepository = busRouteRepository;
    }

    public static class BusArrival {
        public String busId;
        public double etaMin;

        public BusArrival(String busId, double etaMin) {
            this.busId = busId;
            this.etaMin = etaMin;
        }
    }

    public List<NearestBusResponse> getNearestStops(double userLat, double userLon, double radiusMeters) {
        List<BusStop> nearestStops = busStopRepository.findStopsWithinRadius(userLat, userLon, radiusMeters);
        List<NearestBusResponse> responses = new ArrayList<>();
        for (BusStop stop : nearestStops) {
            double distance = calculateHaversineDistance(userLat, userLon, stop.getLatitude(), stop.getLongitude());
            NearestBusResponse dto = new NearestBusResponse(
                stop.getStopId(),
                stop.getStopName(), 
                stop.getLatitude(), 
                stop.getLongitude(),
                distance
            );
            responses.add(dto);
        }
        return responses;
    }

    public double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371000; 
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    public List<BusArrival> getFastestBusArrivals(String routeId, String stopId, List<BusPosition> busPositions) {
        PriorityQueue<BusArrival> busArrivals = new PriorityQueue<>(
            Comparator.comparingDouble(bus -> bus.etaMin)
        );

        for (BusPosition bus : busPositions) {
            Double distance = busRouteRepository.calculateCurvyRoad(
                routeId, stopId, bus.getCurrentLocation().getX(), bus.getCurrentLocation().getY()
            );
            if (distance != null && distance > 0) {
                // Dynamic Speed Logic based on Prasarana Passenger Volume (0=Empty, 1=Seats, 2=Standing, 3=Crushed)
                double speedKmH = 25.0; // Default normal city speed

                if (bus.getPassengerVolume() != null) {
                    if (bus.getPassengerVolume() >= 2) {
                        speedKmH = 12.0; // Heavy load / Slow traffic
                    } else if (bus.getPassengerVolume() == 1) {
                        speedKmH = 18.0; // Medium load
                    }
                }
                
                double speedMetersPerMin = speedKmH * (1000.0 / 60.0);
                double safeSpeed = Math.max(speedMetersPerMin, 10.0);

                double eta = distance / safeSpeed;
                busArrivals.offer(new BusArrival(bus.getBusId(), eta));
            }
        }

        List<BusArrival> topArrivals = new ArrayList<>(); 
        while (!busArrivals.isEmpty()) {
            topArrivals.add(busArrivals.poll());
        }
        return topArrivals;
    }
}