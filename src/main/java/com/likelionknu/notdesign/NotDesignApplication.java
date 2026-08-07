package com.likelionknu.notdesign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class NotDesignApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotDesignApplication.class, args);
    }

}
