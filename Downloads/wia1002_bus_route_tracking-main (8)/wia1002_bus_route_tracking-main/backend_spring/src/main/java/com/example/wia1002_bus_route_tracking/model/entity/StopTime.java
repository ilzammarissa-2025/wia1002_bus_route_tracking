package com.example.wia1002_bus_route_tracking.model.entity;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity 
@Table(name = "stop_times", indexes = {
    @Index(name = "idx_stop_id", columnList = "stop_id"),
    @Index(name = "idx_route_id", columnList = "routeId")
})

public class StopTime {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)  
    private Long id;
    private String stopId;
    private String tripId;
    private String routeId;
    private LocalTime arrivalTime;
    private LocalTime departureTime;  
    private Integer stopSequence;

    public StopTime() {}

    public StopTime(String stopId, String tripId, String routeId, LocalTime arrivalTime, LocalTime departureTime, Integer stopSequence) {
        this.stopId = stopId;
        this.tripId = tripId;
        this.routeId = routeId;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.stopSequence = stopSequence;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getTripId() { return tripId; }
    public String getStopId() { return stopId; }
    public String getRouteId() { return routeId; }
    public LocalTime getArrivalTime() { return arrivalTime; }
    public LocalTime getDepartureTime() { return departureTime; }
    public Integer getStopSequence() { return stopSequence; }
}
    

