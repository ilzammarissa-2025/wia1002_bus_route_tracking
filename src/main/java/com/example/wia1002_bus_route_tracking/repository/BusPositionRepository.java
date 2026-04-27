package com.example.wia1002_bus_route_tracking.repository;

import com.example.wia1002_bus_route_tracking.model.entity.BusPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusPositionRepository extends JpaRepository<BusPosition, String>{
    //auto provide basic CRUD operations for BusPosition entity, with busId as the primary key (String)
}
