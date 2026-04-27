package com.example.wia1002_bus_route_tracking.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "bus_stops")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusStop {

    @Id
    @Column(name = "stop_id", length = 50)
    private String stopId;

    @Column(name = "stop_name", nullable = false)
    private String stopName;

    // check the columnDefinition matches your exact SQL script
    @Column(name = "location", columnDefinition = "geography(Point, 4326)")
    private Point location; 
}
