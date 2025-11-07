package com.example.demo.config;

import com.example.demo.service.MetricsService;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Interceptor to collect metrics for all API requests including:
 * - API response time
 * - Request size
 * - Response size
 * - API call success/failure
 */
@Component
public class MetricsInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(MetricsInterceptor.class);
    private static final String TIMER_SAMPLE_ATTRIBUTE = "metrics.timer.sample";
    private static final String REQUEST_START_TIME_ATTRIBUTE = "metrics.request.start.time";

    @Autowired
    private MetricsService metricsService;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        // Start timer for API response time
        Timer.Sample timerSample = Timer.start(metricsService.getMeterRegistry());
        request.setAttribute(TIMER_SAMPLE_ATTRIBUTE, timerSample);
        
        // Record request start time
        request.setAttribute(REQUEST_START_TIME_ATTRIBUTE, System.currentTimeMillis());
        
        // Record request size
        int contentLength = request.getContentLength();
        if (contentLength > 0) {
            metricsService.recordRequestSize(contentLength);
        }
        
        return true;
    }

    @Override
    public void postHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, 
                          @Nullable ModelAndView modelAndView) {
        // This method is called after the handler is executed but before the view is rendered
        // We don't need to do anything here as we'll handle everything in afterCompletion
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, 
                                @NonNull Object handler, @Nullable Exception ex) {
        // Stop the timer and record API response time
        Timer.Sample timerSample = (Timer.Sample) request.getAttribute(TIMER_SAMPLE_ATTRIBUTE);
        if (timerSample != null) {
            timerSample.stop(metricsService.getApiResponseTimer());
        }
        
        // Record response size
        // Note: For response size, we need to check if there's a way to get it
        // Since we can't easily get the actual response body size here,
        // we'll skip response size recording in the interceptor
        // (it can be handled differently if needed)
        
        // Record API call success/failure based on HTTP status code
        int statusCode = response.getStatus();
        boolean isSuccess = statusCode >= 200 && statusCode < 400;
        metricsService.recordApiCall(isSuccess);
        
        // Log if there was an exception
        if (ex != null) {
            logger.debug("Request completed with exception: {}", ex.getMessage());
        }
    }
}

