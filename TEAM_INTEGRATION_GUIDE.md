# Singko de Bangko - Microservices Integration Guide

This guide is for team members developing **Auth Service**, **Account Service**, and **Transaction Service** to seamlessly connect with **Eureka Server** and **Spring Cloud API Gateway**.

---

## Architecture Overview

```
[ Client / Postman ] 
        │
        ▼ (Port 8080)
┌─────────────────────────────────────────────────────────┐
│                   SPRING CLOUD GATEWAY                  │
│  - Routes requests                                      │
│  - Validates JWT tokens on secured routes               │
│  - Injects 'X-User-Id' & 'X-User-Email' downstream      │
└────────┬───────────────────┬───────────────────┬────────┘
         │                   │                   │
         │ /login            │ /accounts/**      │ /transactions/**
         ▼ (Port 8081)       ▼ (Port 8082)       ▼ (Port 8083)
  ┌──────────────┐    ┌──────────────┐    ┌─────────────────────┐
  │ AUTH-SERVICE │    │ACCOUNT-SERVICE│   │ TRANSACTION-SERVICE │
  └──────┬───────┘    └──────┬───────┘    └──────────┬──────────┘
         │                   │                       │
         └───────────────────┴───────────────────────┘
                             │ Heartbeat & Register
                             ▼ (Port 8761)
                   ┌───────────────────┐
                   │   EUREKA SERVER   │
                   └───────────────────┘
```

---

## 1. What Each Service Needs in `pom.xml`

Add the Eureka Client dependency to your service:

```xml
<dependencies>
    <!-- Eureka Client -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
    </dependency>
</dependencies>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2023.0.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## 2. Configuration for Each Service

In your service's `application.properties` or `application.yml`:

### Auth Service (`application.properties`)
```properties
server.port=8081
spring.application.name=AUTH-SERVICE
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

### Account Service (`application.properties`)
```properties
server.port=8082
spring.application.name=ACCOUNT-SERVICE
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

### Transaction Service (`application.properties`)
```properties
server.port=8083
spring.application.name=TRANSACTION-SERVICE
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

> [!IMPORTANT]
> The `spring.application.name` must match the Gateway routing table:
> - `AUTH-SERVICE`
> - `ACCOUNT-SERVICE`
> - `TRANSACTION-SERVICE`

---

## 3. Gateway Routing & Security Rules

All incoming client requests should enter via Gateway at `http://localhost:8080`.

| Path Pattern | Target Service | Access Level | Description |
| :--- | :--- | :--- | :--- |
| `/login` | `AUTH-SERVICE` | Public | Login endpoint issuing JWT |
| `/accounts/**` | `ACCOUNT-SERVICE` | Secured | Account balance & profile queries |
| `/transactions/**` | `TRANSACTION-SERVICE` | Secured | Fund transfers & txn history |

### How Gateway Handles Authentication

1. **Public Route (`/login`)**:
   - The Gateway forwards requests directly without checking for tokens.
2. **Secured Routes (`/accounts/**`, `/transactions/**`)**:
   - Clients must send `Authorization: Bearer <JWT_TOKEN>`.
   - The Gateway cryptographically verifies the token.
   - If valid, the Gateway injects the authenticated identity into downstream request headers:
     - `X-User-Id`: e.g. `1001`
     - `X-User-Email`: e.g. `juan.delacruz@neobank.com`
   - Downstream controllers can easily access the caller using Spring's `@RequestHeader`:
     ```java
     @GetMapping("/balance")
     public ResponseEntity<?> getBalance(@RequestHeader("X-User-Id") String userId) {
         // Authenticated user ID is directly available!
     }
     ```

---

## 4. How to Test Your Service with Eureka & Gateway

1. Start **Eureka Server**:
   ```bash
   cd eureka-server
   ./mvnw spring-boot:run
   ```
   Open `http://localhost:8761` in your browser to see registered services.

2. Start your **Downstream Service** (e.g. `auth-service`):
   ```bash
   cd auth-service
   ./mvnw spring-boot:run
   ```
   Refresh `http://localhost:8761` to confirm your service appears.

3. Start **API Gateway**:
   ```bash
   cd api-gateway
   ./mvnw spring-boot:run
   ```

4. Call your service through the Gateway:
   ```bash
   curl http://localhost:8080/login
   ```
