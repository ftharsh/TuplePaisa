# Wallet Banking App - Backend

This repository contains the backend functionality for a wallet-based banking application. The backend is built using **Spring Boot** and **MongoDB**, providing secure, scalable, and efficient APIs for user registration, login, wallet management, transactions, and more.

---

## Table of Contents

1. [Features](#features)
2. [Endpoints](#endpoints)
3. [Security Considerations](#security-considerations)
4. [Concurrency Handling](#concurrency-handling)
5. [Testing](#testing)
6. [Optional Features](#optional-features)
7. [Setup Instructions](#setup-instructions)
8. [Technologies Used](#technologies-used)
9. [Contributing](#contributing)

---

## Features

### Core Functionality
1. **User Registration**
   - Validate input data (email format, password strength).
   - Check for duplicate users.
   - Hash passwords securely using BCrypt.
   - Generate JWT tokens for authentication.

2. **User Login**
   - Authenticate users by verifying email and password.
   - Generate JWT tokens upon successful login.

3. **Wallet Recharge**
   - Validate recharge amounts.
   - Update wallet balances and create transaction records.
   - Support optional cashback feature.

4. **Wallet Transfer**
   - Validate transfer amounts and recipient details.
   - Ensure atomicity of transactions using database transactions.
   - Deduct from sender's wallet and credit to recipient's wallet.

5. **View Account Statement**
   - Retrieve paginated transaction history.
   - Filter transactions by date range and type.

6. **User Logout**
   - Invalidate JWT tokens (e.g., via blacklist or short-lived tokens).

### Optional Features
1. **Cashbacks**
   - Calculate and apply cashback based on recharge amounts.
   - Maintain a cashback history for users.

2. **Email Notifications**
   - Send welcome emails after registration.
   - Notify users about transaction confirmations and cashback rewards.

---

## Endpoints

### User Management
- `POST /api/auth/register` - Register a new user.
- `POST /api/auth/login` - Log in an existing user.
- `POST /api/auth/logout` - Log out the current user.

### Wallet Management
- `POST /api/wallet/recharge` - Recharge the user's wallet.
- `POST /api/wallet/transfer` - Transfer funds between wallets.
- `GET /api/wallet/statement` - View the user's transaction history.

### Cashbacks (Optional)
- `GET /api/cashback/history` - View the user's cashback history.

---

## Security Considerations

1. **JWT Authentication**
   - All protected endpoints require valid JWT tokens.
   - Tokens are validated using Spring Security.

2. **Rate Limiting**
   - Prevent abuse of APIs by implementing rate limiting.

3. **HTTPS**
   - All communications must use HTTPS to ensure data security.

4. **Error Handling**
   - Proper error handling and logging are implemented to prevent information leakage.

---

## Concurrency Handling

1. **Optimistic Locking**
   - MongoDB's optimistic locking mechanism ensures data consistency during concurrent updates.

2. **Retry Mechanisms**
   - Critical operations like wallet transfers implement retry logic to handle conflicts.

3. **Distributed Locks**
   - For highly critical operations, distributed locks can be used to ensure atomicity.

---

## Testing

1. **Unit Tests**
   - Comprehensive unit tests for all service methods.

2. **Integration Tests**
   - Integration tests for controllers using MockMvc.

3. **In-Memory MongoDB**
   - Use an in-memory MongoDB instance for isolated testing.

4. **Code Coverage**
   - Aim for at least 80% code coverage.

---

## Optional Features

1. **Cashbacks**
   - Implement logic to calculate and apply cashbacks.
   - Store cashback history in MongoDB.

2. **Email Notifications**
   - Use Spring's `JavaMailSender` to send transactional emails.
   - Notify users about account activities and rewards.

---

## Setup Instructions

### Prerequisites
- Java 17+
- MongoDB (or Docker for running MongoDB locally)
- Maven or Gradle for dependency management

### Steps

1. **Clone the Repository**
   ```bash
   git clone https://github.com/your-repo/wallet-banking-app-backend.git
   cd wallet-banking-app-backend
