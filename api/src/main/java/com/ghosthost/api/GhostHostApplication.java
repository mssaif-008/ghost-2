package com.ghosthost.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Ghost Host — Mini Managed Static Deployment Platform
 *
 * Main entry point. @EnableScheduling allows the BuildWorker
 * to poll the in-memory queue on a fixed interval.
 */
@SpringBootApplication
@EnableScheduling
public class GhostHostApplication {

    public static void main(String[] args) {
        SpringApplication.run(GhostHostApplication.class, args);
    }
}
