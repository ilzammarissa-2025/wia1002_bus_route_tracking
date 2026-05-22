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

    //show fastest bus arrival time for each stop
    public static class BusArrival {
        String busId;
        double etaMin;

        //constructor
        public BusArrival(String busId, double etaMin) {
            this.busId = busId;
            this.etaMin = etaMin;
        }
    }

    public List<NearestBusResponse> getNearestStops(double userLat, double userLon, double radiusMeters) {
        // 1. Let the database do the heavy lifting instead of loop
        List<BusStop> nearestStops = busStopRepository.findStopsWithinRadius(userLat, userLon, radiusMeters);
        List<NearestBusResponse> responses = new ArrayList<>();
        //map database entities to DTOs
        for (BusStop stop : nearestStops) {
            double distance = calculateHaversineDistance(userLat, userLon, stop.getLatitude(), stop.getLongitude());
            NearestBusResponse dto = new NearestBusResponse(
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
        final int EARTH_RADIUS = 6371000; // in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    public double calculateVincentyDistance(double lat1, double lon1, double lat2, double lon2) {
        double SEMI_MAJOR_AXIS_MT = 6378137;
        double SEMI_MINOR_AXIS_MT = 6356752.314245;
        double FLATTENING = 1 / 298.257223563;
        double ERROR_TOLERANCE = 1e-12; 

        double U1 = Math.atan((1 - FLATTENING) * Math.tan(Math.toRadians(lat1)));
        double U2 = Math.atan((1 - FLATTENING) * Math.tan(Math.toRadians(lat2)));

        double longitudeDif = Math.toRadians(lon2 - lon1);

        double sinU1 = Math.sin(U1);
        double cosU1 = Math.cos(U1);
        double sinU2 = Math.sin(U2);
        double cosU2 = Math.cos(U2);

        double sinSigma, cosSigma, sigma, sinAlpha, cosSqAlpha, cos2SigmaM, previouslongitudeDif;

        do {
        sinSigma = Math.sqrt(Math.pow(cosU2 * Math.sin(longitudeDif), 2) +
            Math.pow(cosU1 * sinU2 - sinU1 * cosU2 * Math.cos(longitudeDif), 2));

        cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * Math.cos(longitudeDif);

        sigma = Math.atan2(sinSigma, cosSigma);

        sinAlpha = cosU1 * cosU2 * Math.sin(longitudeDif) / sinSigma;

        cosSqAlpha = 1 - Math.pow(sinAlpha, 2);

        cos2SigmaM = cosSigma - 2 * sinU1 * sinU2 / cosSqAlpha;

        if (Double.isNaN(cos2SigmaM)) {
            cos2SigmaM = 0;
        }

        previouslongitudeDif = longitudeDif;

        double C = FLATTENING / 16 * cosSqAlpha * (4 + FLATTENING * (4 - 3 * cosSqAlpha));

        longitudeDif = Math.toRadians(lon2 - lon1) + (1 - C) * FLATTENING * sinAlpha *
            (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1 + 2 * Math.pow(cos2SigmaM, 2))));

        } while (Math.abs(longitudeDif - previouslongitudeDif) > ERROR_TOLERANCE);

        double uSq = cosSqAlpha * (Math.pow(SEMI_MAJOR_AXIS_MT, 2) - Math.pow(SEMI_MINOR_AXIS_MT, 2)) / Math.pow(SEMI_MINOR_AXIS_MT, 2);

        double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));

        double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));

        double deltaSigma = B * sinSigma * (cos2SigmaM + B / 4 * (cosSigma * (-1 + 2 * Math.pow(cos2SigmaM, 2))
            - B / 6 * cos2SigmaM * (-3 + 4 * Math.pow(sinSigma, 2)) * (-3 + 4 * Math.pow(cos2SigmaM, 2))));

        double distanceMt = SEMI_MINOR_AXIS_MT * A * (sigma - deltaSigma);
        
        return distanceMt / 1000;
    }

    public List<BusArrival> getFastestBusArrivals(String routeId, String stopId, List<BusPosition> busPositions) {
        //Instantiate the Priority Queue (Min-Heap)
        PriorityQueue<BusArrival> busArrivals = new PriorityQueue<>(
            Comparator.comparingDouble(bus -> bus.etaMin)
        );//sort the heap by etaMin

        //calculate ETA for each bus and enqueue
        for (BusPosition bus : busPositions) {
            Double distance = busRouteRepository.calculateCurvyRoad(
                routeId, stopId, bus.getCurrentLocation().getX(), bus.getCurrentLocation().getY()
            );
            if (distance != null && distance > 0) {
                // 2. Use live speed (fallback to 15km/h if bus is stopped at a light)
                double speedKmH  = (bus.getPassengerVolume() != null) ? 15.0 : 15.0; //later replace with bus.getSpeed()
                double speedMetersPerMin = speedKmH * (1000.0 / 60.0);
                double safeSpeed = Math.max(speedMetersPerMin, 10.0);

                double eta = distance / safeSpeed;
                busArrivals.offer(new BusArrival(bus.getBusId(), eta));
            }
        }

        List<BusArrival> topArrivals = new ArrayList<>(); //dequeue the top results for the user
        while (!busArrivals.isEmpty()) {
            topArrivals.add(busArrivals.poll());//heap instantly restructures itself sp the fastest is at the top
        }
        return topArrivals;
    }
}
