# AGENTS.md

This file provides guidance to Qoder (qoder.com) when working with code in this repository.

## Project Overview

CodeMate is a Spring Boot-based developer community and AI Agent platform. It is a multi-module Maven project with layered architecture separating API definitions, business logic, core utilities, frontend resources, and web endpoints.

## Build and Test Commands

### Build
```bash
# Clean and install all modules
mvn clean install -DskipTests=true

# Build for specific environment (dev/test/pre/prod)
mvn clean install -DskipTests=true -P<env>

# Build web module only
cd codemate-web && mvn clean package spring-boot:repackage -DskipTests=true -Pprod
```

### Test
```bash
# Run all tests
mvn test

# Run tests in specific module
cd codemate-web && mvn test

# Run single test class
mvn test -Dtest=ClassName

# Run single test method
mvn test -Dtest=ClassName#methodName
```

### Run Application
```bash
# Local development (requires MySQL and Redis running)
# Entry point: QuickForumApplication in codemate-web module
# Default port: 8080
# Configure database in: codemate-web/src/main/resources-env/dev/application-dal.yml

# Production deployment
./launch.sh start        # Build and deploy
./launch.sh restart      # Restart existing deployment
```

## Architecture

### Module Dependencies
```
codemate-web
├── depends on: codemate-ui, codemate-service
│
codemate-service
├── depends on: codemate-core, codemate-api
│
codemate-core
├── depends on: codemate-api
│
codemate-api
└── (base module: entities, DTOs, VOs, enums)
```

### Key Modules
- **codemate-api**: Entity definitions, DTOs, VOs, common enums
- **codemate-core**: Utilities, search, cache, recommendations, common components
- **codemate-service**: Business logic, MyBatis-Plus database operations
- **codemate-ui**: Thymeleaf templates, JavaScript, CSS, static resources
- **codemate-web**: Controllers, REST endpoints, `QuickForumApplication` entry point, global exception handling, authentication

### Configuration Structure
- Environment configs in `codemate-web/src/main/resources-env/<env>/`
  - `application-dal.yml`: Database config
  - `application-image.yml`: Image upload config
  - `application-web.yml`: Web config
- Main configs in `codemate-web/src/main/resources/`
  - `application.yml`: Main entry
  - `application-config.yml`: Site configuration
  - `logback-spring.xml`: Logging

### Technology Stack
- Spring Boot 2.7.1, Java 8+
- MyBatis-Plus for ORM
- Thymeleaf for SSR
- Redis for caching/sessions
- ElasticSearch for search
- RabbitMQ for messaging
- MongoDB for NoSQL
- Liquibase for schema migrations (in `codemate-web/src/main/resources/liquibase`)

## Development Guidelines

### Database Changes
- Add Liquibase changesets to `codemate-web/src/main/resources/liquibase`
- Database auto-creates on first startup (default: `pai_coding`)
- Use MyBatis-Plus for database operations
- Entity classes in codemate-api module

### Adding New Features
- Follow layered architecture: API → Service → Core
- Check existing code patterns in neighboring files
- Use libraries already present in the project (check root `pom.xml`)
- Frontend changes go in codemate-ui module

### API Documentation
- Swagger UI available at `/doc.html`
- Uses Knife4j (knife4j-openapi2-spring-boot-starter)

### Testing
- Supports JUnit and Spock (Groovy-based BDD)
- Test files in `codemate-web/src/test/java` and `codemate-web/src/test/groovy`
