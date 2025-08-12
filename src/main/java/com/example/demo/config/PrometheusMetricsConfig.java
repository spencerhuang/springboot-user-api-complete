package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PrometheusMetricsConfig {

    @Value("${prometheus.server.url:http://localhost:9090}")
    private String prometheusServerUrl;
    
    @Value("${prometheus.server.push.enabled:false}")
    private boolean pushEnabled;
    
    @Value("${prometheus.server.push.interval:15s}")
    private String pushInterval;
    
    @Value("${management.metrics.export.prometheus.enabled:true}")
    private boolean prometheusEnabled;
    
    @Value("${management.metrics.export.prometheus.descriptions:true}")
    private boolean descriptionsEnabled;
    
    @Value("${management.metrics.export.prometheus.step:1m}")
    private String step;

    /**
     * Get Prometheus server configuration information
     */
    public String getPrometheusServerUrl() {
        return prometheusServerUrl;
    }
    
    public boolean isPushEnabled() {
        return pushEnabled;
    }
    
    public String getPushInterval() {
        return pushInterval;
    }
    
    public boolean isPrometheusEnabled() {
        return prometheusEnabled;
    }
    
    public boolean isDescriptionsEnabled() {
        return descriptionsEnabled;
    }
    
    public String getStep() {
        return step;
    }
}
