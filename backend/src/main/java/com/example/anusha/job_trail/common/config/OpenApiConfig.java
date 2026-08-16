package com.example.anusha.job_trail.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI jobTrailOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("JobTrail API")
                        .description("Job application tracking system — auth, application CRUD, status pipeline, analytics.")
                        .version("v0.0.1"))
                // Lets Swagger UI's "Authorize" button (and anything that
                // imports this spec, e.g. Postman) attach a Bearer token to
                // every request without hand-adding the header each time.
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
