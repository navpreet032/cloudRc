package com.cloudrc.server.config;

import com.cloudrc.server.websocket.Esp32WsHandler;
import com.cloudrc.server.websocket.StompAuthChannelInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Configuration
@EnableWebSocket
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketConfigurer, WebSocketMessageBrokerConfigurer {

    @Autowired
    private Esp32WsHandler esp32WsHandler;
    @Autowired
    private StompAuthChannelInterceptor stompAuthInterceptor;

    // Raw WebSocket for ESP32
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(esp32WsHandler, "/esp32")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
                        System.out.println("[WS Handshake] URI: " + request.getURI());
                        System.out.println("[WS Handshake] Headers: " + request.getHeaders());
                        return true; // force allow everything
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                               WebSocketHandler wsHandler, Exception exception) {
                        if (exception != null) {
                            System.out.println("[WS Handshake] FAILED: " + exception.getMessage());
                        }
                    }
                });

    }


    // STOMP for browser
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // prefix for messages FROM browser TO server
        registry.setApplicationDestinationPrefixes("/app");

        // prefix for messages FROM server TO browser
        registry.enableSimpleBroker("/topic", "/queue");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")          // browser connects to ws://server/ws
                .setAllowedOriginPatterns("*")
                .withSockJS();               // fallback for older browsers
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthInterceptor);
    }
}