package com.example.wia1002_bus_route_tracking.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.locationtech.jts.geom.Point;
import com.fasterxml.jackson.annotation.JsonIgnore;

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

    @JsonIgnore
    // check the columnDefinition matches your exact SQL script
    @Column(name = "location", columnDefinition = "geography(Point, 4326)")
    private Point location; 

    public double getLatitude() {
        return this.location != null ? this.location.getY() : 0.0;
    }

    public double getLongitude() {
        return this.location != null ? this.location.getX() : 0.0;
    }
}
