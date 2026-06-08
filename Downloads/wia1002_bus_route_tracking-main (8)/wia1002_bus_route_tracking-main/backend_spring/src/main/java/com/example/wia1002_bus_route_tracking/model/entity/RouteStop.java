package com.example.wia1002_bus_route_tracking.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "route_stops") //links route to stop
@Data

public class RouteStop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)    
    private Long id;

    @Column(name = "route_id")
    private String routeId;

    @Column(name = "stop_id")
    private String stopId;

    @Column(name = "stop_sequence")
    private Integer stopSequence;
}
