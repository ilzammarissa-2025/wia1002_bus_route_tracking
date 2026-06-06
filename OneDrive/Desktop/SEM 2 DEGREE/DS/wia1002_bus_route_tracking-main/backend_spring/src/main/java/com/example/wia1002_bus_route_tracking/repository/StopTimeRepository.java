package com.example.wia1002_bus_route_tracking.repository;

import com.example.wia1002_bus_route_tracking.model.entity.StopTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface StopTimeRepository extends JpaRepository<StopTime, Long> {
    @Query(
        "SELECT st FROM StopTime st WHERE st.stopId = :stopId AND st.routeId = :routeId AND st.arrivalTime >= :currentTime ORDER BY st.arrivalTime ASC"
    )
    List<StopTime> findUpcomingSchedules(
        @Param("stopId") String stopId,
        @Param("routeId") String routeId,
        @Param("currentTime") LocalTime currentTime
    );
}
