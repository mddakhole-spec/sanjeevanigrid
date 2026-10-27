package com.sanjeevanigrid;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SanjeevaniGridApplication {

    public static void main(String[] args) {
        SpringApplication.run(SanjeevaniGridApplication.class, args);
    }
}
