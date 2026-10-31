package com.example.linky.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;

@OpenAPIDefinition(
        info = @Info(
                title = "Linky API",
                version = "1.0.0",
                description = """
                        REST API for Linky, a URL shortening service.

                        Features:
                        - Create and manage shortened URLs
                        - Redirect visitors to original URLs
                        - JWT-based authentication and role-based authorization
                        - Redis caching and rate limiting
                        - Track link usage with total and daily click statistics

                        Authentication:
                        Use the Authorize button to provide a JWT access token.
                        """
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT access token obtained from the login endpoint"
)
public class OpenAPIConfig {
}