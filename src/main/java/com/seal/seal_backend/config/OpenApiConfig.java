package com.seal.seal_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger/OpenAPI metadata (NFR-MNT-02). Owned by Lead. Swagger UI: /swagger-ui.html */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sealOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("SEAL Hackathon Management System API")
                .description("SWP391 — SEAL backend. 6 main-flows split across 3 BE members.")
                .version("0.0.1"))
            .components(new Components().addSecuritySchemes("bearerAuth",
                new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")));
    }
}
