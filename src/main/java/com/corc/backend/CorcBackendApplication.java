package com.corc.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class CorcBackendApplication {

    public static void main(String[] args) {
        System.out.println("JAI SHREE RAM");
        SpringApplication.run(CorcBackendApplication.class, args);
        System.out.println("JAI SHREE RAM");
    }
}
