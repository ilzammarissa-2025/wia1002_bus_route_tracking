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
    /* 
    purpose : find stops within a certain radius (in meters) ~ find bus near me
    nativeQuery = true : indicates that the query is a native SQL query, not JPQL
    CAST(...AS geography): tells the database to calculate distance in meters on globe
    concept: create a circle near user and return points in the bus_stops table fall within
    */ 

    @Query(value = "SELECT * FROM bus_stops b " +
                   "WHERE ST_DWithin(b.location, CAST(ST_SetSRID(ST_MakePoint(:lon, :lat), 4326) AS geography), :radiusMeters)", 
           nativeQuery = true)
    List<BusStop> findStopsWithinRadius(@Param("lat") double lat, 
                                        @Param("lon") double lon, 
                                        @Param("radiusMeters") double radiusMeters);

    
    //use JPQL for BusStop and RouteStop, loop up mapping table to find all IDs associated with a route, then fetches the full BusStop obects for those IDs
    //use when student clicks specific bus route and see every stop
    //Using a JOIN guarantees the outer BusStop list is sorted by the RouteStop sequence.
    @Query("SELECT s FROM BusStop s " +
           "JOIN RouteStop rs ON s.stopId = rs.stopId " +
           "JOIN BusRoute br ON rs.routeId = br.routeId " +
           "WHERE br.routeShortName = :routeShortName " +
           "ORDER BY rs.stopSequence ASC")
    List<BusStop> findStopsByRouteShortName(@Param("routeShortName") String routeShortName);                                  
    
}
