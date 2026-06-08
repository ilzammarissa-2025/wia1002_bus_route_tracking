import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'dart:async';

// Initialize the google map api before app boots up
Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await dotenv.load(fileName: '.env'); 
  runApp(
    MaterialApp(
      title: 'Bus Route Tracking',
      debugShowCheckedModeBanner: false,
      home:MainMapScreen(),
    )
  );
}

class MainMapScreen extends StatefulWidget {
  const MainMapScreen({super.key});
 
  @override
  State<MainMapScreen> createState() => _MainMapScreenState();
}

class _MainMapScreenState extends State<MainMapScreen> {
  final GlobalKey<ScaffoldState> _scaffoldKey = GlobalKey<ScaffoldState>(); //allow drawer open when user taps the menu button
  final Set<Polyline> _mapPolylines = {};
  final Set<Marker> _busMarkers = {}; 
  GoogleMapController? _mapController;

  String _currentRoute = '';
  List<dynamic> _globalLiveBuses = [];

  BitmapDescriptor? _busIcon;
  BitmapDescriptor? _stopIcon;

  Timer? _busUpdateTimer;


  //NEW VARIABLES FOR SMART SEARCH:
  final TextEditingController _searchController = TextEditingController();
  final FocusNode _searchFocusNode = FocusNode();
  List<String> _routeSuggestions = [];
  bool _isSearching = false;
  Timer? _debounceTimer;

  @override
  void initState() {
    super.initState();
    _createCustomMarkers();

    // ADD THIS LINE - listen to search text changes
  _searchController.addListener(() {
    _searchRoutesWithDebounce(_searchController.text);
  });
  
    //auto-refresh bus positions every 20 seconds 
    _busUpdateTimer = Timer.periodic(const Duration(seconds: 20), (timer) => _fetchActiveBuses());
  }

  @override 
  void dispose() {
    _busUpdateTimer?.cancel(); //stop the timer when the widget is disposed
    _debounceTimer?.cancel();
  _searchController.dispose();
  _searchFocusNode.dispose();
    super.dispose();
  }

  Future<void> _createCustomMarkers() async {
    //get screen's pixel ratio so the image is in hig-res
    final ImageConfiguration config = createLocalImageConfiguration(context);
    
    // 🚨 FIXED: Removed "frontend_flutter/" from the path! Flutter reads relative to pubspec.yaml
    _busIcon = await BitmapDescriptor.fromAssetImage(config, 'assets/icons/bus.png');
    _stopIcon = await BitmapDescriptor.fromAssetImage(config, 'assets/icons/location.png');
    
    //trigger rebuild 
    setState(() {}); 

    //force map to redraw the icon instantly with custom icon when the map fetched buses
    if (_globalLiveBuses.isNotEmpty) {
      _fetchActiveBuses();
    }
  }

  Future<void> _searchLocation(String searchText) async {
    if (searchText.isEmpty) return;
    final baseUrl = dotenv.env['API_BASE_URL'] ?? 'http://localhost:8080';
    final url = Uri.parse('$baseUrl/api/map/search?query=${Uri.encodeComponent(searchText)}');

    try {
      final response = await http.get(url);
      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        if (data['places'] != null && (data['places'] as List).isNotEmpty) {
          final location = data['places'][0]['location'];
          _drawRedMarker((location['latitude'] as num).toDouble(), (location['longitude'] as num).toDouble(), searchText);
        } else if (data['results'] != null && (data['results'] as List).isNotEmpty) {
          final location = data['results'][0]['geometry']['location'];
          _drawRedMarker((location['lat'] as num).toDouble(), (location['lng'] as num).toDouble(), searchText);
        }
      }
    } catch (e) {
      print('Error fetching location: $e');
    }
  }

  void _drawRedMarker(double lat, double lon, String title) {
    setState(() {
      _busMarkers.removeWhere((marker) => marker.markerId.value == 'search_result');
      _busMarkers.add(
        Marker(
          markerId: MarkerId('search_result'),
          position: LatLng(lat, lon),
          infoWindow: InfoWindow(title: title),
          icon: BitmapDescriptor.defaultMarkerWithHue(BitmapDescriptor.hueRed),
        ),
      );
    });
    _mapController?.animateCamera(CameraUpdate.newLatLngZoom(LatLng(lat, lon), 16.0));
  }

//Note: placing setState() inside a loop can cause performance issues.For larger datasets, consider batching updates or using a more efficient state management approach.
  Future<void> _fetchActiveBuses() async {
    final baseUrl = dotenv.env['API_BASE_URL'] ?? 'http://localhost:8080';
    final url = Uri.parse('$baseUrl/api/bus/locations');

    try {
      final response = await http.get(url);
      if (response.statusCode == 200) {
        final List<dynamic> liveBuses = json.decode(response.body);
        setState(() {
          _globalLiveBuses = liveBuses;
          _busMarkers.removeWhere((marker) => marker.markerId.value.startsWith('bus_'));
          
          for (var bus in liveBuses) {
            final String busId = bus['id'] ?? 'unknown';
            final double lat = (bus['latitude'] as num?)?.toDouble() ?? 3.1209;
            final double lon = (bus['longitude'] as num?)?.toDouble() ?? 101.6538;

            final String nextStop = bus['nextStop'] ?? '';

            final double eta = (bus['etaToNextStop'] as num?)?.toDouble() ?? 0.0;
            
            String snippetText = nextStop.isNotEmpty ? 'Next: $nextStop' : 'Calculating...';

            if (eta > 0.0) {
              snippetText += '\nETA: ${eta.toStringAsFixed(0)} min';
            }           
            _busMarkers.add(Marker(
              markerId: MarkerId('bus_$busId'), 
              position: LatLng(lat, lon), 
              icon: _busIcon ?? BitmapDescriptor.defaultMarker, 
              infoWindow: InfoWindow(
                title: 'Bus $busId', 
                snippet: snippetText,
              ),
            ));
          }
        });
      }
    } catch (e) {
      print('Error fetching active buses: $e');
    }
  }

  Future<void> _fetchRouteStops(String routeId) async {
    if (routeId.isEmpty) return;
    _currentRoute = routeId.toUpperCase();
    _fetchRoutePath(routeId); // Fetch and display the route path on the map
    final baseUrl = dotenv.env['API_BASE_URL'] ?? 'http://127.0.0.1:8080';
    final url = Uri.parse('$baseUrl/api/routes/${routeId.toUpperCase()}/stops');

    try {
      final response = await http.get(url);
      if (response.statusCode == 200) {
        final List<dynamic> stops = json.decode(response.body);
        setState(() {
          _busMarkers.removeWhere((marker) => marker.markerId.value.startsWith('stop_'));
          for (var stop in stops) {
            final String stopId = stop['stopId'] ?? 'unknown';
            final double lat = (stop['latitude'] as num).toDouble();
            final double lon = (stop['longitude'] as num).toDouble();
            final marker = Marker(
              markerId : MarkerId('stop_$stopId'),
              position: LatLng(lat, lon),
              icon: _stopIcon ?? BitmapDescriptor.defaultMarker,
              infoWindow: InfoWindow(title: 'Stop: ${stop['stopName']}'),
              onTap: () => _openArrivalBoard(stopId),
            );
            _busMarkers.add(marker);
          }
        });

        if (stops.isNotEmpty) {
          var activeBus;
          try {
            activeBus = _globalLiveBuses.firstWhere(
              (bus) => bus['routeId'] == _currentRoute || bus['routeShortName'] == _currentRoute,
            );
          } catch (e) {
            activeBus = null;
          }

          double targetLat = activeBus != null ? activeBus['latitude'] : stops[0]['latitude'];
          double targetLon = activeBus != null ? activeBus['longitude'] : stops[0]['longitude'];

          _mapController?.animateCamera(
            CameraUpdate.newLatLngZoom(LatLng(targetLat, targetLon), 15.5),
          );

          Future.delayed(const Duration(milliseconds: 500), () {
            _openArrivalBoard(stops[0]['stopId'].toString());
          });
        }
      }
    } catch (e) {
      print('Error fetching route stops: $e');
    }
  }

  Future<void> _fetchRoutePath(String routeId) async {
    if (routeId.isEmpty) return;
    
    final baseUrl = dotenv.env['API_BASE_URL'] ?? 'http://127.0.0.1:8080';
    final url = Uri.parse('$baseUrl/api/routes/${routeId.toUpperCase()}/path');

    try {
      final response = await http.get(url);
      if (response.statusCode == 200) {
        final List<dynamic> data = json.decode(response.body);
        
        List<LatLng> polylineCoordinates = [];
        for (var point in data) {
          polylineCoordinates.add(
            LatLng(
              (point['latitude'] as num).toDouble(),
              (point['longitude'] as num).toDouble(),
            ),
          );
        }

        setState(() {
          // Clear previous paths to prevent line overlapping
          _mapPolylines.clear();
          
          // Construct the new smooth route polyline
          _mapPolylines.add(
            Polyline(
              polylineId: PolylineId('route_${routeId.toLowerCase()}'),
              points: polylineCoordinates,
              color: const Color(0xFF1A73E8), // Primary transit blue accent
              width: 5,                       // Line thickness on screen
              jointType: JointType.round,     // Smooths out sharp angular turns
              startCap: Cap.roundCap,
              endCap: Cap.roundCap,
            ),
          );
        });
      }
    } catch (e) {
      print('Error fetching route polylines: $e');
    }
  }

  //request GPS permission, grab live coordinates and calls backend endpoint
  Future<List<dynamic>> _fetchNearestStops() async{
    bool serviceEnabled = await Geolocator.isLocationServiceEnabled();
    if (!serviceEnabled) {
      return Future.error('Location services are disabled.');
    }

    LocationPermission permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
      if (permission == LocationPermission.denied) {
        return Future.error('Location permissions are denied');
      }
    }

    //get live GPS
    Position position = await Geolocator.getCurrentPosition(desiredAccuracy: LocationAccuracy.high);

    // Call the backend with a 1500 meter (1.5km) radius
    final baseUrl = dotenv.env['API_BASE_URL'] ?? 'http://127.0.0.1:8080';
    final url = Uri.parse('$baseUrl/api/bus/nearest?lat=${position.latitude}&lon=${position.longitude}&radius=1500');

    final response = await http.get(url);
    if (response.statusCode == 200) {
      final List<dynamic> data = json.decode(response.body);
      // Sort the list so the absolute closest stop is at the very top
      data.sort((a, b) => (a['distanceMeters'] as num).compareTo(b['distanceMeters'] as num));
      return data;
    }
    throw Exception('Failed to load nearest stops');
  }

  Widget _buildNearMeDrawer() {
    return Drawer(
      child: Column(
        children: [
          Container(
            width: double.infinity,
            padding: const EdgeInsets.only(top: 60, bottom: 20, left: 20),
            color: const Color(0xFF1A73E8), // Beautiful transit blue
            child: const Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(Icons.near_me, color: Colors.white, size: 32),
                SizedBox(height: 10),
                Text('Nearby Stops', style: TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold)),
                Text('Within 1.5 km of your location', style: TextStyle(color: Colors.white70, fontSize: 14)),
              ],
            ),
          ),
          Expanded(
            child: FutureBuilder<List<dynamic>>(
              future: _fetchNearestStops(),
              builder: (context, snapshot) {
                if (snapshot.connectionState == ConnectionState.waiting) {
                  return const Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        CircularProgressIndicator(),
                        SizedBox(height: 16),
                        Text('Finding your location...'),
                      ],
                    )
                  );
                } else if (snapshot.hasError) {
                  return Center(child: Text('Error: ${snapshot.error}', textAlign: TextAlign.center));
                } else if (!snapshot.hasData || snapshot.data!.isEmpty) {
                  return const Center(child: Text('No bus stops found near you.'));
                }

                final stops = snapshot.data!;
                return ListView.separated(
                  padding: const EdgeInsets.all(8),
                  itemCount: stops.length,
                  separatorBuilder: (context, index) => const Divider(),
                  itemBuilder: (context, index) {
                    final stop = stops[index];
                    final String stopName = stop['stopName'] ?? 'Unknown Stop';
                    final String stopId = stop['stopId'] ?? '';
                    final double distance = (stop['distanceMeters'] as num).toDouble();
                    
                    // Convert meters to kilometers for a cleaner UI
                    final String distanceStr = distance > 1000 
                        ? '${(distance / 1000).toStringAsFixed(1)} km' 
                        : '${distance.toStringAsFixed(0)} m';

                    return ListTile(
                      leading: const CircleAvatar(
                        backgroundColor: Colors.cyan,
                        child: Icon(Icons.directions_bus, color: Colors.white, size: 20),
                      ),
                      title: Text(stopName, style: const TextStyle(fontWeight: FontWeight.bold)),
                      subtitle: Text('Distance: $distanceStr'),
                      trailing: const Icon(Icons.chevron_right, color: Colors.grey),
                      onTap: () {
                        // 1. Close the drawer
                        Navigator.pop(context);
                        
                        // 2. Pan the camera smoothly to the selected stop
                        _mapController?.animateCamera(
                          CameraUpdate.newLatLngZoom(
                            LatLng(stop['latitude'], stop['longitude']), 
                            16.5
                          ),
                        );
                        
                        // 3. Open the Arrival Board instantly!
                        Future.delayed(const Duration(milliseconds: 300), () {
                          // Clear current route so arrival board fetches all buses for this stop
                          _currentRoute = ''; 
                          _openArrivalBoard(stopId);
                        });
                      },
                    );
                  },
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      key: _scaffoldKey,
      drawer: _buildNearMeDrawer(),
      body: Stack(
        children: [
          GoogleMap(
            initialCameraPosition: const CameraPosition(
              target: LatLng(3.1209, 101.6538), 
              zoom: 15.0,
            ),
            markers: Set<Marker>.of(_busMarkers), //pass new Set to force repaint 
            polylines: _mapPolylines,
            onMapCreated: (GoogleMapController controller) {
              _mapController = controller;
              _fetchActiveBuses(); 
            },
          ),
        Positioned(
  top: 50,
  left: 15,
  right: 15,
  child: Column(
    children: [
      Container(
        padding: const EdgeInsets.symmetric(horizontal: 16),
        decoration: BoxDecoration(
          color: Colors.white.withValues(alpha: 0.95),
          borderRadius: BorderRadius.circular(8),
          boxShadow: const [
            BoxShadow(blurRadius: 10, offset: Offset(0, 4), color: Colors.black26),
          ],
        ),
        child: Row(
          children: [
            // Hamburger menu button
            IconButton(
              icon: const Icon(Icons.menu, color: Colors.black87),
              onPressed: () {
                _scaffoldKey.currentState?.openDrawer();
              },
            ),
            // Smart search TextField with debouncing
            Expanded(
              child: TextField(
                controller: _searchController,
                focusNode: _searchFocusNode,
                onSubmitted: (value) {
                  // When user presses enter, handle the search
                  if (_routeSuggestions.isNotEmpty) {
                    _fetchRouteStops(_routeSuggestions.first);
                    _searchController.text = _routeSuggestions.first;
                    setState(() => _routeSuggestions = []);
                  } else if (RegExp(r'^[A-Za-z0-9]+$').hasMatch(value.trim())) {
                    _fetchRouteStops(value.trim());
                  } else {
                    _searchLocation(value.trim());
                  }
                  _searchFocusNode.unfocus();
                },
                decoration: InputDecoration(
                  hintText: 'Search route (e.g., "T7" shows T788, T789...)',
                  border: InputBorder.none,
                  suffixIcon: _isSearching
                      ? const SizedBox(
                          width: 20,
                          height: 20,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.search),
                ),
              ),
            ),
          ],
        ),
      ),
      
      // Suggestions Dropdown (appears when user types)
      if (_routeSuggestions.isNotEmpty && _searchController.text.isNotEmpty)
        Container(
          margin: const EdgeInsets.only(top: 8),
          constraints: const BoxConstraints(maxHeight: 300),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(8),
            boxShadow: const [
              BoxShadow(blurRadius: 8, offset: Offset(0, 2), color: Colors.black26),
            ],
          ),
          child: ListView.separated(
            shrinkWrap: true,
            itemCount: _routeSuggestions.length,
            separatorBuilder: (context, index) => const Divider(height: 1),
            itemBuilder: (context, index) {
              final route = _routeSuggestions[index];
              final query = _searchController.text.toUpperCase();
              
              // Highlight the matching part
              bool isExactMatch = route == query;
              bool isPrefixMatch = route.startsWith(query);
              
              return ListTile(
                leading: const Icon(Icons.directions_bus, color: Colors.blue),
                title: RichText(
                  text: TextSpan(
                    text: route,
                    style: const TextStyle(
                      fontWeight: FontWeight.w500,
                      color: Colors.black87,
                      fontSize: 16,
                    ),
                    children: [
                      if (isExactMatch || isPrefixMatch)
                        TextSpan(
                          text: ' ${isExactMatch ? '✓ Exact match' : '• Suggested'}',
                          style: TextStyle(
                            fontSize: 12,
                            color: isExactMatch ? Colors.green : Colors.orange,
                            fontWeight: FontWeight.normal,
                          ),
                        ),
                    ],
                  ),
                ),
                trailing: Chip(
                  label: Text(
                    isExactMatch ? 'EXACT' : 'SIMILAR',
                    style: const TextStyle(fontSize: 10),
                  ),
                  backgroundColor: isExactMatch ? Colors.green[100] : Colors.blue[100],
                ),
                onTap: () {
                  // Select the suggestion
                  _fetchRouteStops(route);
                  _searchController.text = route;
                  setState(() => _routeSuggestions = []);
                  _searchFocusNode.unfocus();
                },
              );
            },
          ),
        ),
    ],
  ),
),
        Align(
          alignment: Alignment.bottomRight,
          child:Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                FloatingActionButton(
                  onPressed: () {
                    _mapController?.animateCamera(CameraUpdate.newLatLngZoom(const LatLng(3.1209, 101.6538), 15.0));
                  },
                  child: const Icon(Icons.my_location),
                ),
                const SizedBox(height: 10),
                FloatingActionButton(
                  onPressed: () {
                    _fetchActiveBuses();
                },
                  child: const Icon(Icons.refresh),
                ),
              ],
             ),
            ),
          ),
        ],
      ),
    );
  }

  String _formatTo12Hour(String timeString) {
    try {
      final parts = timeString.split(':');
      int hour = int.parse(parts[0]);
      int minute = int.parse(parts[1]);
      String period = hour >= 12 ? 'PM' : 'AM';
      if (hour > 12) hour -= 12;
      if (hour == 0) hour = 12;

      String minuteStr = minute.toString().padLeft(2, '0');
      String hourStr = hour.toString().padLeft(2, '0');

      return '$hourStr:$minuteStr $period';
    } catch (e) {
      return timeString; 
    }
  }

  void _openArrivalBoard(String stopId) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true, //allow the sheet to expand for the grid 
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (BuildContext context) {
        return Container(
          height: MediaQuery.of(context).size.height * 0.6,
          padding: const EdgeInsets.all(20.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Header matching your design
              Row(
                children: [
                  const Icon(Icons.directions_bus, color: Colors.indigo),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      'Bus Stop: $stopId', 
                      style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 20),

              Expanded(
                child: FutureBuilder<http.Response>(
                  future: http.get(Uri.parse('${dotenv.env['API_BASE_URL'] ?? 'http://127.0.0.1:8080'}/api/bus/arrivals?routeId=$_currentRoute&stopId=$stopId')),
                  builder: (context, snapshot) {
                    if (snapshot.connectionState == ConnectionState.waiting) {
                      return const Center(child: CircularProgressIndicator());
                    } else if (snapshot.hasError || snapshot.data?.statusCode != 200) {
                      return const Center(child: Text('Error loading arrivals.'));
                    }
                    final List<dynamic> arrivals = json.decode(snapshot.data!.body);
                    if (arrivals.isEmpty) {
                      return const Center(child: Text('No arrivals found.'));
                    } 

                    // Check if the data is live or scheduled based on the first item
                    final bool isLive = arrivals[0]['isLive'] ?? false;

                    //live GPS tracking
                    if (isLive) {
                      return ListView.builder(
                        itemCount: arrivals.length,
                        itemBuilder: (context, index) {
                          final bus = arrivals[index];
                          final String busIdentifier = bus['routeShortName'] ?? 'Unknown';
                          final String etaString = bus['eta']?.toString() ?? 'N/A';

                          return ListTile(
                            leading: const Icon(Icons.directions_bus, color: Colors.orange),
                            title: Text('Bus $busIdentifier'),
                            trailing: Text(
                              'Live: $etaString min',
                              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: Colors.green[700])
                            ),
                          );
                        },
                      );
                    }

                    // static schedule grid
                    else {
                      final nextBusTime = _formatTo12Hour(arrivals[0]['eta'].toString());
                      
                      return Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          // Highlight Card for the Next Bus
                          Container(
                            width: double.infinity,
                            padding: const EdgeInsets.all(20),
                            decoration: BoxDecoration(
                              color: Colors.grey[100],
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const Text('Next bus will depart at', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
                                const SizedBox(height: 8),
                                Text(nextBusTime, style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Colors.black87)),
                              ],
                            ),
                          ),
                          const SizedBox(height: 24),
                          
                          const Text('Scheduled:', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                          const SizedBox(height: 12),

                          // Grid view for the remaining times
                          Expanded(
                            child: GridView.builder(
                              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                                crossAxisCount: 4, // 4 columns just like your image
                                childAspectRatio: 2.5,
                                crossAxisSpacing: 8,
                                mainAxisSpacing: 8,
                              ),
                              itemCount: arrivals.length,
                              itemBuilder: (context, index) {
                                final timeStr = _formatTo12Hour(arrivals[index]['eta'].toString());
                                return Center(
                                  child: Text(
                                    timeStr,
                                    style: const TextStyle(fontSize: 14, color: Colors.black54, fontWeight: FontWeight.w500),
                                 ),
                                );
                              },
                            ),
                          ),
                        ],
                      );
                    }
                  },
                ),
              )
            ],
          )
        );
      },
    );
    }

    // this method to handle debounced search
Future<void> _searchRoutesWithDebounce(String query) async {
  // Cancel previous timer
  if (_debounceTimer != null && _debounceTimer!.isActive) {
    _debounceTimer!.cancel();
  }
  
  // If query is empty, clear suggestions
  if (query.isEmpty) {
    setState(() {
      _routeSuggestions = [];
      _isSearching = false;
    });
    return;
  }
  
  // Set loading state
  setState(() => _isSearching = true);
  
  // Start debounce timer (300ms delay as required)
  _debounceTimer = Timer(const Duration(milliseconds: 300), () async {
    final suggestions = await _fetchRouteSuggestions(query);
    
    if (mounted) {
      setState(() {
        _routeSuggestions = suggestions;
        _isSearching = false;
      });
    }
  });
}

//  this method to call backend API
Future<List<String>> _fetchRouteSuggestions(String prefix) async {
  final baseUrl = dotenv.env['API_BASE_URL'] ?? 'http://localhost:8080';
  final url = Uri.parse('$baseUrl/api/routes/search?prefix=${Uri.encodeComponent(prefix)}');
  
  try {
    final response = await http.get(url);
    if (response.statusCode == 200) {
      final List<dynamic> data = json.decode(response.body);
      return data.map((item) => item.toString().toUpperCase()).toList();
    }
    return [];
  } catch (e) {
    print('Error fetching route suggestions: $e');
    return [];
  }
}

  }
