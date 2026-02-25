package com.echill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class EchillBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EchillBackendApplication.class, args);
    }

}
