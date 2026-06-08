package com.example.wia1002_bus_route_tracking.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer; //register websocket endpoint and configure the message broker will handle message delivery

@Configuration
@EnableWebSocketMessageBroker //turn on real-time websockets
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /* "topic" : broadcast messeage sent to /topic/live-buses , every single phone connected to thatchannel receives message 
    */ 
    @Override
    //configureMessageBroker method setup message routing
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic"); //1 to many broadcasting channel.Clients will subscribe to this prefix for messages
        config.setApplicationDestinationPrefixes("/app"); // Prefix for messages sent from clients
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        //setAllowedOrigins disables CORs , ensure mobile app is not rejected by the server's security firewall
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS(); // WebSocket endpoint with old browser(HTTP) fallback
    }
    
}
