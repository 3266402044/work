package com.classpulse.config;

import com.classpulse.service.DemoDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {
    private final DemoDataService demoDataService;

    @Override
    public void run(ApplicationArguments args) {
        demoDataService.ensureSeed();
        log.info("ClassPulse 初始化数据检查完成");
    }
}
