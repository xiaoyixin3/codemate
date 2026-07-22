# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CodeMate is a developer community and AI Agent system built with Spring Boot, MyBatis-Plus, MySQL, Redis, ElasticSearch, MongoDB, Docker, and RabbitMQ. It supports technical content sharing, retrieval-augmented generation, task planning, and observable Agent runs.

## Architecture

### Module Structure
- **codemate-api**: Common enums, entities, and DTOs/VOs
- **codemate-core**: Core utilities and components (search, cache, recommendations)
- **codemate-service**: Business logic and database operations
- **codemate-ui**: Frontend resources (HTML, JavaScript, CSS, Thymeleaf)
- **codemate-web**: Web layer, HTTP endpoints, application entry point

### Key Technologies
- Spring Boot 2.7.1 with Java 8+
- MyBatis-Plus for database operations
- Thymeleaf for server-side rendering
- Redis for caching and session management
- ElasticSearch for content search
- RabbitMQ for message queuing
- MongoDB for NoSQL data
- Liquibase for database schema management

## Common Development Commands

### Build and Package
```bash
# Clean and install all modules
mvn clean install -DskipTests=true

# Package for specific environment
mvn clean install -DskipTests=true -Pprod

# Build web module specifically
cd codemate-web
mvn clean package spring-boot:repackage -DskipTests=true -Pprod
```

### Testing
```bash
# Run all tests
mvn test

# Skip tests during build
mvn clean install -DskipTests=true
```

### Environment Profiles
- **dev**: Local development (default)
- **test**: Testing environment  
- **pre**: Pre-production
- **prod**: Production

Switch environment using:
```bash
mvn clean package -DskipTests=true -P<environment>
```

### Running the Application

#### Local Development
1. Start required services: MySQL, Redis
2. Configure database connection in `codemate-web/src/main/resources-env/dev/application-dal.yml`
3. Run main class: `QuickForumApplication` (codemate-web module)
4. Access: http://127.0.0.1:8080

#### Production Deployment
```bash
# Use the provided launch script
./launch.sh start

# Or restart existing deployment
./launch.sh restart
```

### Database Setup
- Database auto-creation on first startup
- Schema managed via Liquibase migrations in `codemate-web/src/main/resources/liquibase`
- Default database name: `pai_coding` (configurable via `database.name` property)

## Configuration

### Environment-Specific Configs
Located in `codemate-web/src/main/resources-env/<env>/`:
- `application-dal.yml`: Database configuration
- `application-image.yml`: Image upload settings
- `application-web.yml`: Web-related configuration

### Main Configuration Files
- `application.yml`: Main configuration entry point
- `application-config.yml`: Global site configuration
- `logback-spring.xml`: Logging configuration

## Important Notes

- **Entry Point**: `QuickForumApplication.java` in codemate-web module
- **Default Port**: 8080 (automatically finds available port in dev mode)
- **Auto-Configuration**: Database tables and initial data created automatically
- **Dependencies**: All managed in root `pom.xml` via `dependencyManagement`
- **Testing Framework**: Supports JUnit and Spock (Groovy-based BDD)

## Development Guidelines

### Module Dependencies
- codemate-web depends on codemate-ui and codemate-service
- codemate-service depends on codemate-core and codemate-api
- Follow the layered architecture when adding new features

### Database Operations
- Use MyBatis-Plus for database operations
- Database schema changes should be added as Liquibase changesets
- Entity classes located in codemate-api module

### Frontend Development
- Thymeleaf templates and static resources in codemate-ui module
- Supports Swagger UI at `/doc.html` for API documentation
