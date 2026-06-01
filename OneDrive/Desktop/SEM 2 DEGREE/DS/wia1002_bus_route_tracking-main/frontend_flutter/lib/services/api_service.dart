// lib/services/api_service.dart
// import 'package:http/http.dart' as http;
import '../models/bus_arrival.dart';


class ApiService {
  // Replace with your actual API base URL
  static const String baseUrl = 'http://localhost:3000/api'; // Update for emulator/device
  
  Future<List<BusArrival>> getBusArrivals(String stopId) async {
    // For demo purposes, return mock data if API is not available
    return _getMockArrivals(stopId);
  }
  
  // Mock data for development when API is not ready
  List<BusArrival> _getMockArrivals(String stopId) {
    return [
      BusArrival(
        busNumber: 'WVL614',
        registrationNumber: 'ABC-1234',
        minutesToArrival: 4,
        stopId: stopId,
      ),
      BusArrival(
        busNumber: 'WVL615',
        registrationNumber: 'XYZ-5678',
        minutesToArrival: 7,
        stopId: stopId,
      ),
      BusArrival(
        busNumber: 'WVL616',
        registrationNumber: 'DEF-9012',
        minutesToArrival: 12,
        stopId: stopId,
      ),
    ];
  }
}