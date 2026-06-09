package com.itr.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPIConfig — Swagger UI configuration for API documentation.
 */
@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("TaxERP — ITR Filing API")
                .version("1.0.0")
                .description("Complete API for Indian income tax return preparation, computation, and filing. "
                    + "Supports AY 2026-27 with both old and new tax regimes.")
                .contact(new Contact()
                    .name("TaxERP Support")
                    .email("support@taxerp.com"))
                .license(new License()
                    .name("Proprietary")));
    }
}
