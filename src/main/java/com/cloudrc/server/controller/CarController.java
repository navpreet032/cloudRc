package com.cloudrc.server.controller;

import com.cloudrc.server.dto.CameraAuthRequest;
import com.cloudrc.server.dto.RegisterCarPayload;
import com.cloudrc.server.enums.BookingStatus;
import com.cloudrc.server.enums.CarStatus;
import com.cloudrc.server.exception.InvalidCredentialsException;
import com.cloudrc.server.model.Booking;
import com.cloudrc.server.model.Car;
import com.cloudrc.server.repository.BookingRepository;
import com.cloudrc.server.service.BookingService;
import com.cloudrc.server.service.CarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/cars")
public class CarController {
    @Autowired
    private CarService carService;

    @Autowired
    private BookingRepository bookingRepository;

    @Value("${cloudrc.cam.secret}")
    private String camSecret;

    @PostMapping("/register")
    public ResponseEntity<Car> registerCar(@RequestBody RegisterCarPayload payload) {
        return ResponseEntity.status(200).body(carService.registerCar(payload.getName(), payload.getCarId()));
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<Car>> getAllCars(){
        return ResponseEntity.ok(carService.getAllCars());
    }

    @GetMapping("/{carId}")
    public ResponseEntity<Car> getCarById(@PathVariable Long carId){
        return ResponseEntity.ok(carService.getCarById(carId));
    }


    @PostMapping("/validate-cam")
    public ResponseEntity<?> validateCameraAccess(@RequestBody CameraAuthRequest request) {
        if (!request.getAdminToken().equals(camSecret)) {
            throw new InvalidCredentialsException("Invalid admin token");
        }
        Car car = carService.getCarById(Long.parseLong(request.getCarId()));
        Optional<Booking> active = bookingRepository
                .findByCarAndBookingStatus(car, BookingStatus.ACTIVE);
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "carId", car.getId(),
                "hasActiveDriver", active.isPresent()
        ));
    }


}
