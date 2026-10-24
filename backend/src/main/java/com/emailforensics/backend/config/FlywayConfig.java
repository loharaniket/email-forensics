package com.emailforensics.backend.config;

import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
        return flyway;
    }

    @Bean
    public static BeanFactoryPostProcessor dependsOnPostProcessor() {
        return beanFactory -> {
            String[] names = beanFactory.getBeanNamesForType(EntityManagerFactory.class);
            for (String name : names) {
                beanFactory.getBeanDefinition(name).setDependsOn("flyway");
            }
        };
    }
}
