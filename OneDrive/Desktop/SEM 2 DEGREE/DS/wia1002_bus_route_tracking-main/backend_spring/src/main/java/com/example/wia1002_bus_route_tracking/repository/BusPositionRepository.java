package com.example.wia1002_bus_route_tracking.repository;

import com.example.wia1002_bus_route_tracking.model.entity.BusPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BusPositionRepository extends JpaRepository<BusPosition, String>{
    /*
    auto provide basic CRUD operations for BusPosition entity, with busId as the primary key (String)
    Spring Data JPA Derived Query Methods auto generate SQL query based on the method name using Java Reflectoin and Criteria API
    */

    List<BusPosition> findByRoute_RouteId(String routeId);

    @Transactional 
    //auto. delete buses older than the provded timestamp
    void deleteByLastUpdatedBefore(LocalDateTime timeLimit);
}

