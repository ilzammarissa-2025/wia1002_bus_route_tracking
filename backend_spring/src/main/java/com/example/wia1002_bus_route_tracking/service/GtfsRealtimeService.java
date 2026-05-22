package com.example.wia1002_bus_route_tracking.service;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import com.example.wia1002_bus_route_tracking.model.entity.BusPosition;
import com.example.wia1002_bus_route_tracking.repository.BusPositionRepository;
import com.example.wia1002_bus_route_tracking.repository.BusStopRepository;
import com.example.wia1002_bus_route_tracking.model.entity.BusStop;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate; //use internally to send messages to specific users or bradcast to all subscribers

import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class GtfsRealtimeService {
    private final BusPositionRepository positionRepository;
    private final BusStopRepository stopRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory();
    private final SimpMessagingTemplate messagingTemplate; //websocket bradcaster

    // The data.gov.my endpoint for Prasarana (Rapid KL)
    private final String PRASARANA_API_URL = "https://api.data.gov.my/gtfs-realtime/vehicle-position/prasarana?category=rapid-bus-kl";
//
    public GtfsRealtimeService(BusPositionRepository positionRepository, BusStopRepository stopRepository, SimpMessagingTemplate messagingTemplate) {
        this.positionRepository = positionRepository;
        this.stopRepository = stopRepository;
        this.messagingTemplate = messagingTemplate;
    }

    // This makes the method run automatically every 30 seconds (30000 milliseconds)
    @Scheduled(fixedRate = 30000)
    public void fetchRealTimeBusPositions() {
        try {
            System.out.println("Fetching live bus locations from Prasarana...");
            URL url = URI.create(PRASARANA_API_URL).toURL();
            
            // Magic: The Google library automatically downloads and decodes the Protobuf file into java obj. using parseFrom!
            FeedMessage feed = FeedMessage.parseFrom(url.openStream());

            //create list to save all the new buses 
            List<BusPosition> updatedPositions = new ArrayList<>();

            for (FeedEntity entity : feed.getEntityList()) {
                if (entity.hasVehicle()) {
                    VehiclePosition vehicle = entity.getVehicle();
                    
                    // Extract data
                    String busId = vehicle.getVehicle().getId();
                    double lat = vehicle.getPosition().getLatitude();
                    double lon = vehicle.getPosition().getLongitude();

                    // Convert GPS to PostGIS Point
                    Point location = geometryFactory.createPoint(new Coordinate(lon, lat));
                    location.setSRID(4326);

                    // Update or create the bus in our database
                    BusPosition position = positionRepository.findById(busId).orElse(new BusPosition());
                    position.setBusId(busId);
                    position.setCurrentLocation(location);
                    position.setLastUpdated(LocalDateTime.now());
                    
                    updatedPositions.add(position); // Add to our batch list

                    // Check if this specific bus is physically at a stop right now
                    List<BusStop> nearbyStops = stopRepository.findStopsWithinRadius(lat, lon, 50); // 50m radius
                    
                    if (!nearbyStops.isEmpty()) {
                        BusStop BusesArrivedStop = nearbyStops.get(0); // Assume the first one is the correct stop
                        System.out.println("Arrival: Bus " + busId + " arrived at " + BusesArrivedStop.getStopName());

                        //fire websocket event to remove bus from ETA board and show arrival message  
                        String arrivalMessage = String.format("{\"busId\":\"%s\", \"stopId\":\"%s\"}", busId, BusesArrivedStop.getStopId());
                        messagingTemplate.convertAndSend("/topic/arrivals", arrivalMessage);
                    }
                }
            }
             // Save to PostgreSQL in one massive operation (Much faster)
            positionRepository.saveAll(updatedPositions);
            //push the entire list of new coordinates to the Flutter Map
            messagingTemplate.convertAndSend("/topic/live-buses", updatedPositions);
            System.out.println("Successfully updated bus positions.");

        } catch (Exception e) {
            System.err.println("Error fetching real-time data: " + e.getMessage());
        }
    }
}






