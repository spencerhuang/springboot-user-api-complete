package com.example.demo.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MetricsConfig {

    /**
     * Enhanced meter registry with common tags for better metric organization
     */
    @Bean
    @Primary
    public MeterRegistry enhancedMeterRegistry(MeterRegistry meterRegistry) {
        // Add common tags for better metric organization
        meterRegistry.config().commonTags("application", "user-api");
        meterRegistry.config().commonTags("version", "1.0.0");
        meterRegistry.config().commonTags("environment", "development");
        
        return meterRegistry;
    }
}
