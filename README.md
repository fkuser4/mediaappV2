# MediaApp

A full-stack Java application with Spring Boot backend and JavaFX frontend for media management.

## Project Structure
- `backend/` - Spring Boot REST API server
- `frontend/` - JavaFX desktop application
- `shared/` - Common DTOs and models

## Technology Stack
- **Backend**: Spring Boot 3.2.8, Java 24
- **Frontend**: JavaFX 21, Java 24
- **Database**: PostgreSQL 15
- **Build**: Maven multi-module

## Prerequisites
- Java 24
- Maven 3.x
- Docker & Docker Compose (for database)

## Quick Start

1. **Start database**:
   ```bash
   docker-compose up -d postgres

2. Build project:
   mvn clean compile
3. Run backend:
   cd backend
   mvn spring-boot:run
4. Run frontend:
   cd frontend
   mvn javafx:run

Database Access

- PostgreSQL: localhost:5432
- PgAdmin: http://localhost:5050 (admin@mediaapp.com / admin123)

Configuration

Copy .env.example to .env and configure your environment variables.