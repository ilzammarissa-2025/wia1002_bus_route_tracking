// lib/widgets/smart_search_bar.dart
import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

class SmartSearchBar extends StatefulWidget {
  final Function(String routeId) onRouteSelected;
  final Function(String searchText) onLocationSearch;

  const SmartSearchBar({
    Key? key,
    required this.onRouteSelected,
    required this.onLocationSearch,
  }) : super(key: key);

  @override
  State<SmartSearchBar> createState() => _SmartSearchBarState();
}

class _SmartSearchBarState extends State<SmartSearchBar> {
  final TextEditingController _controller = TextEditingController();
  final FocusNode _focusNode = FocusNode();
  List<dynamic> _suggestions = [];
  List<dynamic> _popularRoutes = [];
  bool _isLoading = false;
  bool _showSuggestions = false;
  Timer? _debounceTimer;

  @override
  void initState() {
    super.initState();
    _fetchPopularRoutes();
  }

  @override
  void dispose() {
    _debounceTimer?.cancel();
    _controller.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  Future<void> _fetchPopularRoutes() async {
    try {
      const baseUrl = String.fromEnvironment('API_BASE_URL', defaultValue: 'http://127.0.0.1:8080');
      final response = await http.get(Uri.parse('$baseUrl/api/search/popular'));
      
      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        setState(() {
          _popularRoutes = data['popular'] ?? [];
        });
      }
    } catch (e) {
      print('Error fetching popular routes: $e');
    }
  }

  Future<void> _searchAutocomplete(String query) async {
    if (query.length < 2) {
      setState(() {
        _suggestions = [];
        _showSuggestions = false;
      });
      return;
    }

    setState(() {
      _isLoading = true;
    });

    try {
      const baseUrl = String.fromEnvironment('API_BASE_URL', defaultValue: 'http://127.0.0.1:8080');
      final response = await http.get(
        Uri.parse('$baseUrl/api/search/autocomplete?q=${Uri.encodeComponent(query)}')
      );
      
      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        setState(() {
          _suggestions = data['suggestions'] ?? [];
          _showSuggestions = true;
          _isLoading = false;
        });
      } else {
        setState(() {
          _suggestions = [];
          _isLoading = false;
        });
      }
    } catch (e) {
      setState(() {
        _suggestions = [];
        _isLoading = false;
      });
    }
  }

  Future<void> _fullSearch(String query) async {
    setState(() {
      _isLoading = true;
    });

    try {
      const baseUrl = String.fromEnvironment('API_BASE_URL', defaultValue: 'http://127.0.0.1:8080');
      final response = await http.get(
        Uri.parse('$baseUrl/api/search/search?q=${Uri.encodeComponent(query)}')
      );
      
      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        final suggestions = data['suggestions'] ?? [];
        
        if (suggestions.isNotEmpty) {
          // Show results in a dialog
          _showSearchResultsDialog(suggestions, data['searchSuggestions']);
        } else {
          // If no route found, treat as location search
          widget.onLocationSearch(query);
        }
      }
    } catch (e) {
      print('Search error: $e');
    } finally {
      setState(() {
        _isLoading = false;
        _showSuggestions = false;
      });
    }
  }

  void _showSearchResultsDialog(List<dynamic> results, List<dynamic> suggestions) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Search Results'),
        content: Container(
          width: double.maxFinite,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (suggestions.isNotEmpty) ...[
                Container(
                  padding: EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: Colors.blue.shade50,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '💡 Did you mean?',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                      SizedBox(height: 8),
                      Wrap(
                        spacing: 8,
                        children: suggestions.map((suggestion) {
                          return ActionChip(
                            label: Text(suggestion.toString()),
                            onPressed: () {
                              Navigator.pop(context);
                              _controller.text = suggestion.toString();
                              _fullSearch(suggestion.toString());
                            },
                          );
                        }).toList(),
                      ),
                    ],
                  ),
                ),
                SizedBox(height: 16),
              ],
              Expanded(
                child: ListView.builder(
                  shrinkWrap: true,
                  itemCount: results.length,
                  itemBuilder: (context, index) {
                    final route = results[index];
                    return ListTile(
                      leading: CircleAvatar(
                        backgroundColor: Colors.blue.shade100,
                        child: Text(
                          route['shortName'],
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            color: Colors.blue.shade800,
                          ),
                        ),
                      ),
                      title: Text('Route ${route['shortName']}'),
                      subtitle: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            route['longName'],
                            style: TextStyle(fontSize: 12),
                          ),
                          if (route['matchScore'] != null)
                            Container(
                              margin: EdgeInsets.only(top: 4),
                              child: LinearProgressIndicator(
                                value: route['matchScore'] / 100,
                                backgroundColor: Colors.grey.shade200,
                                color: Colors.green,
                              ),
                            ),
                        ],
                      ),
                      trailing: Icon(Icons.arrow_forward, color: Colors.blue),
                      onTap: () {
                        Navigator.pop(context);
                        widget.onRouteSelected(route['shortName']);
                      },
                    );
                  },
                ),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text('Close'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: Colors.black12,
            blurRadius: 8,
            offset: Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        children: [
          TextField(
            controller: _controller,
            focusNode: _focusNode,
            onChanged: (value) {
              if (_debounceTimer?.isActive ?? false) _debounceTimer?.cancel();
              _debounceTimer = Timer(Duration(milliseconds: 300), () {
                _searchAutocomplete(value);
              });
            },
            onSubmitted: (value) {
              _fullSearch(value);
              _focusNode.unfocus();
            },
            decoration: InputDecoration(
              hintText: 'Search bus route (e.g., 190, 960, 851)...',
              prefixIcon: Icon(Icons.search, color: Colors.blue),
              suffixIcon: _isLoading
                  ? Padding(
                      padding: EdgeInsets.all(12),
                      child: SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      ),
                    )
                  : _controller.text.isNotEmpty
                      ? IconButton(
                          icon: Icon(Icons.clear),
                          onPressed: () {
                            _controller.clear();
                            setState(() {
                              _suggestions = [];
                              _showSuggestions = false;
                            });
                          },
                        )
                      : null,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: BorderSide.none,
              ),
              filled: true,
              fillColor: Colors.white,
            ),
          ),
          
          // Autocomplete suggestions
          if (_showSuggestions && _suggestions.isNotEmpty)
            Container(
              margin: EdgeInsets.only(top: 4),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(12),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black12,
                    blurRadius: 4,
                  ),
                ],
              ),
              child: ListView.builder(
                shrinkWrap: true,
                itemCount: _suggestions.length,
                itemBuilder: (context, index) {
                  final suggestion = _suggestions[index];
                  return ListTile(
                    leading: Icon(Icons.route, color: Colors.blue, size: 20),
                    title: Text(
                      suggestion['displayText'],
                      style: TextStyle(fontSize: 14),
                    ),
                    subtitle: Text(
                      '${suggestion['origin']} → ${suggestion['destination']}',
                      style: TextStyle(fontSize: 12, color: Colors.grey),
                    ),
                    trailing: Icon(Icons.arrow_forward, size: 16),
                    onTap: () {
                      _controller.text = suggestion['shortName'];
                      _fullSearch(suggestion['shortName']);
                      setState(() {
                        _showSuggestions = false;
                      });
                      _focusNode.unfocus();
                    },
                  );
                },
              ),
            ),
          
          // Popular routes section (when search bar is focused but empty)
          if (_focusNode.hasFocus && _controller.text.isEmpty && !_showSuggestions)
            Container(
              margin: EdgeInsets.only(top: 8),
              padding: EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    '🔥 Popular Routes',
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                      color: Colors.grey[600],
                    ),
                  ),
                  SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    runSpacing: 8,
                    children: _popularRoutes.map((route) {
                      return ActionChip(
                        label: Text(route['shortName']),
                        onPressed: () {
                          _controller.text = route['shortName'];
                          _fullSearch(route['shortName']);
                          _focusNode.unfocus();
                        },
                        backgroundColor: Colors.blue.shade50,
                        labelStyle: TextStyle(color: Colors.blue.shade700),
                      );
                    }).toList(),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }
}