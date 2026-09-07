# API & Service Contracts: Project 'Singko de Bangko'
**Sprint Duration:** 5-Hour High-Intensity Microservices Sprint  
**Target Stack:** Java 17 | Spring Boot | Spring Cloud Gateway | Eureka | Spring Security (JWT) | Docker Compose

---

## 1. System Architecture & Gateway Routing

All client communication enters through the **Spring Cloud API Gateway** (`http://localhost:8080`). Direct client access to downstream microservices is restricted. Services register dynamically with **Eureka Server** (`http://localhost:8761`).

| Service Name | Eureka Service ID | Default Internal Port | Gateway Route Path | Access Level |
| :--- | :--- | :--- | :--- | :--- |
| **Eureka Server** | `EUREKA-SERVER` | `8761` | N/A (Direct Admin) | Internal / Dev |
| **API Gateway** | `API-GATEWAY` | `8080` | `/**` | Public Entrypoint |
| **Auth Service** | `AUTH-SERVICE` | `8081` | `/login` | Public |
| **Account Service** | `ACCOUNT-SERVICE` | `8082` | `/accounts/**` | Secured (Bearer JWT) |
| **Transaction Service** | `TRANSACTION-SERVICE` | `8083` | `/transactions/**` | Secured (Bearer JWT) |

### Gateway Security & Header Propagation
- The API Gateway executes a global or route-specific `JwtAuthenticationFilter`.
- Valid JWT tokens pass through; invalid or missing tokens return `401 Unauthorized`.
- On successful validation, the Gateway injects the validated user context into downstream request headers:
  - `X-User-Id`: Subject identifier from JWT (e.g., `1001`)
  - `X-User-Email`: User email (e.g., `juan.delacruz@neobank.com`)

---

## 2. Global Conventions

### 2.1 Standard Request Headers
```http
Content-Type: application/json
Accept: application/json
Authorization: Bearer <JWT_TOKEN> # Required for all secured routes
```

### 2.2 Standard Error Response (`application/json`)
All microservices and the Gateway adhere to this unified error structure:
```json
{
  "timestamp": "2026-09-07T05:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Insufficient funds in account",
  "path": "/transactions/transfer"
}
```

### 2.3 Common HTTP Status Codes
- `200 OK`: Request succeeded.
- `201 Created`: Resource created successfully.
- `400 Bad Request`: Validation failure or business rule violation.
- `401 Unauthorized`: Missing, expired, or invalid JWT.
- `403 Forbidden`: Authenticated user lacks permission.
- `404 Not Found`: Target entity does not exist.
- `500 Internal Server Error`: Unhandled server-side failure.

---

## 3. Auth Service Contract (`AUTH-SERVICE`)

### 3.1 User Login
Generates a signed JWT token upon valid credential submission.

- **Method / Path:** `POST /login`
- **Auth:** Public
- **Request Body:**
```json
{
  "email": "juan.delacruz@neobank.com",
  "password": "Password123!"
}
```

- **Response `200 OK`:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "userId": 1001,
  "email": "juan.delacruz@neobank.com"
}
```

- **Response `401 Unauthorized`:**
```json
{
  "timestamp": "2026-09-07T05:16:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid email or password",
  "path": "/login"
}
```

### 3.2 JWT Payload Structure
```json
{
  "sub": "1001",
  "email": "juan.delacruz@neobank.com",
  "iat": 1788758160,
  "exp": 1788844560
}
```

---

## 4. Account Service Contract (`ACCOUNT-SERVICE`)
Base Path through Gateway: `/accounts`

### 4.1 Get Account Balance
Retrieves real-time account balance details for an authenticated user.

- **Method / Path:** `GET /accounts/{accountId}/balance`
- **Auth:** Bearer JWT required (`X-User-Id` must match account owner)
- **Parameters:**
  - `accountId` (path variable, integer): ID of the account.

- **Response `200 OK`:**
```json
{
  "accountId": 2001,
  "accountNumber": "ACC-987654321",
  "userId": 1001,
  "currency": "PHP",
  "balance": 15000.75,
  "status": "ACTIVE",
  "updatedAt": "2026-09-07T05:00:00Z"
}
```

- **Response `404 Not Found`:**
```json
{
  "timestamp": "2026-09-07T05:16:30Z",
  "status": 404,
  "error": "Not Found",
  "message": "Account ID 2001 not found",
  "path": "/accounts/2001/balance"
}
```

### 4.2 Get Account Profile
Retrieves account and customer metadata.

- **Method / Path:** `GET /accounts/{accountId}/profile`
- **Auth:** Bearer JWT required
- **Response `200 OK`:**
```json
{
  "accountId": 2001,
  "accountNumber": "ACC-987654321",
  "accountType": "SAVINGS",
  "ownerName": "Juan Dela Cruz",
  "email": "juan.delacruz@neobank.com",
  "status": "ACTIVE"
}
```

### 4.3 Internal Balance Adjustment (Inter-Service REST)
Endpoint invoked directly by `TRANSACTION-SERVICE` during fund transfers.

- **Method / Path:** `PUT /accounts/{accountId}/adjust-balance`
- **Auth:** Internal communication (or forwarded JWT)
- **Request Body:**
```json
{
  "transactionReference": "TXN-20260907-8891",
  "amount": -500.00,
  "type": "DEBIT"
}
```
*Note: Negative amount indicates deduction (DEBIT), positive indicates addition (CREDIT).*

- **Response `200 OK`:**
```json
{
  "accountId": 2001,
  "newBalance": 14500.75,
  "updatedAt": "2026-09-07T05:18:00Z"
}
```

- **Response `400 Bad Request`:**
```json
{
  "timestamp": "2026-09-07T05:18:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Insufficient funds to execute debit",
  "path": "/accounts/2001/adjust-balance"
}
```

---

## 5. Transaction Service Contract (`TRANSACTION-SERVICE`)
Base Path through Gateway: `/transactions`

### 5.1 Fund Transfer
Executes a fund transfer from the authenticated user's account to a destination account. The Transaction Service coordinates with Account Service via REST to adjust balances.

- **Method / Path:** `POST /transactions/transfer`
- **Auth:** Bearer JWT required
- **Request Body:**
```json
{
  "sourceAccountId": 2001,
  "destinationAccountId": 2002,
  "amount": 500.00,
  "currency": "PHP",
  "description": "Payment for lunch"
}
```

- **Response `201 Created`:**
```json
{
  "transactionId": "TXN-20260907-8891",
  "sourceAccountId": 2001,
  "destinationAccountId": 2002,
  "amount": 500.00,
  "currency": "PHP",
  "status": "COMPLETED",
  "description": "Payment for lunch",
  "timestamp": "2026-09-07T05:18:00Z"
}
```

- **Response `400 Bad Request` (Validation / Business Logic):**
```json
{
  "timestamp": "2026-09-07T05:18:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Transfer amount must be greater than zero",
  "path": "/transactions/transfer"
}
```

### 5.2 Transaction History
Fetches transaction logs for an account.

- **Method / Path:** `GET /transactions/account/{accountId}`
- **Auth:** Bearer JWT required
- **Response `200 OK`:**
```json
[
  {
    "transactionId": "TXN-20260907-8891",
    "sourceAccountId": 2001,
    "destinationAccountId": 2002,
    "amount": 500.00,
    "currency": "PHP",
    "status": "COMPLETED",
    "timestamp": "2026-09-07T05:18:00Z"
  }
]
```

---

## 6. End-to-End Postman / cURL Verification Sequence (Hour 5 Demo)

Teams can verify the entire running containerized stack using this test sequence:

### Step 1: Health & Discovery Check
Ensure all containers have registered with Eureka:
```bash
curl -s http://localhost:8761/eureka/apps | grep "<name>"
```

### Step 2: Authenticate via Gateway (Get JWT)
```bash
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"email":"juan.delacruz@neobank.com","password":"Password123!"}'
```
*Export the returned token: `export TOKEN="<eyJhbGciOi...>"*

### Step 3: Query Account Balance (Verified via Gateway)
```bash
curl -X GET http://localhost:8080/accounts/2001/balance \
  -H "Authorization: Bearer $TOKEN"
```

### Step 4: Unauthorized Request Test (Verify Gateway Rejection)
```bash
curl -i -X GET http://localhost:8080/accounts/2001/balance
# Expected: 401 Unauthorized
```

### Step 5: Execute Fund Transfer (Inter-Service REST Test)
```bash
curl -X POST http://localhost:8080/transactions/transfer \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountId": 2001,
    "destinationAccountId": 2002,
    "amount": 500.00,
    "currency": "PHP",
    "description": "Sprint Demo Transfer"
  }'
```

### Step 6: Verify Updated Balance
```bash
curl -X GET http://localhost:8080/accounts/2001/balance \
  -H "Authorization: Bearer $TOKEN"
# Expected: Balance decreased by 500.00
```
