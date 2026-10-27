package com.emailforensics.backend.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class DotenvApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        DotenvLoader.load();
    }
}
