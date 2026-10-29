package com.example.linky.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
        info = @Info(
                contact = @Contact(
                        name = "Linky APIs",
                        email = "adelaalimani1@gmail.com",
                        url = "https://adelaal.com"
                ),
                description = "LINKY authentication and REST APIs",
                title = "OpenApi specification",
                version = "1.0",
                license = @License(
                        name = "Licence name",
                        url = "https://adelaal.com/licence"),
                termsOfService = "Terms of service"),
        servers = {
                @Server(
                        description = "Active ENV",
                        url = "${server.public-url:http://localhost:8080}"
                )
        },
        security = {
                @SecurityRequirement(
                        name = "bearerAuth"
                )
        }
)
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT auth description",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenAPIConfig {
}
