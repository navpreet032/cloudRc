package com.cloudrc.server.repository;

import com.cloudrc.server.enums.CarStatus;
import com.cloudrc.server.model.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface CarRepository extends JpaRepository<Car, Long> {

    public List<Car> findByStatus(CarStatus status);

}
