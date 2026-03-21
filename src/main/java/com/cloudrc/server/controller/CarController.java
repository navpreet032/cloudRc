package com.cloudrc.server.controller;

import com.cloudrc.server.dto.RegisterCarPayload;
import com.cloudrc.server.enums.CarStatus;
import com.cloudrc.server.model.Car;
import com.cloudrc.server.service.CarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cars")
public class CarController {
    @Autowired
    private CarService carService;

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


}
