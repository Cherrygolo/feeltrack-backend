package ld.feeltrack_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
* Global CORS configuration for the application.
*
* This class defines Cross-Origin Resource Sharing (CORS) rules 
* to allow frontends from different domains to access the API.
* 
* Best practices applied:
* - Authorised origins are configurable via the property 
* {@code app.cors.allowed-origins} in application.properties.
* - Authorised HTTP methods are limited to common REST methods.
*
* Usage:
* - Define the authorised domains in application.properties.
* - Changes to authorised domains can only be made in 
* application.properties without modifying the code.
*/

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}