# CORC Backend

Spring Boot 3.x backend for the CORC Fashion E-Commerce platform.

## Table of Contents
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Environment Variables](#environment-variables)
  - [Installation](#installation)
  - [Running Locally](#running-locally)
- [Building for Production](#building-for-production)
- [Running in Production](#running-in-production)
- [API Documentation](#api-documentation)
- [Database](#database)
- [Testing](#testing)
- [Docker (Optional)](#docker-optional)
- [Troubleshooting](#troubleshooting)
- [License](#license)

## Features
- RESTful JSON API
- JWT-based authentication & authorization
- Role-based access control (ADMIN, USER)
- Product catalog with filtering, pagination, search
- Shopping cart & wishlist
- Order management (creation, tracking, history)
- Payment integration (Stripe/Razorpay ready)
- Email notifications (welcome, password reset, order confirmation)
- File upload (local or AWS S3)
- Admin dashboard endpoints
- Caching (Caffeine)
- Spring Boot Actuator for monitoring
- Input validation (Bean Validation 3.0)
- Global exception handling
- Async email sending
- Data seeding for development (dev/test profiles only)

## Tech Stack
- **Java 21** (or latest LTS)
- **Spring Boot 3.2.5**
- **Spring Data JPA** with Hibernate
- **PostgreSQL** (or any compatible database)
- **Spring Security** with JWT
- **Spring Boot Starter Mail**
- **Spring Boot Starter Cache** (Caffeine)
- **Spring Boot Starter Actuator**
- **AWS S3 SDK** (optional)
- **Lombok** (to reduce boilerplate)
- **Gradle** as build tool
- **Thymeleaf** (for email templates)

## Getting Started

### Prerequisites
- **Java JDK 21+**
- **PostgreSQL** (or configure another datasource)
- **Gradle 8.x** (the wrapper `gradlew` is included)
- (Optional) **AWS S3** credentials if using cloud storage
- (Optional) **SMTP** server for email (Gmail, SendGrid, etc.)

### Environment Variables
Create a `.env` file in the `backend/` directory (or set system environment variables).  
Copy `.env.example` to `.env` and fill in the values.

```env
# Database
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# Mail (SMTP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@example.com
MAIL_PASSWORD=your_app_password_or_email_password

# JWT
JWT_SECRET=your_long_random_secret_here_make_it_at_least_32_characters

# CORS (comma-separated, no spaces)
CORS_ORIGINS=http://localhost:5173,http://localhost:3000

# Frontend URL (for redirects in emails)
FRONTEND_URL=http://localhost:5173

# AWS S3 (optional)
S3_BUCKET=your_s3_bucket_name
S3_REGION=your_s3_region
S3_ACCESS_KEY=your_s3_access_key
S3_SECRET_KEY=your_s3_secret_key
```

> **Never commit your `.env` file.** The `.gitignore` already excludes it.  
> Keep `.env.example` as a template for contributors.

### Installation
1. Clone the repository (if you haven't already):
   ```bash
   git clone <repository-url>
   cd CORC/backend
   ```
2. Ensure the Gradle wrapper is executable:
   ```bash
   chmod +x gradlew
   ```
3. (Optional) Install dependencies:
   ```bash
   ./gradlew dependencies
   ```

### Running Locally (Development)
```bash
./gradlew bootRun
```
The API will be available at **http://localhost:8080/api**.

>**  
*(Adjust port if changed in `application.yml`)*

### Running with a Specific Profile
To run with the `dev` profile (enables data seeding):
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```
To run with `test` profile (useful for integration tests):
```bash
./gradlew bootRun --args='--spring.profiles.active=test'
```
**Note:** The `prod` profile disables data seeding for security.

## Building for Production
```bash
./gradlew clean build
```
The executable JAR will be generated at:
```
build/libs/corc-backend-<version>.jar
```

## Running in Production
```bash
java -jar build/libs/corc-backend-*.jar
```
**Important:** Set the `spring.profiles.active=prod` environment variable or system property to activate production settings (disables seeding, enables validation, adjusts logging, etc.).

Example:
```bash
java -jar -Dspring.profiles.active=prod build/libs/corc-backend-*.jar
```
Or set via environment variable:
```bash
SPRING_PROFILES_ACTIVE=prod java -jar build/libs/corc-backend-*.jar
```

### Production Configuration Highlights (`application-prod.yml`)
- `spring.jpa.show-sql: false`
- `spring.jpa.hibernate.ddl-auto: validate` (use Flyway/Liquibase in real prod)
- Reduced log levels (`root: WARN`)
- Actuator endpoints exposed (`/actuator/health`, `/actuator/info`, `/actuator/metrics`)
- Ensure `JWT_SECRET`, `CORS_ORIGINS`, `FRONTEND_URL` are set via environment variables.

## API Documentation
API endpoints are under `/api/v1/`.  
You can view interactive API documentation (if enabled) at:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html` (if Springdoc/OpenAPI added)
- Otherwise, refer to the controller classes under `src/main/java/com/corc/backend/controller/`.

### Key Controllers
- `AuthController`: `/api/v1/auth/**` (register, login, refresh, password reset)
- `UserController`: `/api/v1/users/me` (profile)
- `ProductController`: `/api/v1/products/**` (catalog)
- `CartController`: `/api/v1/cart/**`
- `OrderController`: `/api/v1/orders/**`
- `AdminController`: `/api/v1/admin/**` (ADMIN only)
- `MailService`: Async email sending (used internally)
- `MediaUploadController`: `/api/v1/media/upload/**`

## Database
The application uses **PostgreSQL** by default.  
Configure the datasource in `application.yml` or via environment variables:
- `spring.datasource.url=jdbc:postgresql://localhost:5432/your_db`
- `spring.datasource.username=${DB_USERNAME}`
- `spring.datasource.password=${DB_PASSWORD}`

Schema is managed by Hibernate (`ddl-auto: update` in dev, `validate` in prod).  
For production, consider using **Flyway** or **Liquibase** for version-controlled migrations.

## Testing
### Unit Tests
```bash
./gradlew test
```
### Integration Tests (requires a test database)
Set `spring.profiles.active=test` and ensure a test database is configured (e.g., an in-memory H2 or a separate PostgreSQL schema).

## Docker (Optional)
### Build Image
```bash
docker build -t corc-backend:latest .
```
### Run Container
```bash
docker run -p 8080:8080 \
  --env-file .env \
  corc-backend:latest
```
*(Ensure `.env` contains all required variables.)*

## Troubleshooting
- **Mail authentication failure**: Verify `MAIL_USERNAME` and `MAIL_PASSWORD` (use an App Password for Gmail with 2FA).
- **Port already in use**: Another process is on 8080. Kill it or change `server.port` in `application.yml`.
- **Database connection failed**: Check `DB_USERNAME`, `DB_PASSWORD`, and that PostgreSQL is running and accessible.
- **JWT authentication issues**: Ensure `JWT_SECRET` is set and is a strong secret (min 32 chars).  
- **CORS errors**: Confirm `CORS_ORIGINS` includes your frontend URL (no trailing spaces).

## License
This project is licensed under the MIT License - see the root `LICENSE` file for details.

---  
*For frontend setup, see `../frontend/README.md`.*