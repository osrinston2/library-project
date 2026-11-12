package com.library.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI libraryOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Library Management System API")
                        .description("RESTful API for registering borrowers and books, listing catalog copies, and managing book loans.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Library Engineering Team")
                                .email("support@library.example.com")));
    }
}

