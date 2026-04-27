package com.example.wia1002_bus_route_tracking.repository;

import com.example.wia1002_bus_route_tracking.model.entity.BusStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BusStopRepository extends JpaRepository<BusStop, String> {

    //REPOSITORY LAYER : interface for CRUD operations and custom PostGIS queries (similar to SQL)
    // purpose : find stops within a certain radius (in meters)
    @Query(value = "SELECT * FROM bus_stops b " +
                   "WHERE ST_DWithin(b.location, CAST(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326) AS geography), :radiusMeters)", 
           nativeQuery = true)
    List<BusStop> findStopsWithinRadius(@Param("lat") double lat, 
                                        @Param("lon") double lon, 
                                        @Param("radiusMeters") double radiusMeters);
}
