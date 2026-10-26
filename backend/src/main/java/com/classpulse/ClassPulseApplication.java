package com.classpulse;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.classpulse.mapper")
@EnableAsync
public class ClassPulseApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClassPulseApplication.class, args);
    }
}
