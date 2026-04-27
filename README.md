
# 🚌  Bus Tracker Intro (**REST API** using **Java Spring Boot**)

This API will act as the "brain" for Flutter mobile app (after we have completed the backend main logic ), securely fetching geographic bus data from our database and sending it to the users' phones.

## 🛠️ 1. The Required Toolkit (Prerequisites)
Before you write any code, ensure your local environment has the following installed:

1.  **Java Development Kit (JDK):** Version 25 (This is my version, make sure your version can support the current dependencies).
2.  **IDE:** Visual Studio Code (VS Code) or any you comfort with.
3.  **VS Code Extensions:**
    * **Extension Pack for Java** (by Microsoft)
    * **Spring Boot Extension Pack** (by VMware)
    * **Lombok Annotations Support** (helps keep our code clean)
4.  **Database Engine:** PostgreSQL 17.
5.  **Spatial Extension:** PostGIS (Installed via Stack Builder, this allows PostgreSQL to understand GPS coordinates).
6.  **Database Manager:** pgAdmin 4 (To view our tables visually).
7.  **API Tester:** Postman or any you comfort with (To test our web endpoints, the CRUD without needing the Flutter app).

---

## 📦 2. The Tech Stack & `pom.xml` Explained
Spring Boot uses **Maven** to manage external libraries. All our project dependencies are listed in the `pom.xml` file. When you first open the project, Maven will automatically download these libraries.

Here is what powers our project:
* **Spring Web:** The core web server (Apache Tomcat). It allows us to create URLs (endpoints) that the Flutter app can call.
* **Spring Data JPA:** The "translator." It writes SQL queries for us so we can interact with the database using pure Java.
* **PostgreSQL Driver:** The physical connection bridge between our Java app and the pgAdmin database.
* **Hibernate Spatial:** **(Critical)** This extension teaches Spring Boot how to read PostGIS geographic data like `Point` (for bus stops) and `LineString` (for bus routes).
* **Lombok:** A tool that auto-generates repetitive Java code (like Getters, Setters, and Constructors) using simple annotations like `@Data`.
* **Spring Boot DevTools:** Automatically restarts our local server every time we save a file, saving us tons of time.

---

## 🏗️ 3. Our File Structure & Architecture
We use a standard **Layered Architecture**:

```text
src/main/java/com/example/wia1002_bus_route_tracking/
│
├── Wia1002BusRouteTrackingApplication.java       # Main starting point
│
├── config/                          # Configuration files
│   └── WebSocketConfig.java         # (For later) STOMP setup
│
├── model/                           # Database Entities (Tables)
│   ├── entity/                      
│   │   ├── BusStop.java             # Maps to 'bus_stops' table
│   │   ├── BusRoute.java            # Maps to 'bus_routes' table
│   │   └── BusPosition.java         # Maps to 'bus_positions' table
│   └── dto/                         # Data Transfer Objects (What you send to Flutter)
│       └── NearestBusResponse.java  
│
├── repository/                      # Database Access (Spring Data JPA)
│   ├── BusStopRepository.java       # Interfaces with ST_Distance queries
│   └── BusPositionRepository.java   
│
├── service/                         # Business Logic & Algorithms
│   ├── GtfsStaticService.java       # Logic to read .txt files and save to DB
│   ├── GtfsRealtimeService.java     # Logic to fetch live API data
│   └── SpatialLogicService.java     # Logic to calculate ETA and nearest stops
│
└── controller/                      # REST API Endpoints (URLs)
    ├── BusController.java           # e.g., GET /api/buses/nearest
    └── RouteController.java         # e.g., GET /api/routes/{id}
```

### The Layers Explained:
1.  **`model/entity/` (The Database Blueprint):** These are Java classes (like `BusStop.java`) that perfectly mirror our database tables. An `@Entity` annotation tells Spring, "This class represents a row in PostgreSQL." *Note: The Lead has already set these up!*
2.  **`repository/` (The Database Whisperer):** We do not write raw `SELECT * FROM...` queries. Instead, we create Java Interfaces here. If we need to find a bus stop, we simply call `busStopRepository.findById()`. For spatial queries, we write custom PostGIS commands here, like `ST_DWithin` to find nearby buses.
3.  **`service/` (The Brains / Business Logic):** This is where the heavy lifting happens. The Service layer asks the Repository for data, processes it, calculates the Estimated Time of Arrival (ETA), or parses the GTFS text files. **Rules of the Service layer:** It never talks to the web directly; it only crunches numbers and logic.
4.  **`controller/` (The Front Desk):** This handles incoming HTTP requests from the Flutter app. A Controller receives a request (e.g., `GET /api/buses/nearest?lat=3.1&lon=101.6`), asks the Service layer for the answer, and hands that answer back to the mobile app in JSON format.
5.  **`model/dto/` (Data Transfer Objects):** Sometimes, our database Entities contain sensitive or bulky data we don't want to send to the phone. A DTO is a lightweight, custom-built object (e.g., `NearestBusResponse`) that carries *only* the specific data the Flutter screen needs.

---

## 🐛 4. Testing & Debugging Guide

### How to Run the App
1.  Open the **Spring Boot Dashboard** in the VS Code sidebar.
2.  Click the **Play** button next to our project name.
3.  <img width="345" height="823" alt="image" src="https://github.com/user-attachments/assets/71beb9f1-3da5-4562-89f4-41455c137214" />
4.  Use incognito mode window tab , copy paste to `http://localhost:8080/hello` (prefer use incognito mode).Here's the result:
6.  <img width="1109" height="372" alt="image" src="https://github.com/user-attachments/assets/c3293b17-9316-4341-976e-e89d1bbbb8c1" />
5.  Watch the terminal. If you see `Tomcat started on port 8080`, the server is alive! 

### How to Test an Endpoint
Use **Postman** or your browser.
1.  Open your browser (Use Incognito mode to avoid cookie errors).
2.  Navigate to `http://localhost:8080/hello` (or whatever endpoint you just built).
3.  If it returns text or JSON data, your Controller is working perfectly.

### How to debug
Check your terminal (open at full screen for readability):
* **Read from the Bottom Up:** Scroll to the bottom of the red terminal text and look for the words `Caused by:`. This is usually the exact line of code that broke.
* **Common Error - "Port 8080 already in use":** You left the server running in the background. Stop the terminal (Ctrl+C or the red square icon) and restart.
* **Use the Debugger (The Bug Icon):** Instead of hitting "Play", click the "Debug" icon. You can click to the left of any line number in VS Code to place a red "Breakpoint." The code will freeze when it hits that line, allowing you to hover over variables and see exactly what data they hold in real-time.
* **Smart Logging:** Avoid using `System.out.println()`. Since we have Lombok, add `@Slf4j` to the top of your class and use `log.info("Fetching bus data...");`. This prints a clean, timestamped log in the console.

***

