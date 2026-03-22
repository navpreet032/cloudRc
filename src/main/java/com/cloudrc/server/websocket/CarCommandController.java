package com.cloudrc.server.websocket;

import com.cloudrc.server.enums.BookingStatus;
import com.cloudrc.server.model.Booking;
import com.cloudrc.server.model.User;
import com.cloudrc.server.repository.BookingRepository;
import com.cloudrc.server.dto.CarCommandPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.util.Optional;

@Controller
public class CarCommandController {

    @Autowired
    private Esp32WsHandler esp32WsHandler;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Browser sends to /app/car/{carId}/control
    @MessageMapping("/car/{carId}/control")
    public void handleCommand(
            @DestinationVariable Long carId,
            @Payload CarCommandPayload payload,
            Principal principal) throws Exception {

        // 1. Get current user from JWT principal
        User user = (User) ((Authentication) principal).getPrincipal();

        // 2. Check user has ACTIVE booking for this car
        Optional<Booking> booking = bookingRepository
                .findByUserAndBookingStatus(user, BookingStatus.ACTIVE);

        if (booking.isEmpty()) {
            // Send error back to this user only
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    "No active booking"
            );
            return;
        }

        // 3. Check booking is for THIS car
        if (!booking.get().getCar().getId().equals(carId)) {
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    "Not authorized for this car"
            );
            return;
        }

        // 4. Validate values
        float t = payload.getT();
        float s = payload.getS();
        if (t < -1.0 || t > 1.0 || s < -1.0 || s > 1.0) {
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    "Values must be between -1.0 and 1.0"
            );
            return;
        }

        // 5. Forward to ESP32
        String command = String.format("{\"t\":%.2f,\"s\":%.2f}", t, s);
        esp32WsHandler.sendCommand(carId, command);
    }
}