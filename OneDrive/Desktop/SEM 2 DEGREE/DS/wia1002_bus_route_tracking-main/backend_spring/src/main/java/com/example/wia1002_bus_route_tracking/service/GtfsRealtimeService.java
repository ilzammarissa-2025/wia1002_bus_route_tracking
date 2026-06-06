package com.example.wia1002_bus_route_tracking.service;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import com.example.wia1002_bus_route_tracking.model.entity.BusPosition;
import com.example.wia1002_bus_route_tracking.model.entity.BusRoute;
import com.example.wia1002_bus_route_tracking.repository.BusPositionRepository;
import com.example.wia1002_bus_route_tracking.repository.BusStopRepository;
import com.example.wia1002_bus_route_tracking.repository.BusRouteRepository; 
import com.example.wia1002_bus_route_tracking.model.entity.BusStop;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class GtfsRealtimeService {
    private final BusPositionRepository positionRepository;
    private final BusStopRepository stopRepository;
    private final BusRouteRepository busRouteRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory();
    private final SimpMessagingTemplate messagingTemplate;

    private static final String PRASARANA_API_URL = "https://api.data.gov.my/gtfs-realtime/vehicle-position/prasarana?category=rapid-bus-kl";

    public GtfsRealtimeService(BusPositionRepository positionRepository, BusStopRepository stopRepository, BusRouteRepository busRouteRepository, SimpMessagingTemplate messagingTemplate) {
        this.positionRepository = positionRepository;
        this.stopRepository = stopRepository;
        this.busRouteRepository = busRouteRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedRate = 30000)
    public void fetchRealTimeBusPositions() {
        try {
            URL url = URI.create(PRASARANA_API_URL).toURL();
            FeedMessage feed = FeedMessage.parseFrom(url.openStream());
            List<BusPosition> updatedPositions = new ArrayList<>();

            for (FeedEntity entity : feed.getEntityList()) {
                if (entity.hasVehicle()) {
                    VehiclePosition vehicle = entity.getVehicle();
                    
                    String busId = vehicle.getVehicle().getId();
                    double lat = vehicle.getPosition().getLatitude();
                    double lon = vehicle.getPosition().getLongitude();
                    String currentRouteId = vehicle.getTrip().getRouteId();

                    Point location = geometryFactory.createPoint(new Coordinate(lon, lat));
                    location.setSRID(4326);

                    BusPosition position = positionRepository.findById(busId).orElse(new BusPosition());
                    position.setBusId(busId);
                    position.setCurrentLocation(location);
                    position.setLastUpdated(LocalDateTime.now());

                    if (currentRouteId != null && !currentRouteId.isEmpty()) {
                        Optional<BusRoute> routeOpt = busRouteRepository.findById(currentRouteId);
                        
                        // Fallback: match by short name first (if RT has "T789" but DB has "rapid-kl_T789")
                        if (!routeOpt.isPresent()) {
                            routeOpt = busRouteRepository.findByRouteShortName(currentRouteId);
                        }
                        
                        // Secondary fallback: partial match
                        if (!routeOpt.isPresent()) {
                            routeOpt = busRouteRepository.findFirstByRouteIdContaining(currentRouteId);
                        }

                        if (routeOpt.isPresent()) {
                            BusRoute matchedRoute = routeOpt.get();
                            position.setRoute(matchedRoute);

                            try {
                                List<Object[]> snappedCoords = busRouteRepository.snapBusToRoute(matchedRoute.getRouteId(), lon, lat);
                                if (!snappedCoords.isEmpty() && snappedCoords.get(0).length == 2) {
                                    // Override the raw Lon/Lat with the clean snapped values
                                    lon = (Double) snappedCoords.get(0)[0];
                                    lat = (Double) snappedCoords.get(0)[1];
                                    
                                    Point snappedLocation = geometryFactory.createPoint(new Coordinate(lon, lat));
                                    snappedLocation.setSRID(4326);
                                    position.setCurrentLocation(snappedLocation); // Save the perfectly snapped point!
                                }

                                // Must use actual matched DB route ID to successfully calculate distances
                                String nextStop = busRouteRepository.findNextStopName(matchedRoute.getRouteId(), lon, lat);
                                position.setNextStopName(nextStop);

                                //Get Stop ID to calculate ETA
                                String nextStopId = busRouteRepository.findNextStopId(matchedRoute.getRouteId(), lon, lat);
                                if (nextStopId !=null) {
                                    Double distance  = busRouteRepository.calculateCurvyRoad(matchedRoute.getRouteId(), nextStopId, lon, lat);
                                    if (distance != null && distance > 0) {
                                        double speedMetersPerMin = 20.0 * (1000.0 / 60.0); // 20 km/h in m/min
                                        position.setEtaToNextStop(distance / speedMetersPerMin);
                                    }
                                }
                            } catch (Exception e) {
                                position.setNextStopName("End of Route");
                                position.setEtaToNextStop(0.0);
                            }
                        } else {
                            position.setRoute(null);
                        }
                    }
                    
                    updatedPositions.add(position);

                    List<BusStop> nearbyStops = stopRepository.findStopsWithinRadius(lat, lon, 50);
                    if (!nearbyStops.isEmpty()) {
                        BusStop BusesArrivedStop = nearbyStops.get(0);
                        String arrivalMessage = String.format("{\"busId\":\"%s\", \"stopId\":\"%s\"}", busId, BusesArrivedStop.getStopId());
                        messagingTemplate.convertAndSend("/topic/arrivals", arrivalMessage);
                    }
                }
            }

            System.out.println("Fetched " + updatedPositions.size() + " real-time bus positions");
            positionRepository.saveAll(updatedPositions);
            messagingTemplate.convertAndSend("/topic/live-buses", updatedPositions);

            //delete buses that haven't pinged in 15 minutes
            positionRepository.deleteByLastUpdatedBefore(LocalDateTime.now().minusMinutes(15));

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error fetching real-time data: " + e.getMessage());
        }
    }
}