import 'dart:async';
import 'package:http/http.dart' as http;
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'dart:convert';

class RouteSearchService {
  // Debouncing: Timer to delay API calls
  static Timer? _debounceTimer;
  
  // This function implements debouncing - waits 300ms after user stops typing
  static Future<List<String>> searchRoutesWithDebounce(
    String searchText,
    Function(List<String>) onResults,
  ) async {
    // Cancel previous timer if user is still typing
    if (_debounceTimer != null && _debounceTimer!.isActive) {
      _debounceTimer!.cancel();
    }
    
    // If search is empty, return empty list immediately
    if (searchText.isEmpty) {
      onResults([]);
      return [];
    }
    
    // Start new timer - wait 300ms before searching
    return Future.delayed(const Duration(milliseconds: 300), () async {
      final results = await _fetchRouteSuggestions(searchText);
      onResults(results);
      return results;
    });
  }
  
  // Actual API call to backend
  static Future<List<String>> _fetchRouteSuggestions(String prefix) async {
    final baseUrl = dotenv.env['API_BASE_URL'] ?? 'http://localhost:8080';
    final url = Uri.parse('$baseUrl/api/routes/search?prefix=${Uri.encodeComponent(prefix)}');
    
    try {
      final response = await http.get(url);
      
      if (response.statusCode == 200) {
        final List<dynamic> data = json.decode(response.body);
        return data.map((item) => item.toString().toUpperCase()).toList();
      }
      
      print('Error: ${response.statusCode}');
      return [];
      
    } catch (e) {
      print('Network error: $e');
      return [];
    }
  }
  
  // Optional: Pre-fetch all routes for client-side filtering (faster but more memory)
  static Future<List<String>> prefetchAllRoutes() async {
    final baseUrl = dotenv.env['API_BASE_URL'] ?? 'http://localhost:8080';
    final url = Uri.parse('$baseUrl/api/routes');
    
    try {
      final response = await http.get(url);
      if (response.statusCode == 200) {
        final List<dynamic> data = json.decode(response.body);
        return data.map((r) => (r['shortId'] ?? r['routeId'] ?? '').toString().toUpperCase()).toList();
      }
    } catch (e) {
      print('Error prefetching: $e');
    }
    return [];
  }
}