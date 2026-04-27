package com.example.wia1002_bus_route_tracking.service;

import com.example.wia1002_bus_route_tracking.model.entity.BusStop;
// import com.youruniversity.bustracker.model.entity.BusRoute; 
import com.example.wia1002_bus_route_tracking.repository.BusStopRepository;

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
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class GtfsStaticService {
    
    private final BusStopRepository busStopRepository;

    // (later uncomment) private final BusRouteRepository busRouteRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    //API endpoints from data.gov.my GFTS, note that we only parse the rapid-kl bus data
    private final String STATIC_ZIP_URL = "https://api.data.gov.my/gtfs-static/prasarana?category=rapid-bus-kl";

    public GtfsStaticService(BusStopRepository busStopRepository) {
        this.busStopRepository = busStopRepository;

        //(later uncomment) this.busRouteRepository = busRouteRepository;
    }

    @PostConstruct 
    
    public void init() {
        System.out.println("🚀 GTFS Service Initialized!");
        importStaticData(); // Call your download/parse method here
    }

    public void importStaticData() {
        if (busStopRepository.count() == 0) {
            System.out.println("Database is empty. Downloading and extracting GTFS ZIP from API...");
            downloadAndParseZip();
        } else {
            System.out.println("Static GTFS Data already exists. Skipping download.");
        }
    }

    private void downloadAndParseZip() {
        try {
            //open connection to API and download the ZIP file )
            URL url = URI.create(STATIC_ZIP_URL).toURL();
            InputStream in = url.openStream();

            //to let database auto read the zip file
            ZipInputStream zis = new ZipInputStream(in);
            ZipEntry entry;

            //iterate every file
            while ((entry = zis.getNextEntry()) != null) {
                String fileName = entry.getName();

                if (fileName.equals("stops.txt")) {
                    System.out.println("Found stops.txt! Parsing now...");
                    parseStops(zis);
                } 
                else if (fileName.equals("routes.txt")) {
                    System.out.println("Found routes.txt! Parsing now...");
                    // parseRoutes(zis); // You will build this next!
                }
                
                // Close the current file inside the zip and move to the next
                zis.closeEntry();
            }

            zis.close();
            System.out.println("Finished processing the ZIP file!");
        } catch (Exception e) {
            System.err.println("Failed to download or parse ZIP: " + e.getMessage());
        }
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
}
