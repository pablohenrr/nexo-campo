package com.nexocampo;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NexoCampoApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexoCampoApplication.class, args);
    }
}
