package com.befapress;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * BEFA Press - Breaking Ethiopian Facts & Articles
 * Main Application Entry Point
 */
@SpringBootApplication
@EnableScheduling
public class BefaPressApplication {

    public static void main(String[] args) {
        SpringApplication.run(BefaPressApplication.class, args);
    }
}
