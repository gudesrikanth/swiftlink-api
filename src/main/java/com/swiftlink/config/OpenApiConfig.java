package com.swiftlink.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI swiftLinkOpenAPI(
            @Value("${swiftlink.base-url}") String baseUrl,
            @Value("${spring.application.version:1.0.0}") String version
    ) {
        return new OpenAPI()
                .info(new Info()
                        .title("SwiftLink API")
                        .description("Production-grade URL shortener with analytics. Shorten URLs, track clicks, analyse traffic.")
                        .version(version)
                        .contact(new Contact().name("SwiftLink").url(baseUrl))
                        .license(new License().name("MIT")))
                .servers(List.of(new Server().url(baseUrl).description("Current server")));
    }
}
