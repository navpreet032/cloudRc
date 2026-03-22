package com.cloudrc.server.websocket;

import com.cloudrc.server.enums.CarStatus;
import com.cloudrc.server.service.CarService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class Esp32WsHandler extends TextWebSocketHandler {

    // carId → ESP32 session
    private final Map<Long, WebSocketSession> esp32Sessions = new ConcurrentHashMap<>();

    @Autowired
    private CarService carService;

    @Autowired
    private ObjectMapper objectMapper;


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // ESP32 sends carId as query param: ws://server/esp32?carId=1
        String carId = getCarId(session);

        if (carId == null) {
            session.close(CloseStatus.BAD_DATA);
            System.out.println("[ESP32] Connection rejected — no carId");
            return;
        }

        Long carIdLong = Long.parseLong(carId);
        esp32Sessions.put(carIdLong, session);

        carService.updateCarStatus(carIdLong, CarStatus.IDLE);

        System.out.println("[ESP32] Car " + carId + " connected");
    }

    // ── ESP32 sends telemetry
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String carId = getCarId(session);
        System.out.println("[ESP32] Telemetry from car " + carId + ": " + message.getPayload());

        // TODO: parse battery telemetry and update car in DB

    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String carId = getCarId(session);

        if (carId != null) {
            Long carIdLong = Long.parseLong(carId);
            esp32Sessions.remove(carIdLong);

            carService.updateCarStatus(carIdLong, CarStatus.OFFLINE);
            System.out.println("[ESP32] Car " + carId + " disconnected");
        }
    }


    public void sendCommand(Long carId, String command) throws IOException {
        WebSocketSession session = esp32Sessions.get(carId);

        if (!isConnected(carId)) {

            throw new RuntimeException("ESP32 not connected for car: " + carId);

        }

        session.sendMessage(new TextMessage(command));
        System.out.println("[ESP32] Command sent to car " + carId + ": " + command);
    }


    public boolean isConnected(Long carId) {
        WebSocketSession session = esp32Sessions.get(carId);
        return session != null && session.isOpen();
    }


    private String getCarId(WebSocketSession session) {
        String query = session.getUri().getQuery(); // "carId=1"
        if (query == null) return null;

        for (String param : query.split("&")) {
            String[] kv = param.split("=");
            if (kv.length == 2 && kv[0].equals("carId")) {
                return kv[1];
            }
        }
        return null;
    }
}