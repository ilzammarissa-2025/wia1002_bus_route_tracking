package com.example.wia1002_bus_route_tracking;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
    @GetMapping("/hello")
    public String sayHello() {
        return "Hello, World! The Bus Tracker is alive.";
    }
}

//this is just a test controller to check if the application is running correctly. It will return a simple message when the /hello endpoint is accessed.