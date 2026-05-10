package com.example.wia1002_bus_route_tracking.repository;

import com.example.wia1002_bus_route_tracking.model.entity.BusRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface busRouteRepository extends JpaRepository<BusRoute, String> {
}