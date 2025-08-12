package com.example.demo.service;

import io.micrometer.core.instrument.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;
    
    // Custom counters
    private final Counter totalApiCalls;
    private final Counter successfulApiCalls;
    private final Counter failedApiCalls;
    
    // Custom gauges
    private final AtomicInteger activeUsers;
    private final AtomicInteger totalUsers;
    
    // Custom timers
    private final Timer apiResponseTime;
    private final Timer databaseQueryTime;
    
    // Custom distribution summaries
    private final DistributionSummary requestSize;
    private final DistributionSummary responseSize;

    @Autowired
    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters
        this.totalApiCalls = Counter.builder("api_calls_total")
                .description("Total number of API calls")
                .tag("type", "all")
                .register(meterRegistry);
                
        this.successfulApiCalls = Counter.builder("api_calls_successful_total")
                .description("Total number of successful API calls")
                .tag("type", "success")
                .register(meterRegistry);
                
        this.failedApiCalls = Counter.builder("api_calls_failed_total")
                .description("Total number of failed API calls")
                .tag("type", "failure")
                .register(meterRegistry);
        
        // Initialize gauges
        this.activeUsers = meterRegistry.gauge("active_users", 
                new AtomicInteger(0), AtomicInteger::get);
        this.totalUsers = meterRegistry.gauge("total_users", 
                new AtomicInteger(0), AtomicInteger::get);
        
        // Initialize timers
        this.apiResponseTime = Timer.builder("api_response_time")
                .description("API response time")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
                
        this.databaseQueryTime = Timer.builder("database_query_time")
                .description("Database query execution time")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
        
        // Initialize distribution summaries
        this.requestSize = DistributionSummary.builder("request_size_bytes")
                .description("Request size in bytes")
                .baseUnit("bytes")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
                
        this.responseSize = DistributionSummary.builder("response_size_bytes")
                .description("Response size in bytes")
                .baseUnit("bytes")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }
    
    /**
     * Record API call metrics
     */
    public void recordApiCall(boolean success) {
        totalApiCalls.increment();
        if (success) {
            successfulApiCalls.increment();
        } else {
            failedApiCalls.increment();
        }
    }
    
    /**
     * Update active users count
     */
    public void setActiveUsers(int count) {
        activeUsers.set(count);
    }
    
    /**
     * Update total users count
     */
    public void setTotalUsers(int count) {
        totalUsers.set(count);
    }
    
    /**
     * Record API response time
     */
    public Timer.Sample startApiResponseTimer() {
        return Timer.start(meterRegistry);
    }
    
    /**
     * Stop API response timer and record the time
     */
    public void stopApiResponseTimer(Timer.Sample sample) {
        sample.stop(apiResponseTime);
    }
    
    /**
     * Record database query time
     */
    public Timer.Sample startDatabaseQueryTimer() {
        return Timer.start(meterRegistry);
    }
    
    /**
     * Stop database query timer and record the time
     */
    public void stopDatabaseQueryTimer(Timer.Sample sample) {
        sample.stop(databaseQueryTime);
    }
    
    /**
     * Record request size
     */
    public void recordRequestSize(long sizeInBytes) {
        requestSize.record(sizeInBytes);
    }
    
    /**
     * Record response size
     */
    public void recordResponseSize(long sizeInBytes) {
        responseSize.record(sizeInBytes);
    }
    
    /**
     * Record custom business metric
     */
    public void recordBusinessMetric(String metricName, double value, String... tags) {
        Gauge.builder("business_" + metricName, () -> value)
                .description("Business metric: " + metricName)
                .tags(tags)
                .register(meterRegistry);
    }
    
    /**
     * Get current metrics summary
     */
    public String getMetricsSummary() {
        return String.format(
            "Metrics Summary:\n" +
            "- Total API Calls: %d\n" +
            "- Successful API Calls: %d\n" +
            "- Failed API Calls: %d\n" +
            "- Active Users: %d\n" +
            "- Total Users: %d",
            (int) totalApiCalls.count(),
            (int) successfulApiCalls.count(),
            (int) failedApiCalls.count(),
            activeUsers.get(),
            totalUsers.get()
        );
    }
}
