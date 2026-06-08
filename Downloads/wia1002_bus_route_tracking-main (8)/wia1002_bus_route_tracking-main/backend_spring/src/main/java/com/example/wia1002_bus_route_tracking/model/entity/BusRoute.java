package com.example.wia1002_bus_route_tracking.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.locationtech.jts.geom.LineString;

@Entity
@Table(name = "bus_routes")
@Data
public class BusRoute {

    @Id
    @Column(name = "route_id", length = 50)
    private String routeId;

    @Column(name = "route_short_name", length = 50)
    private String routeShortName;

    @Column(name = "route_long_name")
    private String routeLongName;

    @Column(name = "route_path", columnDefinition = "geography(LineString, 4326)")
    private LineString routePath;
}