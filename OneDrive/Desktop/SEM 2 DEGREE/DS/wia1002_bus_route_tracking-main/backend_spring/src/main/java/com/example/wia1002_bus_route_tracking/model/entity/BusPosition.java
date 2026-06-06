package com.example.wia1002_bus_route_tracking.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.locationtech.jts.geom.Point;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "bus_positions")
@Data
public class BusPosition {

    @Id
    @Column(name = "bus_id", length = 50)
    private String busId;

    @JsonProperty("id") 
    public String getId() {
        return this.busId;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    @JsonIgnore 
    private BusRoute route;

    //allow flutter to track the bus route
    @JsonProperty("routeId")
    public String getRouteId() {
        return route != null ? route.getRouteId() : null;
    }

    @JsonIgnore
    @Column(name = "current_location", columnDefinition = "geography(Point, 4326)")
    private Point currentLocation;

    @JsonProperty("latitude")
    public Double getLatitude() {
        if (currentLocation != null) return currentLocation.getY();
        return null;
    }

    @JsonProperty("longitude")
    public Double getLongitude() {
        if (currentLocation != null) return currentLocation.getX();
        return null;
    }

    @Column(name = "passenger_volume")
    private Integer passengerVolume;

    @Column(name = "next_stop_name")
    @JsonProperty("nextStop")
    private String nextStopName;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "eta_to_next_stop")
    @JsonProperty("etaToNextStop")
    private Double etaToNextStop; 
}