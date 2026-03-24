package com.cloudrc.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.security.Principal;

@Controller
public class WebRtcSignalingController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Android → Backend → Browser
    // Android sends its WebRTC offer
    // Browser receives it on /topic/webrtc/offer/{carId}
    @MessageMapping("/webrtc/offer/{carId}")
    public void handleOffer(
            @DestinationVariable String carId,
            @Payload String offer,
            Principal principal) {
        messagingTemplate.convertAndSend("/topic/webrtc/offer/" + carId, offer);
    }

    // Browser → Backend → Android
    // Browser sends its WebRTC answer
    // Android receives it on /topic/webrtc/answer/{carId}
    @MessageMapping("/webrtc/answer/{carId}")
    public void handleAnswer(
            @DestinationVariable String carId,
            @Payload String answer) {
        messagingTemplate.convertAndSend("/topic/webrtc/answer/" + carId, answer);
    }

    // Android → Backend → Browser
    // Android sends ICE candidates
    // Browser receives on /topic/webrtc/ice/browser/{carId}
    @MessageMapping("/webrtc/ice/browser/{carId}")
    public void handleIceToBrowser(
            @DestinationVariable String carId,
            @Payload String candidate) {
        messagingTemplate.convertAndSend("/topic/webrtc/ice/browser/" + carId, candidate);
    }

    // Browser → Backend → Android
    // Browser sends ICE candidates
    // Android receives on /topic/webrtc/ice/android/{carId}
    @MessageMapping("/webrtc/ice/android/{carId}")
    public void handleIceToAndroid(
            @DestinationVariable String carId,
            @Payload String candidate) {
        messagingTemplate.convertAndSend("/topic/webrtc/ice/android/" + carId, candidate);
    }
}
