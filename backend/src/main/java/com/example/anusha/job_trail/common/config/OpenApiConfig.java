package com.example.anusha.job_trail.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI jobTrailOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("JobTrail API")
                        .description("Job application tracking system — auth, application CRUD, status pipeline, analytics.")
                        .version("v0.0.1"));
    }
}
