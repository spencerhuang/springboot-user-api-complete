package com.example.demo.controller;

import com.example.demo.entity.User;
import com.example.demo.service.MetricsService;
import com.example.demo.service.UserService;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "APIs for managing users in the system")
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private MetricsService metricsService;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    private Counter userCreatedCounter;
    private Counter userUpdatedCounter;
    private Counter userDeletedCounter;
    
    @PostConstruct
    public void init() {
        this.userCreatedCounter = Counter.builder("user_created_total")
                .description("Total number of users created")
                .register(meterRegistry);
        this.userUpdatedCounter = Counter.builder("user_updated_total")
                .description("Total number of users updated")
                .register(meterRegistry);
        this.userDeletedCounter = Counter.builder("user_deleted_total")
                .description("Total number of users deleted")
                .register(meterRegistry);
    }

    @GetMapping
    @Timed(value = "user.list.time", description = "Time taken to list users")
    @Operation(
        summary = "List all users",
        description = "Retrieve a paginated list of all users with optional sorting and filtering"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved users",
            content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> listUsers(
        @Parameter(description = "Page number (0-based)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Number of items per page", example = "10")
        @RequestParam(defaultValue = "10") int size,
        @Parameter(description = "Sorting criteria (field,direction)", example = "id,asc")
        @RequestParam(defaultValue = "id,asc") String[] sort,
        HttpServletRequest request
    ) {
        try {
            // Record request size
            if (request.getContentLength() > 0) {
                metricsService.recordRequestSize(request.getContentLength());
            }
            
            Sort.Direction direction = sort[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
            PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sort[0]));
            Page<User> pageResult = userService.getAllUsers(pageRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("users", pageResult.getContent());
            response.put("currentPage", pageResult.getNumber());
            response.put("totalItems", pageResult.getTotalElements());
            response.put("totalPages", pageResult.getTotalPages());
            response.put("hasNext", pageResult.hasNext());
            response.put("hasPrevious", pageResult.hasPrevious());

            // Record response size
            String responseJson = response.toString();
            metricsService.recordResponseSize(responseJson.getBytes().length);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            metricsService.recordApiCall(false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve users", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/{id}")
    @Timed(value = "user.get.time", description = "Time taken to get a user by ID")
    @Operation(
        summary = "Get user by ID",
        description = "Retrieve a specific user by their unique identifier"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user",
            content = @Content(schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    public ResponseEntity<?> getUserById(
        @Parameter(description = "User ID", example = "1")
        @PathVariable Long id,
        HttpServletRequest request
    ) {
        try {
            // Record request size
            if (request.getContentLength() > 0) {
                metricsService.recordRequestSize(request.getContentLength());
            }
            
            Optional<User> user = userService.getUserById(id);
            if (user.isPresent()) {
                // Record response size
                String responseJson = user.get().toString();
                metricsService.recordResponseSize(responseJson.getBytes().length);
                
                return ResponseEntity.ok(user.get());
            } else {
                metricsService.recordApiCall(false);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found", "id", id));
            }
        } catch (Exception e) {
            metricsService.recordApiCall(false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to retrieve user", "message", e.getMessage()));
        }
    }
    
    @PostMapping
    @Timed(value = "user.create.time", description = "Time taken to create a user")
    @Operation(
        summary = "Create a new user",
        description = "Create a new user with the provided information"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User created successfully",
            content = @Content(schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "409", description = "User already exists"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    public ResponseEntity<?> createUser(
        @Parameter(description = "User object to create", required = true)
        @Valid @RequestBody User user,
        HttpServletRequest request
    ) {
        try {
            // Record request size
            if (request.getContentLength() > 0) {
                metricsService.recordRequestSize(request.getContentLength());
            }
            
            User savedUser = userService.createUser(user);
            userCreatedCounter.increment();
            
            // Record response size
            String responseJson = savedUser.toString();
            metricsService.recordResponseSize(responseJson.getBytes().length);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            metricsService.recordApiCall(false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create user", "message", e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    @Timed(value = "user.update.time", description = "Time taken to update a user")
    @Operation(
        summary = "Update an existing user",
        description = "Update an existing user with the provided information"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User updated successfully",
            content = @Content(schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    public ResponseEntity<?> updateUser(
        @Parameter(description = "User ID", example = "1")
        @PathVariable Long id,
        @Parameter(description = "Updated user object", required = true)
        @Valid @RequestBody User userDetails,
        HttpServletRequest request
    ) {
        try {
            // Record request size
            if (request.getContentLength() > 0) {
                metricsService.recordRequestSize(request.getContentLength());
            }
            
            User updatedUser = userService.updateUser(id, userDetails);
            userUpdatedCounter.increment();
            
            // Record response size
            String responseJson = updatedUser.toString();
            metricsService.recordResponseSize(responseJson.getBytes().length);
            
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            metricsService.recordApiCall(false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update user", "message", e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    @Timed(value = "user.delete.time", description = "Time taken to delete a user")
    @Operation(
        summary = "Delete a user",
        description = "Delete a user by their unique identifier"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    public ResponseEntity<?> deleteUser(
        @Parameter(description = "User ID", example = "1")
        @PathVariable Long id,
        HttpServletRequest request
    ) {
        try {
            // Record request size
            if (request.getContentLength() > 0) {
                metricsService.recordRequestSize(request.getContentLength());
            }
            
            userService.deleteUser(id);
            userDeletedCounter.increment();
            
            Map<String, Object> response = Map.of("message", "User deleted successfully", "id", id);
            
            // Record response size
            String responseJson = response.toString();
            metricsService.recordResponseSize(responseJson.getBytes().length);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            metricsService.recordApiCall(false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete user", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/search")
    @Timed(value = "user.search.time", description = "Time taken to search users")
    @Operation(
        summary = "Search users",
        description = "Search users by username or email with pagination"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully searched users",
            content = @Content(schema = @Schema(implementation = Map.class))),
        @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    public ResponseEntity<?> searchUsers(
        @Parameter(description = "Search term for username or email", example = "john")
        @RequestParam String query,
        @Parameter(description = "Page number (0-based)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Number of items per page", example = "10")
        @RequestParam(defaultValue = "10") int size,
        HttpServletRequest request
    ) {
        try {
            // Record request size
            if (request.getContentLength() > 0) {
                metricsService.recordRequestSize(request.getContentLength());
            }
            
            PageRequest pageRequest = PageRequest.of(page, size);
            Page<User> pageResult = userService.searchUsers(query, pageRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("users", pageResult.getContent());
            response.put("currentPage", pageResult.getNumber());
            response.put("totalItems", pageResult.getTotalElements());
            response.put("totalPages", pageResult.getTotalPages());
            response.put("searchQuery", query);

            // Record response size
            String responseJson = response.toString();
            metricsService.recordResponseSize(responseJson.getBytes().length);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            metricsService.recordApiCall(false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to search users", "message", e.getMessage()));
        }
    }
    
    @GetMapping("/count")
    @Timed(value = "user.count.time", description = "Time taken to get user count")
    @Operation(
        summary = "Get user count",
        description = "Get total and active user counts (cached for performance)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user counts"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    public ResponseEntity<?> getUserCounts(HttpServletRequest request) {
        try {
            // Record request size
            if (request.getContentLength() > 0) {
                metricsService.recordRequestSize(request.getContentLength());
            }
            
            long totalUsers = userService.getUserCount();
            long activeUsers = userService.getActiveUserCount();
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalUsers", totalUsers);
            response.put("activeUsers", activeUsers);
            response.put("cached", true);
            
            // Record response size
            String responseJson = response.toString();
            metricsService.recordResponseSize(responseJson.getBytes().length);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            metricsService.recordApiCall(false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to get user counts", "message", e.getMessage()));
        }
    }
    
    @PostMapping("/cache/clear")
    @Timed(value = "user.cache.clear.time", description = "Time taken to clear user caches")
    @Operation(
        summary = "Clear user caches",
        description = "Clear all cached user data (admin only)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Caches cleared successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token required")
    })
    public ResponseEntity<?> clearUserCaches(HttpServletRequest request) {
        try {
            // Record request size
            if (request.getContentLength() > 0) {
                metricsService.recordRequestSize(request.getContentLength());
            }
            
            userService.clearAllCaches();
            
            Map<String, Object> response = Map.of("message", "User caches cleared successfully");
            
            // Record response size
            String responseJson = response.toString();
            metricsService.recordResponseSize(responseJson.getBytes().length);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            metricsService.recordApiCall(false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to clear caches", "message", e.getMessage()));
        }
    }
}