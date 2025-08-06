This is a Java springboot project that will
1. create a springboot web app with a rest endpoint
2. secure that endpoint, require jwt token to access ( http://localhost:8080/api/v1/users, /api/v1/auth/login?username=xxx )
3. create public SwaggerUI for that endpoint ( http://localhost:8080/swagger-ui/index.html )
4. contains libraries for observability if needed
5. has configuration using h2 db for demo purpose, do not use for production.
6. Comes from Spring Boot 3.2.4, meaning no more javax.\*, use jakarta.\* only, including JUnit and all other library dependencies. Run the following to check:
   ```
   mvn dependency:tree | grep javax
   ```

The motivation is to create a simple, minimal project that can be compiled to start with. Even with GenAI or Vibe-coding, the start is always buggy and time-consuming. This project should get you going faster.
