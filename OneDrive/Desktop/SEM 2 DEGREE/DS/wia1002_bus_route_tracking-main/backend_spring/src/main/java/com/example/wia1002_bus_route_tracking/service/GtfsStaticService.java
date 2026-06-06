package com.example.wia1002_bus_route_tracking.service;

import com.example.wia1002_bus_route_tracking.model.entity.BusRoute;
import com.example.wia1002_bus_route_tracking.model.entity.BusStop;
import com.example.wia1002_bus_route_tracking.model.entity.RouteStop;
import com.example.wia1002_bus_route_tracking.model.entity.StopTime;
import com.example.wia1002_bus_route_tracking.repository.BusStopRepository;
import com.example.wia1002_bus_route_tracking.repository.RouteStopRepository;
import com.example.wia1002_bus_route_tracking.repository.BusRouteRepository;
import com.example.wia1002_bus_route_tracking.repository.StopTimeRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalTime;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.*;

@Service
public class GtfsStaticService {
    @Value("${gtfs.file.path}")
    private String gtfsFilePath;
    private final Map<String, String> routeShapeMap = new HashMap<>(); 
    private final BusRouteRepository busRouteRepository;
    private final RouteStopRepository routeStopRepository;
    private final BusStopRepository busStopRepository;
    private final StopTimeRepository stopTimeRepository;
    private final Map<String, String> tripRouteMap = new HashMap<>(); 
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public GtfsStaticService(BusStopRepository busStopRepository, RouteStopRepository routeStopRepository, BusRouteRepository busRouteRepository, StopTimeRepository stopTimeRepository) {
        this.busStopRepository = busStopRepository;
        this.routeStopRepository = routeStopRepository;
        this.busRouteRepository = busRouteRepository;
        this.stopTimeRepository = stopTimeRepository;
    }   

    private static class ShapePoint {
        Coordinate coordinate;
        int sequence;
        ShapePoint(Coordinate c, int s) {
            this.coordinate = c;
            this.sequence = s;
        }
    }

    @PostConstruct 
    public void init() {
        importStaticData();
    }

    public void importStaticData() {
        if (busStopRepository.count() == 0 || busRouteRepository.count() == 0 || routeStopRepository.count() == 0) {
            downloadAndParseZip();
        }
    }

    private void downloadAndParseZip() {
        try {
            InputStream in = getClass().getResourceAsStream(gtfsFilePath);
            
            if (in == null) {
                System.err.println("🚨 CRITICAL ERROR: Could not find the ZIP file at " + gtfsFilePath);
                System.err.println("🚨 Please check your src/main/resources/gtfs-data folder!");
                return;
            }

            System.out.println("✅ ZIP file found! Starting to parse... (This might take a minute)");

            byte[] zipData = in.readAllBytes(); 
            in.close();

            processZipPass(zipData, "trips.txt"); 
            processZipPass(zipData, "routes.txt");
            processZipPass(zipData, "stops.txt");
            processZipPass(zipData, "stop_times.txt");
            processZipPass(zipData, "shapes.txt");
        } catch (Exception e) {
            System.err.println("Failed to download or parse ZIP: " + e.getMessage());
        }
    }

    private void processZipPass(byte[] zipData, String targetFile) throws Exception {
        ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipData));
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            if (entry.getName().equals(targetFile)) {
                if (targetFile.equals("trips.txt")) parseTrips(zis);
                else if (targetFile.equals("routes.txt")) parseRoutes(zis);
                else if (targetFile.equals("stops.txt")) parseStops(zis);
                else if (targetFile.equals("stop_times.txt")) parseStopTimes(zis);
                else if (targetFile.equals("shapes.txt")) parseShapes(zis);
            }
            zis.closeEntry();
        }
        zis.close();
    }

    private void parseTrips(ZipInputStream zis) throws Exception {
        Set<String> trips = new HashSet<>();
        Reader reader = new InputStreamReader(zis);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader);

        for (CSVRecord record : records) {
            String tripId = record.get("trip_id");
            String routeId = record.get("route_id");
            String shapeId = record.get("shape_id");

            tripRouteMap.put(tripId, routeId);

            if (!trips.contains(routeId)) {
                routeShapeMap.put(routeId, shapeId);
                trips.add(routeId);
            }
        }
    }

    private void parseStopTimes(ZipInputStream zis) throws Exception {
        List<RouteStop> bridgeData = new ArrayList<>();
        List<StopTime> scheduleData = new ArrayList<>();
        Set<String> uniqueRouteStops = new HashSet<>();
        Reader reader = new InputStreamReader(zis);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader);

        System.out.println("⏳ Parsing stop_times.txt (This is a huge file, please wait...)");
        int count = 0;

        for (CSVRecord record : records) {
            String tripId = record.get("trip_id");
            if (tripRouteMap.containsKey(tripId)) {
                String routeId = tripRouteMap.get(tripId);
                String stopId = record.get("stop_id");
                int sequence = Integer.parseInt(record.get("stop_sequence"));
                String uniqueKey = routeId + "-" + stopId;

                // 1. Keep map data
                if (!uniqueRouteStops.contains(uniqueKey)) {
                    uniqueRouteStops.add(uniqueKey);
                    RouteStop routeStop = new RouteStop();
                    routeStop.setRouteId(routeId);
                    routeStop.setStopId(stopId);
                    routeStop.setStopSequence(sequence);
                    bridgeData.add(routeStop);
                }

                // 2. Add Schedule Time Data
                try {
                    String arrivalStr = record.get("arrival_time").trim();
                    String departureStr = record.get("departure_time").trim();
                    LocalTime arrivalTime = parseGtfsTime(arrivalStr);
                    LocalTime departureTime = parseGtfsTime(departureStr);

                    StopTime stopTime = new StopTime(tripId, stopId, routeId, arrivalTime, departureTime, sequence);
                    scheduleData.add(stopTime);
                } catch (Exception e) {
                    // Skip bad rows
                }

                count++;

                //batch save to avoid OOM for large files
                if (count % 5000 == 0) {
                    routeStopRepository.saveAll(bridgeData);
                    stopTimeRepository.saveAll(scheduleData);
                    bridgeData.clear();
                    scheduleData.clear();
                    System.out.println("Saved " + count + " schedule rows...");
                } 
            }
        }
        if (!bridgeData.isEmpty()) routeStopRepository.saveAll(bridgeData);
        if (!scheduleData.isEmpty()) stopTimeRepository.saveAll(scheduleData);
    }

    private LocalTime parseGtfsTime(String timeStr) {
        String[] parts = timeStr.split(":");
        int hours = Integer.parseInt(parts[0]);
        if (hours >= 24) hours = hours % 24; 
        return LocalTime.of(hours, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    private void parseShapes(ZipInputStream zis) throws Exception {
        Map<String, List<ShapePoint>> shapeCoordinates = new HashMap<>();
        Reader reader = new InputStreamReader(zis);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader);

        for (CSVRecord record : records) {
            String shapeId = record.get("shape_id");
            double lat = Double.parseDouble(record.get("shape_pt_lat"));
            double lon = Double.parseDouble(record.get("shape_pt_lon"));
            int sequence = Integer.parseInt(record.get("shape_pt_sequence"));
            shapeCoordinates.putIfAbsent(shapeId, new ArrayList<>());
            shapeCoordinates.get(shapeId).add(new ShapePoint(new Coordinate(lon, lat), sequence));
        }

        List<BusRoute> allRoutes = busRouteRepository.findAll();
        for (BusRoute busRoute : allRoutes) {
            String shapeId = routeShapeMap.get(busRoute.getRouteId());
            if (shapeId != null && shapeCoordinates.containsKey(shapeId)) {
                List<ShapePoint> points = shapeCoordinates.get(shapeId);
                points.sort(Comparator.comparingInt(p -> p.sequence));
                Coordinate[] coordArray = points.stream().map(p -> p.coordinate).toArray(Coordinate[]::new);
                if (coordArray.length > 1) { 
                    busRoute.setRoutePath(geometryFactory.createLineString(coordArray));
                }   
            }
        }
        busRouteRepository.saveAll(allRoutes); 
    }

    private void parseRoutes(ZipInputStream zis) throws Exception {
        List<BusRoute> routesToSave = new ArrayList<>();
        Reader reader = new InputStreamReader(zis);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader);

        for (CSVRecord record : records) {
            BusRoute route = new BusRoute();
            route.setRouteId(record.get("route_id"));
            route.setRouteShortName(record.get("route_short_name"));
            route.setRouteLongName(record.get("route_long_name"));
            routesToSave.add(route);
        }
        busRouteRepository.saveAll(routesToSave);
    }
    
    private void parseStops(ZipInputStream zis) throws Exception {
        List<BusStop> stopsToSave = new ArrayList<>();
        Reader reader = new InputStreamReader(zis);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader);
                
        for (CSVRecord record : records) {
            String stopId = record.get("stop_id");
            String stopName = record.get("stop_name");
            double lat = Double.parseDouble(record.get("stop_lat"));
            double lon = Double.parseDouble(record.get("stop_lon"));

            Coordinate coord = new Coordinate(lon, lat);
            Point location = geometryFactory.createPoint(coord);

            BusStop stop = new BusStop(stopId, stopName, location);
            stopsToSave.add(stop);
        }
        busStopRepository.saveAll(stopsToSave);
    }
}