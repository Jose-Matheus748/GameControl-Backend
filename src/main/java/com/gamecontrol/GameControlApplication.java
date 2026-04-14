package com.gamecontrol;

import com.gamecontrol.service.GameService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GameControlApplication {

    public static void main(String[] args) {
        SpringApplication.run(GameControlApplication.class, args);
    }
}