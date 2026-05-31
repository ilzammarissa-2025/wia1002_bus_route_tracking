// lib/models/bus_arrival.dart
class BusArrival {
  final String busNumber;
  final String registrationNumber;
  final int minutesToArrival;
  final String stopId;
  
  BusArrival({
    required this.busNumber,
    required this.registrationNumber,
    required this.minutesToArrival,
    required this.stopId,
  });
  
  factory BusArrival.fromJson(Map<String, dynamic> json) {
    return BusArrival(
      busNumber: json['busNumber'] ?? json['bus_number'] ?? 'Unknown',
      registrationNumber: json['registrationNumber'] ?? json['registration_number'] ?? 'Unknown',
      minutesToArrival: json['minutesToArrival'] ?? json['minutes_to_arrival'] ?? 0,
      stopId: json['stopId'] ?? json['stop_id'] ?? '',
    );
  }
  
  String get displayText => 'Bus $busNumber ($registrationNumber) - Arriving in $minutesToArrival mins';
  
  String get shortDisplayText => 'Bus $busNumber - ${minutesToArrival}min';
}