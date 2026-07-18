package com.mt.friotrackapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FriotrackapiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FriotrackapiApplication.class, args);
    }
}
