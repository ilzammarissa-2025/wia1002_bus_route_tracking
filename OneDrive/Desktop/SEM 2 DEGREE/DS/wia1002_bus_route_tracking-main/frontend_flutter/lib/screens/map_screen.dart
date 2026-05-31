
import 'package:flutter/material.dart';
import '../widgets/bus_stop_bottom_sheet.dart';

class MapScreen extends StatelessWidget {
  const MapScreen({super.key});
  
  // This would be your actual map implementation
  // For demonstration, showing how to show the bottom sheet
  
  void _onBusStopTap(BuildContext context, String stopId, String stopName, String? stopCode) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => BusStopBottomSheet(
        stopId: stopId,
        stopName: stopName,
        stopCode: stopCode,
      ),
    );
  }
  
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: ElevatedButton(
          onPressed: () => _onBusStopTap(
            context,
            'STOP123',
            'Central Station Bus Stop',
            'A12',
          ),
          child: const Text('Show Bus Stop Details'),
        ),
      ),
    );
  }
}