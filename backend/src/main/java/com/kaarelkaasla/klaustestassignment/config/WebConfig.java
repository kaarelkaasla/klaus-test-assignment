package com.kaarelkaasla.klaustestassignment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration class for setting up web-related configurations, such as CORS (Cross-Origin Resource Sharing) settings.
 */
@Configuration
public class WebConfig {

    /**
     * Configures CORS settings for the application.
     *
     * @return a {@link WebMvcConfigurer} that defines the CORS configuration
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {

            /**
             * Adds CORS mappings to allow specified origins to access the API.
             *
             * @param registry
             *            the {@link CorsRegistry} to add the CORS mappings to
             */
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/v1/**").allowedOriginPatterns("*") // Allow all origins for testing purposes
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS").allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
