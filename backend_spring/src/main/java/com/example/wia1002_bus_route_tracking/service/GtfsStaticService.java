package com.example.wia1002_bus_route_tracking.service;

import com.example.wia1002_bus_route_tracking.model.entity.BusRoute;
import com.example.wia1002_bus_route_tracking.model.entity.BusStop;
import com.example.wia1002_bus_route_tracking.model.entity.RouteStop;
// import com.youruniversity.bustracker.model.entity.BusRoute; 
import com.example.wia1002_bus_route_tracking.repository.BusStopRepository;

import com.example.wia1002_bus_route_tracking.repository.RouteStopRepository;
import com.example.wia1002_bus_route_tracking.repository.BusRouteRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.*;;

@Service
public class GtfsStaticService {
    private final Map<String, String> routeShapeMap = new HashMap<>(); //route.id -> shape.id
    
    private final BusRouteRepository busRouteRepository;

    private final RouteStopRepository routeStopRepository;

    private final BusStopRepository busStopRepository;
    
    private final Map<String, String> tripRouteMap = new HashMap<>(); // trip_id -> route_id mapping 

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public GtfsStaticService(BusStopRepository busStopRepository,  RouteStopRepository routeStopRepository, BusRouteRepository busRouteRepository) {
        this.busStopRepository = busStopRepository;
        this.routeStopRepository = routeStopRepository;
        this.busRouteRepository = busRouteRepository;
    }   

    @PostConstruct 
    
    public void init() {
        System.out.println("🚀 GTFS Service Initialized!");
        importStaticData(); // Call your download/parse method here
    }

    public void importStaticData() {
        if (busStopRepository.count() == 0 || busRouteRepository.count() == 0 || routeStopRepository.count() == 0) {
            System.out.println("Database is empty. Downloading and extracting GTFS ZIP from API...");
            downloadAndParseZip();
        } else {
            System.out.println("Static GTFS Data already exists. Skipping download.");
        }
    }

    private void downloadAndParseZip() {
        try {

            InputStream in = getClass().getResourceAsStream("/gtfs-data/gtfs_rapid_bus_kl.zip");

            if (in==null)  {
                System.err.println("Failed to load ZIP file from resources! Check the path and filename.");
                return;
            }

            byte[] zipData = in.readAllBytes(); 
            in.close();

            System.out.println("--- PASS 1: Building Trip Dictionary ---");
            processZipPass(zipData, "trips.txt"); // First pass to build tripRouteMap

            System.out.println("--- PASS 2: Extracting & Saving Data ---");
            processZipPass(zipData, "routes.txt");
            processZipPass(zipData, "stops.txt");
            processZipPass(zipData, "stop_times.txt");
            processZipPass(zipData, "shapes.txt");

            System.out.println("Finished processing the ZIP file!");
        } catch (Exception e) {
            System.err.println("Failed to download or parse ZIP: " + e.getMessage());
        }
    }

    private void parseShapes(ZipInputStream zis) throws Exception {
        Map<String, List<Coordinate>> shapeCoordinates = new HashMap<>();

        Reader reader = new InputStreamReader(zis);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .build()
            .parse(reader);

        for (CSVRecord record : records) {
            String shapeId = record.get("shape_id");
            double lat = Double.parseDouble(record.get("shape_pt_lat"));
            double lon = Double.parseDouble(record.get("shape_pt_lon"));

            shapeCoordinates.putIfAbsent(shapeId, new ArrayList<>());
            shapeCoordinates.get(shapeId).add(new Coordinate(lon, lat));
        }

        System.out.println("Parsed " + shapeCoordinates.size() + " unique shapes from the GTFS data.");
    
    //fetch all saved routes to update them
    List<BusRoute> allRoutes = busRouteRepository.findAll();
    for (BusRoute busRoute : allRoutes) {
        String shapeId = routeShapeMap.get(busRoute.getRouteId());
        if (shapeId != null && shapeCoordinates.containsKey(shapeId)) {
            List<Coordinate> coords = shapeCoordinates.get(shapeId);
            //// Convert list to array for JTS GeometryFactory
            Coordinate[] coordArray = coords.toArray(new Coordinate[0]);
            //create curvy road
            busRoute.setRoutePath(geometryFactory.createLineString(coordArray));
            }
        }

        busRouteRepository.saveAll(allRoutes); //update the DB with curby lines
        System.out.println("Updated " + allRoutes.size() + " bus routes with their corresponding shapes!");
    }

    private void processZipPass(byte[] zipData, String targetFile) throws Exception {
        ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipData));
        ZipEntry entry;
        
        while ((entry = zis.getNextEntry()) != null) {
            if (entry.getName().equals(targetFile)) {
                System.out.println("Found " + targetFile + "! Parsing now...");
                
                if (targetFile.equals("trips.txt")) parseTrips(zis);
                else if (targetFile.equals("routes.txt")) parseRoutes(zis);
                else if (targetFile.equals("stops.txt")) parseStops(zis);
                else if (targetFile.equals("stop_times.txt")) parseStopTimes(zis);
                else if (targetFile.equals("shapes.txt")) parseShapes(zis);
                else System.out.println("No parser defined for " + targetFile);
            }
            zis.closeEntry();
        }
        zis.close();
    }
    

    //pass the ZipInputStream directly into the CSV parser, no need to read the whole file into memory first
    private void parseStops(ZipInputStream zis) throws Exception {
        List<BusStop> stopsToSave = new ArrayList<>();
        Reader reader = new InputStreamReader(zis);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build()
                .parse(reader);
                
        for (CSVRecord record : records) {
            String stopId = record.get("stop_id");
            String stopName = record.get("stop_name");
            double lat = Double.parseDouble(record.get("stop_lat"));
            double lon = Double.parseDouble(record.get("stop_lon"));

            // Create a Point geometry for the bus stop location
            Coordinate coord = new Coordinate(lon, lat); // Note: longitude comes first in Coordinate
            Point location = geometryFactory.createPoint(coord);

            BusStop stop = new BusStop(stopId, stopName, location);
            stopsToSave.add(stop);
        }

        busStopRepository.saveAll(stopsToSave);
        System.out.println("Saved " + stopsToSave.size() + " bus stops to the database.");
    }

    private void parseTrips(ZipInputStream zis) throws Exception {
        Set<String> trips = new HashSet<>();

        Reader reader = new InputStreamReader(zis);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build()
                .parse(reader);

        for (CSVRecord record : records) {
            String tripId = record.get("trip_id");
            String routeId = record.get("route_id");
            String shapeId = record.get("shape_id");

            if (!trips.contains(routeId)) {
                tripRouteMap.put(tripId, routeId);
                routeShapeMap.put(routeId, shapeId);// Map the Route to its Shape
                trips.add(routeId);
            }
            
        }
        System.out.println("Mapped " + trips.size() + " unique routes to trips.");
    }

    private void parseStopTimes(ZipInputStream zis) throws Exception {
        List<RouteStop> bridgeData = new ArrayList<>();

        Reader reader = new InputStreamReader(zis);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build()
                .parse(reader);

        for (CSVRecord record : records) {
            String tripId = record.get("trip_id");

            if (tripRouteMap.containsKey(tripId)) {
                String routeId = tripRouteMap.get(tripId);
                RouteStop routeStop = new RouteStop();
                routeStop.setRouteId(routeId);
                routeStop.setStopId(record.get("stop_id"));
                routeStop.setStopSequence(Integer.parseInt(record.get("stop_sequence")));
                bridgeData.add(routeStop);
            }
        }
        routeStopRepository.saveAll(bridgeData);
        System.out.println("Saved " + bridgeData.size() + " route sequence links to the database!"); 
    }

    private void parseRoutes(ZipInputStream zis) throws Exception {
        List<BusRoute> routesToSave = new ArrayList<>();
        Reader reader = new InputStreamReader(zis);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build()
                .parse(reader);

        for (CSVRecord record : records) {
            BusRoute route = new BusRoute();
            route.setRouteId(record.get("route_id"));
            route.setRouteShortName(record.get("route_short_name"));
            route.setRouteLongName(record.get("route_long_name"));

            routesToSave.add(route);
        }

        busRouteRepository.saveAll(routesToSave);
        System.out.println("Saved " + routesToSave.size() + " bus routes to the database.");

    }

}