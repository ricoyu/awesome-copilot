# User Management REST API

A simple User management REST API built with Spring Boot and the awesome-copilot framework.

## Features

- CRUD operations for User management
- RESTful API design
- JPA/Hibernate for database operations
- Unified JSON response format using framework's `Result` class
- Proper exception handling with framework's exception handlers
- Input validation
- Transaction management

## Technology Stack

- **Spring Boot** - Application framework
- **Spring Data JPA** - Data access layer
- **Hibernate** - ORM framework
- **MySQL** - Database
- **awesome-copilot framework** - Custom framework components
  - `copilot-orm` - ORM utilities and BaseEntity
  - `copilot-web` - Web utilities and exception handling
  - `commons-lang` - Common utilities and Result response format
  - `copilot-validation` - Validation utilities

## Project Structure

```
user-api/
├── src/main/java/com/awesomecopilot/user/
│   ├── controller/
│   │   └── UserController.java          # REST API endpoints
│   ├── service/
│   │   └── UserService.java             # Business logic
│   ├── repository/
│   │   └── UserRepository.java          # JPA repository
│   ├── entity/
│   │   └── User.java                    # User entity (extends BaseEntity)
│   ├── dto/
│   │   └── UserRequest.java             # Request DTO
│   └── UserApiApplication.java          # Application entry point
├── src/main/resources/
│   ├── application.yml                  # Application configuration
│   └── schema.sql                       # Database schema
└── pom.xml                              # Maven dependencies
```

## Entity Model

### User Entity
Extends `BaseEntity` from copilot-orm framework which provides:
- `id` (Long) - Auto-generated primary key
- `createTime` (LocalDateTime) - Auto-managed creation timestamp
- `updateTime` (LocalDateTime) - Auto-managed update timestamp

Additional fields:
- `username` (String, 3-50 chars, unique, required)
- `email` (String, max 100 chars, unique, required, valid email format)
- `phone` (String, max 20 chars, optional)

## API Endpoints

### Base URL
```
http://localhost:8080/api/users
```

### 1. Create User
**POST** `/api/users`

**Request Body:**
```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "phone": "1234567890"
}
```

**Success Response (200 OK):**
```json
{
  "code": "0",
  "status": "success",
  "message": "User created successfully",
  "data": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "phone": "1234567890",
    "createTime": "2026-04-18T10:30:00",
    "updateTime": "2026-04-18T10:30:00"
  }
}
```

**Error Response (Duplicate Username):**
```json
{
  "code": "4001",
  "status": "fail",
  "message": "Username already exists: john_doe"
}
```

### 2. Get User by ID
**GET** `/api/users/{id}`

**Success Response (200 OK):**
```json
{
  "code": "0",
  "status": "success",
  "message": null,
  "data": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "phone": "1234567890",
    "createTime": "2026-04-18T10:30:00",
    "updateTime": "2026-04-18T10:30:00"
  }
}
```

**Error Response (Not Found):**
```json
{
  "code": "4004",
  "status": "fail",
  "message": "User not found with id: 999"
}
```

### 3. Get All Users
**GET** `/api/users`

**Success Response (200 OK):**
```json
{
  "code": "0",
  "status": "success",
  "message": null,
  "data": [
    {
      "id": 1,
      "username": "john_doe",
      "email": "john@example.com",
      "phone": "1234567890",
      "createTime": "2026-04-18T10:30:00",
      "updateTime": "2026-04-18T10:30:00"
    },
    {
      "id": 2,
      "username": "jane_smith",
      "email": "jane@example.com",
      "phone": "0987654321",
      "createTime": "2026-04-18T11:00:00",
      "updateTime": "2026-04-18T11:00:00"
    }
  ]
}
```

### 4. Update User
**PUT** `/api/users/{id}`

**Request Body:**
```json
{
  "username": "john_updated",
  "email": "john.updated@example.com",
  "phone": "1111111111"
}
```

**Success Response (200 OK):**
```json
{
  "code": "0",
  "status": "success",
  "message": "User updated successfully",
  "data": {
    "id": 1,
    "username": "john_updated",
    "email": "john.updated@example.com",
    "phone": "1111111111",
    "createTime": "2026-04-18T10:30:00",
    "updateTime": "2026-04-18T12:00:00"
  }
}
```

### 5. Delete User
**DELETE** `/api/users/{id}`

**Success Response (200 OK):**
```json
{
  "code": "0",
  "status": "success",
  "message": "User deleted successfully",
  "data": null
}
```

## Response Format

All API responses follow the unified `Result` format from the awesome-copilot framework:

```json
{
  "code": "0",           // "0" = success, other codes = error
  "status": "success",   // "success" or "fail"
  "message": "...",      // Optional message or error details
  "data": {...}          // Response data (can be object, array, or null)
}
```

## Error Codes

| Code | Description |
|------|-------------|
| 0    | Success |
| 4001 | Username already exists |
| 4002 | Email already exists |
| 4004 | User not found |
| 4000 | Validation error |
| 5000 | Internal server error |

## Validation Rules

### Username
- Required
- Length: 3-50 characters
- Must be unique

### Email
- Required
- Must be valid email format
- Max length: 100 characters
- Must be unique

### Phone
- Optional
- Max length: 20 characters

## Database Configuration

Update `application.yml` with your database credentials:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/user_db
    username: root
    password: your_password
```

Run the provided `schema.sql` to create the database and table.

## Dependencies

Key dependencies in `pom.xml`:

```xml
<!-- Awesome Copilot Framework -->
<dependency>
    <groupId>com.awesomecopilot</groupId>
    <artifactId>commons-lang</artifactId>
</dependency>
<dependency>
    <groupId>com.awesomecopilot</groupId>
    <artifactId>copilot-orm</artifactId>
</dependency>
<dependency>
    <groupId>com.awesomecopilot</groupId>
    <artifactId>copilot-web</artifactId>
</dependency>
<dependency>
    <groupId>com.awesomecopilot</groupId>
    <artifactId>copilot-validation</artifactId>
</dependency>

<!-- Spring Boot -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Database -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>
```

## Running the Application

1. **Setup Database:**
   ```bash
   mysql -u root -p < src/main/resources/schema.sql
   ```

2. **Update Configuration:**
   Edit `src/main/resources/application.yml` with your database credentials

3. **Build the Project:**
   ```bash
   mvn clean install
   ```

4. **Run the Application:**
   ```bash
   mvn spring-boot:run
   ```

   Or run the main class:
   ```bash
   java -jar target/user-api-17.0.0.jar
   ```

5. **Access the API:**
   ```
   http://localhost:8080/api/users
   ```

## Testing with cURL

### Create User
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "email": "john@example.com",
    "phone": "1234567890"
  }'
```

### Get User by ID
```bash
curl http://localhost:8080/api/users/1
```

### Get All Users
```bash
curl http://localhost:8080/api/users
```

### Update User
```bash
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_updated",
    "email": "john.updated@example.com",
    "phone": "1111111111"
  }'
```

### Delete User
```bash
curl -X DELETE http://localhost:8080/api/users/1
```

## Framework Components Used

### 1. BaseEntity (copilot-orm)
Provides common entity fields and automatic timestamp management:
- Auto-generated ID
- Automatic createTime on insert
- Automatic updateTime on update

### 2. Result & Results (commons-lang)
Unified response format builder:
- Consistent API responses
- Type-safe generic support
- Builder pattern for easy construction

### 3. RestExceptionAdvice (copilot-web)
Global exception handling:
- Catches BusinessException, EntityNotFoundException
- Handles validation errors
- Returns consistent error responses

### 4. BusinessException & EntityNotFoundException (commons-lang)
Framework exception types:
- BusinessException for business logic errors
- EntityNotFoundException for resource not found errors
- Automatically handled by RestExceptionAdvice

## License

Part of the awesome-copilot framework project.
