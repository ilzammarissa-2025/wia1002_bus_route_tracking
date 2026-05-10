package com.example.wia1002_bus_route_tracking.repository;

import com.example.wia1002_bus_route_tracking.model.entity.RouteStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {

}
