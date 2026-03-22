package com.cloudrc.server.service;

import com.cloudrc.server.enums.CarStatus;
import com.cloudrc.server.exception.DuplicateResourceException;
import com.cloudrc.server.exception.ResourceNotFoundException;
import com.cloudrc.server.model.Car;
import com.cloudrc.server.repository.CarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class CarService {
    @Autowired
    CarRepository carRepository;

    public Car registerCar(String name, Long carId){
        if(carRepository.existsById(carId)){
            throw new DuplicateResourceException("Car already exists: " + carId);
        }
        Car car = new Car();
        car.setName(name);
        car.setId(carId);

       return carRepository.save(car);
    }

    public List<Car> getAllCars(){
        return carRepository.findAll();
    }

    public Car getCarById(Long carId){
        return carRepository.findById(carId).orElseThrow(()->new ResourceNotFoundException("Car not found") );
    }

    public Car updateCarStatus(Long carId, CarStatus status){
        Car car = getCarById(carId);
        car.setStatus(status);
        car.setLastSeen(LocalDateTime.now());
        return carRepository.save(car);


    }

}

