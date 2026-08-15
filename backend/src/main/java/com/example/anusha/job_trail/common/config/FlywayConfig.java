package com.example.anusha.job_trail.common.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Manual Flyway wiring. Spring Boot 4's autoconfigure module no longer
 * ships a FlywayAutoConfiguration, so migration has to be triggered
 * explicitly, and Hibernate's EntityManagerFactory has to be told to wait
 * for it — otherwise {@code ddl-auto: validate} can race Flyway and validate
 * against a schema that hasn't been migrated yet.
 */
@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure(getClass().getClassLoader())
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
    }

    @Bean
    static EntityManagerFactoryDependsOnPostProcessor entityManagerFactoryDependsOnFlywayPostProcessor() {
        return new EntityManagerFactoryDependsOnPostProcessor("flyway");
    }
}
