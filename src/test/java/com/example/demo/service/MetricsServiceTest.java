package com.example.demo.service;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    @Mock
    private MeterRegistry mockMeterRegistry;

    private MetricsService metricsService;
    private SimpleMeterRegistry simpleMeterRegistry;

    @BeforeEach
    void setUp() {
        simpleMeterRegistry = new SimpleMeterRegistry();
        metricsService = new MetricsService(simpleMeterRegistry);
    }

    @Test
    void testRecordApiCall_Success() {
        // When
        metricsService.recordApiCall(true);

        // Then
        Counter successCounter = simpleMeterRegistry.get("api_calls_successful_total").counter();
        Counter totalCounter = simpleMeterRegistry.get("api_calls_total").counter();
        
        assertEquals(1.0, successCounter.count());
        assertEquals(1.0, totalCounter.count());
    }

    @Test
    void testRecordApiCall_Failure() {
        // When
        metricsService.recordApiCall(false);

        // Then
        Counter failureCounter = simpleMeterRegistry.get("api_calls_failed_total").counter();
        Counter totalCounter = simpleMeterRegistry.get("api_calls_total").counter();
        
        assertEquals(1.0, failureCounter.count());
        assertEquals(1.0, totalCounter.count());
    }

    @Test
    void testRecordApiCall_MultipleCalls() {
        // When
        metricsService.recordApiCall(true);
        metricsService.recordApiCall(true);
        metricsService.recordApiCall(false);
        metricsService.recordApiCall(true);

        // Then
        Counter successCounter = simpleMeterRegistry.get("api_calls_successful_total").counter();
        Counter failureCounter = simpleMeterRegistry.get("api_calls_failed_total").counter();
        Counter totalCounter = simpleMeterRegistry.get("api_calls_total").counter();
        
        assertEquals(3.0, successCounter.count());
        assertEquals(1.0, failureCounter.count());
        assertEquals(4.0, totalCounter.count());
    }

    @Test
    void testSetActiveUsers() {
        // When
        metricsService.setActiveUsers(5);

        // Then
        Gauge activeUsersGauge = simpleMeterRegistry.get("active_users").gauge();
        assertEquals(5.0, activeUsersGauge.value());
    }

    @Test
    void testSetTotalUsers() {
        // When
        metricsService.setTotalUsers(10);

        // Then
        Gauge totalUsersGauge = simpleMeterRegistry.get("total_users").gauge();
        assertEquals(10.0, totalUsersGauge.value());
    }

    @Test
    void testStartApiResponseTimer() {
        // When
        Timer.Sample sample = metricsService.startApiResponseTimer();

        // Then
        assertNotNull(sample);
        assertTrue(sample instanceof Timer.Sample);
    }

    @Test
    void testStopApiResponseTimer() {
        // Given
        Timer.Sample sample = metricsService.startApiResponseTimer();

        // When
        metricsService.stopApiResponseTimer(sample);

        // Then
        Timer apiResponseTimer = simpleMeterRegistry.get("api_response_time").timer();
        assertEquals(1, apiResponseTimer.count());
        assertTrue(apiResponseTimer.totalTime(TimeUnit.NANOSECONDS) > 0);
    }

    @Test
    void testStartDatabaseQueryTimer() {
        // When
        Timer.Sample sample = metricsService.startDatabaseQueryTimer();

        // Then
        assertNotNull(sample);
        assertTrue(sample instanceof Timer.Sample);
    }

    @Test
    void testStopDatabaseQueryTimer() {
        // Given
        Timer.Sample sample = metricsService.startDatabaseQueryTimer();

        // When
        metricsService.stopDatabaseQueryTimer(sample);

        // Then
        Timer databaseQueryTimer = simpleMeterRegistry.get("database_query_time").timer();
        assertEquals(1, databaseQueryTimer.count());
        assertTrue(databaseQueryTimer.totalTime(TimeUnit.NANOSECONDS) > 0);
    }

    @Test
    void testRecordRequestSize() {
        // Given
        int requestSize = 1024;

        // When
        metricsService.recordRequestSize(requestSize);

        // Then
        DistributionSummary requestSizeMeter = simpleMeterRegistry.get("request_size_bytes")
                .summary();
        assertNotNull(requestSizeMeter);
        assertEquals(1, requestSizeMeter.count());
        assertEquals(requestSize, requestSizeMeter.totalAmount(), 0.01);
    }

    @Test
    void testRecordResponseSize() {
        // Given
        int responseSize = 2048;

        // When
        metricsService.recordResponseSize(responseSize);

        // Then
        DistributionSummary responseSizeMeter = simpleMeterRegistry.get("response_size_bytes")
                .summary();
        assertNotNull(responseSizeMeter);
        assertEquals(1, responseSizeMeter.count());
        assertEquals(responseSize, responseSizeMeter.totalAmount(), 0.01);
    }

    @Test
    void testRecordBusinessMetric() {
        // When
        metricsService.recordBusinessMetric("user_registration", 42.5, "status", "success");

        // Then
        Gauge businessMetricGauge = simpleMeterRegistry.get("business_user_registration").gauge();
        assertEquals(42.5, businessMetricGauge.value());
    }

    @Test
    void testGetMetricsSummary() {
        // Given
        metricsService.recordApiCall(true);
        metricsService.recordApiCall(false);
        metricsService.setActiveUsers(3);
        metricsService.setTotalUsers(10);

        // When
        String summary = metricsService.getMetricsSummary();

        // Then
        assertNotNull(summary);
        assertTrue(summary.contains("Total API Calls: 2"));
        assertTrue(summary.contains("Successful API Calls: 1"));
        assertTrue(summary.contains("Failed API Calls: 1"));
        assertTrue(summary.contains("Active Users: 3"));
        assertTrue(summary.contains("Total Users: 10"));
    }

    @Test
    void testMetricsServiceConstructor_RegistersAllMetrics() {
        // Then - verify all expected metrics are registered
        assertNotNull(simpleMeterRegistry.get("api_calls_total"));
        assertNotNull(simpleMeterRegistry.get("api_calls_successful_total"));
        assertNotNull(simpleMeterRegistry.get("api_calls_failed_total"));
        assertNotNull(simpleMeterRegistry.get("active_users"));
        assertNotNull(simpleMeterRegistry.get("total_users"));
        assertNotNull(simpleMeterRegistry.get("api_response_time"));
        assertNotNull(simpleMeterRegistry.get("database_query_time"));
        assertNotNull(simpleMeterRegistry.get("request_size_bytes"));
        assertNotNull(simpleMeterRegistry.get("response_size_bytes"));
    }

    @Test
    void testMultipleTimerStops() {
        // Given
        Timer.Sample sample1 = metricsService.startApiResponseTimer();
        Timer.Sample sample2 = metricsService.startApiResponseTimer();

        // When
        metricsService.stopApiResponseTimer(sample1);
        metricsService.stopApiResponseTimer(sample2);

        // Then
        Timer apiResponseTimer = simpleMeterRegistry.get("api_response_time").timer();
        assertEquals(2, apiResponseTimer.count());
    }

    @Test
    void testGaugeValuesUpdate() {
        // Given
        metricsService.setActiveUsers(5);
        metricsService.setTotalUsers(20);

        // When
        metricsService.setActiveUsers(8);
        metricsService.setTotalUsers(25);

        // Then
        Gauge activeUsersGauge = simpleMeterRegistry.get("active_users").gauge();
        Gauge totalUsersGauge = simpleMeterRegistry.get("total_users").gauge();
        
        assertEquals(8.0, activeUsersGauge.value());
        assertEquals(25.0, totalUsersGauge.value());
    }
}
