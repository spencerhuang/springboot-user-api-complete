package com.example.demo.controller;

import com.example.demo.service.AuthService;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "APIs for user authentication and JWT token management")
@Validated
public class AuthController {

    @Autowired
    private AuthService authService;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    private Counter loginAttemptsCounter;
    private Counter loginSuccessCounter;
    private Counter loginFailureCounter;
    
    @PostConstruct
    public void init() {
        this.loginAttemptsCounter = Counter.builder("auth_login_attempts_total")
                .description("Total number of login attempts")
                .register(meterRegistry);
        this.loginSuccessCounter = Counter.builder("auth_login_success_total")
                .description("Total number of successful logins")
                .register(meterRegistry);
        this.loginFailureCounter = Counter.builder("auth_login_failure_total")
                .description("Total number of failed login attempts")
                .register(meterRegistry);
    }

    @PostMapping("/login")
    @Timed(value = "auth.login.time", description = "Time taken to process login")
    @Operation(
        summary = "User login",
        description = "Authenticate a user and return a JWT token for subsequent API calls"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input - username is required"),
        @ApiResponse(responseCode = "401", description = "Authentication failed"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> login(
        @Parameter(description = "Username for authentication", example = "john_doe", required = true)
        @RequestParam @NotBlank(message = "Username is required") String username
    ) {
        try {
            loginAttemptsCounter.increment();
            
            Map<String, Object> response = authService.authenticateUser(username);
            loginSuccessCounter.increment();
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            loginFailureCounter.increment();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            loginFailureCounter.increment();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Login failed", "message", e.getMessage()));
        }
    }
    
    @PostMapping("/validate")
    @Timed(value = "auth.validate.time", description = "Time taken to validate token")
    @Operation(
        summary = "Validate JWT token",
        description = "Validate a JWT token and return token information"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token is valid",
            content = @Content(schema = @Schema(implementation = TokenValidationResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid or expired token"),
        @ApiResponse(responseCode = "400", description = "Token not provided")
    })
    public ResponseEntity<?> validateToken(
        @Parameter(description = "JWT token to validate", required = true)
        @RequestHeader("Authorization") String authorization
    ) {
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid authorization header format. Use 'Bearer <token>'"));
            }
            
            String token = authorization.substring(7);
            boolean isValid = authService.validateToken(token);
            
            if (isValid) {
                TokenValidationResponse response = new TokenValidationResponse();
                response.setValid(true);
                response.setMessage("Token is valid");
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or expired token"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token validation failed", "message", e.getMessage()));
        }
    }
    
    @PostMapping("/logout")
    @Timed(value = "auth.logout.time", description = "Time taken to logout user")
    @Operation(
        summary = "User logout",
        description = "Logout a user and clear their cached session"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Logout successful"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<?> logout(
        @Parameter(description = "Username to logout", required = true)
        @RequestParam String username
    ) {
        try {
            authService.logoutUser(username);
            
            Map<String, Object> response = Map.of("message", "User logged out successfully", "username", username);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Logout failed", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/session/{username}")
    @Timed(value = "auth.session.time", description = "Time taken to get user session")
    @Operation(
        summary = "Get user session",
        description = "Get cached user session information"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Session retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<?> getUserSession(
        @Parameter(description = "Username", example = "john_doe")
        @PathVariable String username
    ) {
        try {
            Map<String, Object> session = authService.getUserSession(username);
            
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get session", "message", e.getMessage()));
        }
    }
    
    @PostMapping("/session/refresh/{username}")
    @Timed(value = "auth.refresh.time", description = "Time taken to refresh user session")
    @Operation(
        summary = "Refresh user session",
        description = "Refresh user session and update cache"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Session refreshed successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<?> refreshUserSession(
        @Parameter(description = "Username", example = "john_doe")
        @PathVariable String username
    ) {
        try {
            Map<String, Object> session = authService.refreshUserSession(username);
            
            return ResponseEntity.ok(session);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to refresh session", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    @Operation(
        summary = "Authentication service health check",
        description = "Check if the authentication service is running properly"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service is healthy")
    })
    public ResponseEntity<?> health() {
        try {
            Map<String, Object> response = Map.of(
                "status", "UP",
                "service", "Authentication Service",
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Health check failed", "message", e.getMessage()));
        }
    }
    
    @PostMapping("/cache/clear")
    @Timed(value = "auth.cache.clear.time", description = "Time taken to clear auth caches")
    @Operation(
        summary = "Clear authentication caches",
        description = "Clear all cached authentication data (admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Caches cleared successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<?> clearAuthCaches() {
        try {
            authService.clearAllAuthCaches();
            
            Map<String, Object> response = Map.of("message", "Authentication caches cleared successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to clear caches", "message", e.getMessage()));
        }
    }
    
    // Response models for Swagger documentation
    @Schema(description = "Login response containing JWT token")
    public static class LoginResponse {
        @Schema(description = "JWT token for authentication", example = "eyJhbGciOiJIUzI1NiJ9...")
        private String token;
        
        @Schema(description = "Username of the authenticated user", example = "john_doe")
        private String username;
        
        @Schema(description = "Response message", example = "Login successful")
        private String message;
        
        @Schema(description = "Token type", example = "Bearer")
        private String tokenType;
        
        @Schema(description = "Token expiration time", example = "1 hour")
        private String expiresIn;
        
        // Getters and setters
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }
        
        public String getExpiresIn() { return expiresIn; }
        public void setExpiresIn(String expiresIn) { this.expiresIn = expiresIn; }
    }
    
    @Schema(description = "Token validation response")
    public static class TokenValidationResponse {
        @Schema(description = "Whether the token is valid", example = "true")
        private boolean valid;
        
        @Schema(description = "Validation message", example = "Token is valid")
        private String message;
        
        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}