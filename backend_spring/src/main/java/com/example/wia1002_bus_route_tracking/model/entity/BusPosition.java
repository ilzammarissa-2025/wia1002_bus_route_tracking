package com.example.wia1002_bus_route_tracking.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.locationtech.jts.geom.Point;
import java.time.LocalDateTime;

@Entity
@Table(name = "bus_positions")
@Data
public class BusPosition {

    @Id
    @Column(name = "bus_id", length = 50)
    private String busId;

    // Links this position to a specific route
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private BusRoute route;

    @Column(name = "current_location", columnDefinition = "geography(Point, 4326)")
    private Point currentLocation;

    @Column(name = "passenger_volume")
    private Integer passengerVolume;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}
