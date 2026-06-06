package com.example.wia1002_bus_route_tracking.repository;

import com.example.wia1002_bus_route_tracking.model.entity.BusRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface BusRouteRepository extends JpaRepository<BusRoute, String> {

    Optional<BusRoute> findByRouteShortName(String routeShortName);
    Optional<BusRoute> findFirstByRouteIdContaining(String routeId);
    
    //map matching to extract the exact snapped longitude and latitude 
    @Query(value = """
            SELECT
                ST_X(ST_ClosestPoint(route_path::geometry, ST_SetSRID(ST_MakePoint(:busLon, :busLat), 4326))) AS lon,
                ST_Y(ST_ClosestPoint(route_path::geometry, ST_SetSRID(ST_MakePoint(:busLon, :busLat), 4326))) AS lat
            FROM bus_routes
            WHERE route_id = :routeId
            """, nativeQuery = true)
    List<Object[]> snapBusToRoute(
        @Param("routeId") String routeId,
        @Param("busLon") double busLon,
        @Param("busLat") double busLat
    );
            
    @Query(value = """
        SELECT bs.stop_name
        FROM bus_routes br
        JOIN route_stops rs ON br.route_id = rs.route_id
        JOIN bus_stops bs ON rs.stop_id = bs.stop_id
        WHERE br.route_id = :routeId
        ORDER BY(
            CAST(ST_LineLocatePoint(br.route_path::geometry, bs.location::geometry) AS numeric) - 
            CAST(ST_LineLocatePoint(br.route_path::geometry, ST_SetSRID(ST_MakePoint(:busLon, :busLat), 4326)) AS numeric)
            + 1.0
        ) % 1.0 ASC
        LIMIT 1
        """, nativeQuery = true)

    String findNextStopName(
        @Param("routeId") String routeId,
        @Param("busLon") double busLon,
        @Param("busLat") double busLat
    );

    @Query(value = """
        SELECT bs.stop_id
        FROM bus_routes br
        JOIN route_stops rs ON br.route_id = rs.route_id
        JOIN bus_stops bs ON rs.stop_id = bs.stop_id
        WHERE br.route_id = :routeId
        ORDER BY (
            CAST(ST_LineLocatePoint(br.route_path::geometry, bs.location::geometry) AS numeric) - 
            CAST(ST_LineLocatePoint(br.route_path::geometry, ST_SetSRID(ST_MakePoint(:busLon, :busLat), 4326)) AS numeric) 
            + 1.0
        ) % 1.0 ASC
        LIMIT 1
        """, nativeQuery = true)
    String findNextStopId(
        @Param("routeId") String routeId,
        @Param("busLon") double busLon,
        @Param("busLat") double busLat
    );

    @Query(value = """
        SELECT CASE
            -- Bus is BEHIND the stop (Normal approach)
            WHEN ST_LineLocatePoint(br.route_path::geometry, ST_SetSRID(ST_MakePoint(:busLon, :busLat), 4326)) <=
                 ST_LineLocatePoint(br.route_path::geometry, bs.location::geometry)
            THEN ST_Length(
                ST_LineSubstring(
                    br.route_path::geometry,
                    ST_LineLocatePoint(br.route_path::geometry, ST_SetSRID(ST_MakePoint(:busLon, :busLat), 4326)),
                    ST_LineLocatePoint(br.route_path::geometry, bs.location::geometry)
                )::geography
            )
            -- Bus is AHEAD of the stop (Loop wrap-around)
            ELSE 
                -- Distance from Bus to the END of the route
                ST_Length(
                    ST_LineSubstring(
                        br.route_path::geometry,
                        ST_LineLocatePoint(br.route_path::geometry, ST_SetSRID(ST_MakePoint(:busLon, :busLat), 4326)),
                        1.0
                    )::geography
                )
                +
                -- PLUS Distance from START of the route to the Stop
                ST_Length(
                    ST_LineSubstring(
                        br.route_path::geometry,
                        0.0,
                        ST_LineLocatePoint(br.route_path::geometry, bs.location::geometry)
                    )::geography
                )
        END AS distance_meters
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