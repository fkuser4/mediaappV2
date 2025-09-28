# MediaApp

A full-stack Java application with **Spring Boot backend** and **JavaFX** frontend for social media content management.

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

## Screenshots

### Login 
<img width="712" height="433" alt="image" src="https://github.com/user-attachments/assets/113cf988-2120-4d5b-a3dc-f7041cb1a0ef" />

### Dashboard
<img width="714" height="434" alt="image" src="https://github.com/user-attachments/assets/8d207e4a-8ead-4340-8ecd-52ed6216a0b1" />

### Post preview
<img width="710" height="432" alt="image" src="https://github.com/user-attachments/assets/86f4191e-9513-4151-95ba-d76aa7e38223" />

### Posts view
<img width="706" height="430" alt="image" src="https://github.com/user-attachments/assets/2599e530-2fee-48fd-b336-f1868cdf8432" />

### Settings
<img width="712" height="433" alt="image" src="https://github.com/user-attachments/assets/22c0471a-5cbf-491c-ac46-205cb38d641a" />
