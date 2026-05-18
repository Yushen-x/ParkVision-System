package com.parkvision.cps;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ParkVisionApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParkVisionApplication.class, args);
    }
}
