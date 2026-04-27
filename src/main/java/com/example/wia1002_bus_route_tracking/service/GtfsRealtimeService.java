package com.example.wia1002_bus_route_tracking.service;

import com.google.transit.realtime.GtfsRealtime.FeedEntity;
import com.google.transit.realtime.GtfsRealtime.FeedMessage;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import com.example.wia1002_bus_route_tracking.model.entity.BusPosition;
import com.example.wia1002_bus_route_tracking.repository.BusPositionRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;

@Service
public class GtfsRealtimeService {
    private final BusPositionRepository positionRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    // The data.gov.my endpoint for Prasarana (Rapid KL)
    private final String PRASARANA_API_URL = "https://api.data.gov.my/gtfs-realtime/vehicle-position/prasarana?category=rapid-bus-kl";

    public GtfsRealtimeService(BusPositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    // This makes the method run automatically every 30 seconds (30000 milliseconds)
    @Scheduled(fixedRate = 30000)
    public void fetchRealTimeBusPositions() {
        try {
            System.out.println("Fetching live bus locations from Prasarana...");
            URL url = URI.create(PRASARANA_API_URL).toURL();
            
            // Magic: The Google library automatically downloads and decodes the Protobuf file!
            FeedMessage feed = FeedMessage.parseFrom(url.openStream());

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
                    
                    // Save to PostgreSQL
                    positionRepository.save(position);
                }
            }
            System.out.println("Successfully updated bus positions.");

        } catch (Exception e) {
            System.err.println("Error fetching real-time data: " + e.getMessage());
        }
    }
}






