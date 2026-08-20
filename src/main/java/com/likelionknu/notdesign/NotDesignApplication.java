package com.likelionknu.notdesign;

import com.likelionknu.notdesign.common.ai.AiFallbackProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import java.util.TimeZone;

@SpringBootApplication
@EnableJpaAuditing
@EnableConfigurationProperties(AiFallbackProperties.class)
public class NotDesignApplication {

    @PostConstruct
    public void init() {
        // 애플리케이션 전역 기본 타임존을 한국 시간(KST)으로 고정
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

    public static void main(String[] args) {
        SpringApplication.run(NotDesignApplication.class, args);
    }

}
