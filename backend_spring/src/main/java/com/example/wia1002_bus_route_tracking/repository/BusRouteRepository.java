package com.example.wia1002_bus_route_tracking.repository;

import com.example.wia1002_bus_route_tracking.model.entity.BusRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BusRouteRepository extends JpaRepository<BusRoute, String> {
    @Query(value = """
        SELECT ST_Length(
            ST_LineSubstring(
                br.route_path::geometry,
                --Fraction 1 : location of the bus on the route
                LEAST(
                        ST_LineLocatePoint(br.route_path::geometry, ST_SetSRID(ST_makePoint(:busLon, :busLat), 4326)),
                        ST_LineLocatePoint(br.route_path::geometry, bs.location::geometry)
                    ),
                -- Fraction 2 : the location of the stop on the route 
                GREATEST(
                    ST_LineLocatePoint(br.route_path::geometry, ST_SetSRID(ST_makePoint(:busLon, :busLat), 4326)),
                    ST_LineLocatePoint(br.route_path::geometry, bs.location::geometry)
                    )
                )::geography
        ) AS distance_meters
        FROM bus_routes br
        JOIN route_stops rs ON br.route_id = rs.route_id
        JOIN bus_stops bs ON rs.stop_id = bs.stop_id
        WHERE br.route_id = :routeId AND bs.stop_id = :stopId
        """, nativeQuery = true)
    Double calculateCurvyRoad(
        @Param("routeId") String routeId,
        @Param("stopId") String stopId,
        @Param("busLon") double busLon,
        @Param("busLat") double busLat
    );
        
}