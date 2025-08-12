# Spring Boot User Management API

A comprehensive REST API for user management with JWT authentication, built with Spring Boot 3.2.4. This API provides secure endpoints for user operations and authentication, with built-in observability and monitoring capabilities.

## Features

- **RESTful API**: Complete CRUD operations for user management
- **JWT Authentication**: Secure endpoints with JWT token validation
- **Swagger Documentation**: Interactive API documentation at `/swagger-ui/index.html`
- **Prometheus Metrics**: Comprehensive application metrics and monitoring
- **H2 Database**: In-memory database for development and testing
- **Actuator Endpoints**: Health checks, metrics, and application information
- **Validation**: Input validation with proper error handling
- **Logging**: Structured logging with configurable levels

## Technology Stack

- **Spring Boot**: 3.2.4
- **Java**: 17+ (Jakarta EE 9+)
- **Database**: H2 (in-memory)
- **Security**: Spring Security with JWT
- **Documentation**: OpenAPI 3 (Swagger)
- **Monitoring**: Micrometer + Prometheus
- **Build Tool**: Maven

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Optional: Prometheus server for metrics collection

### Running the Application

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd springboot-user-api-complete
   ```

2. **Build the project**
   ```bash
   mvn clean install
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the application**
   - API Base URL: `http://localhost:8080`
   - Swagger UI: `http://localhost:8080/swagger-ui/index.html`
   - H2 Console: `http://localhost:8080/h2-console`
   - Actuator: `http://localhost:8080/actuator`

## API Endpoints

### Authentication
- `POST /api/v1/auth/login` - User login (returns JWT token)
- `POST /api/v1/auth/validate` - Validate JWT token
- `GET /api/v1/auth/health` - Authentication service health check

### User Management
- `GET /api/v1/users` - List all users (paginated)
- `GET /api/v1/users/{id}` - Get user by ID
- `POST /api/v1/users` - Create new user
- `PUT /api/v1/users/{id}` - Update existing user
- `DELETE /api/v1/users/{id}` - Delete user
- `GET /api/v1/users/search` - Search users by username or email

## Authentication

The API uses JWT (JSON Web Tokens) for authentication:

1. **Login**: Call `/api/v1/auth/login?username=<username>` to get a JWT token
2. **Use Token**: Include the token in the `Authorization` header: `Bearer <token>`
3. **Token Validation**: Use `/api/v1/auth/validate` to verify token validity

### Example Usage

```bash
# Login to get token
curl -X POST "http://localhost:8080/api/v1/auth/login?username=john_doe"

# Use token to access protected endpoints
curl -H "Authorization: Bearer <your-jwt-token>" \
     "http://localhost:8080/api/v1/users"
```

## Prometheus Integration

The application is configured to work with Prometheus for metrics collection and monitoring.

### Configuration

The application properties include comprehensive Prometheus configuration:

```properties
# Prometheus Configuration
management.metrics.export.prometheus.enabled=true
management.metrics.export.prometheus.descriptions=true
management.metrics.export.prometheus.step=1m
management.metrics.export.prometheus.pushgateway.enabled=false

# Prometheus Server Configuration (configurable)
prometheus.server.url=http://localhost:9090
prometheus.server.push.enabled=false
prometheus.server.push.interval=15s
```

### Available Metrics

The application exposes the following metrics:

#### Application Metrics
- `user_created_total` - Total number of users created
- `user_updated_total` - Total number of users updated
- `user_deleted_total` - Total number of users deleted

#### Authentication Metrics
- `auth_login_attempts_total` - Total login attempts
- `auth_login_success_total` - Successful logins
- `auth_login_failure_total` - Failed login attempts

#### API Performance Metrics
- `user.list.time` - Time taken to list users
- `user.get.time` - Time taken to get a user by ID
- `user.create.time` - Time taken to create a user
- `user.update.time` - Time taken to update a user
- `user.delete.time` - Time taken to delete a user
- `user.search.time` - Time taken to search users
- `auth.login.time` - Time taken to process login
- `auth.validate.time` - Time taken to validate token

#### Custom Business Metrics
- `api_calls_total` - Total API calls
- `api_calls_successful_total` - Successful API calls
- `api_calls_failed_total` - Failed API calls
- `active_users` - Number of active users
- `total_users` - Total number of users
- `api_response_time` - API response time distribution
- `database_query_time` - Database query execution time
- `request_size_bytes` - Request size distribution
- `response_size_bytes` - Response size distribution

### Setting Up Prometheus

1. **Download Prometheus** from [prometheus.io](https://prometheus.io/download/)

2. **Configure Prometheus** (`prometheus.yml`):
   ```yaml
   global:
     scrape_interval: 15s
   
   scrape_configs:
     - job_name: 'spring-boot-app'
       static_configs:
         - targets: ['localhost:8080']
       metrics_path: '/actuator/prometheus'
       scrape_interval: 5s
   ```

3. **Start Prometheus**:
   ```bash
   ./prometheus --config.file=prometheus.yml
   ```

4. **Access Prometheus UI**: `http://localhost:9090`

### Metrics Endpoints

- **Prometheus Metrics**: `/actuator/prometheus`
- **All Metrics**: `/actuator/metrics`
- **Health Check**: `/actuator/health`
- **Application Info**: `/actuator/info`

## Swagger Documentation

The API is fully documented using OpenAPI 3 (Swagger):

- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`
- **OpenAPI YAML**: `http://localhost:8080/v3/api-docs.yaml`

### Features
- Interactive API testing
- Request/response examples
- Authentication requirements
- Parameter descriptions
- Response schemas

## Database Configuration

The application uses H2 in-memory database for development:

```properties
spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true
```

**Note**: H2 is configured for demo purposes only. Do not use in production.

## Caching

The application uses Spring Cache with Caffeine for high-performance in-memory caching:

### Cache Configuration
```properties
# Cache Configuration
spring.cache.type=caffeine
spring.cache.cache-names=users
spring.cache.caffeine.spec=maximumSize=10,expireAfterWrite=600s
```

### How Caching Works

#### User Caching (`users` cache)
- **Cache Keys**: Only static content like user counts
- **Cache Eviction**: Automatically expires after 10 minutes
- **Performance**: Reduces database queries for infrequently changing data

#### Why Limited Caching?
- **JWT Tokens**: Have built-in TTL (Time To Live), so caching validation defeats their purpose
- **User Data**: Most user operations are dynamic and should be real-time
- **Authentication**: Should always be real-time for security reasons
- **Static Content**: Only user counts are cached as they change infrequently

#### Cache Annotations Used
- `@Cacheable`: Only used for static content like user counts
- **No caching** for dynamic operations like CRUD, search, or authentication

```

## Configuration Properties

### Core Configuration
```properties
# Server
server.port=8080
server.servlet.context-path=/

# JWT
jwt.secret=myjwtsecretkey12345678901234567890
jwt.expiration=86400000

# Logging
logging.level.com.example.demo=DEBUG
logging.level.org.springframework.security=DEBUG
```

### Performance Configuration
```properties
# Connection Pool
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5

# Cache
spring.cache.type=caffeine
spring.cache.cache-names=users,auth

# Async
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=10
```

## Development

### Building
```bash
mvn clean compile
```

### Testing
```bash
mvn test
```

### Running Tests with Coverage
```bash
mvn jacoco:report
```

### Checking Dependencies
```bash
# Check for javax.* dependencies (should be empty)
mvn dependency:tree | grep javax

# Check for Jakarta dependencies
mvn dependency:tree | grep jakarta
```

## Testing

The application includes comprehensive unit and integration tests:

### Unit Tests
- **UserService**: Tests CRUD operations, caching, and business logic
- **AuthService**: Tests authentication, token validation, and session management
- **MetricsService**: Tests metrics collection, counters, timers, and gauges

### Integration Tests
- **CachingIntegrationTest**: Tests real caching functionality with database
- **Cache eviction**: Tests cache clearing on user updates/deletes
- **Performance**: Tests cache performance improvements

### Running Tests
```bash
# Run all tests
mvn test

# Run only unit tests
mvn test -Dtest="*Test" -DexcludedGroups="integration"

# Run only integration tests
mvn test -Dtest="*IntegrationTest"

# Run with coverage
mvn jacoco:report
```

## Monitoring and Observability

### Health Checks
- Application health: `/actuator/health`
- Database connectivity
- JWT service status

### Metrics Dashboard
Consider using Grafana with Prometheus for visualization:
- Import Prometheus as data source
- Create dashboards for application metrics
- Set up alerts for critical metrics

### Logging
- Structured logging with configurable levels
- Request/response logging
- Security event logging
- Performance metrics logging

## Security Considerations

- JWT tokens expire after 1 hour
- All endpoints (except auth and Swagger) require authentication
- Input validation on all user inputs
- SQL injection protection via JPA
- CORS configuration for web clients

## Production Deployment

### Environment Variables
```bash
export SPRING_PROFILES_ACTIVE=production
export JWT_SECRET=your-secure-secret-key
export DATABASE_URL=your-production-database-url
export PROMETHEUS_SERVER_URL=your-prometheus-server-url
```

### Database
- Replace H2 with PostgreSQL, MySQL, or Oracle
- Configure connection pooling
- Set up database monitoring

### Monitoring
- Configure Prometheus for production
- Set up Grafana dashboards
- Configure alerting rules
- Monitor application performance

## Troubleshooting

### Common Issues

1. **JWT Token Expired**: Re-authenticate to get a new token
2. **Database Connection**: Check H2 console at `/h2-console`
3. **Metrics Not Showing**: Verify Prometheus configuration
4. **Swagger Not Loading**: Check if `/swagger-ui/index.html` is accessible

### Logs
Check application logs for detailed error information:
```bash
tail -f logs/application.log
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License.

## Support

For questions and support:
- Create an issue in the repository
- Check the Swagger documentation
- Review the application logs
- Monitor the health endpoints
