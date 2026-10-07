# Microservices Roadmap for Beginners

A step-by-step path from zero to building microservices. Don't skip steps: each one is needed for the next. Use this repo as your practice project.

## What is a microservice?

Instead of one big application (a monolith), you build several small applications. Each does one job, has its own database, and talks to the others over the network.

Example from this repo: `user-service` handles login, `product-service` handles products, `order-service` handles orders.

## Step 1 — Learn the basics (2–4 weeks)

You can't build microservices before you can build one normal app.

- Java 17: classes, interfaces, collections, streams, exceptions
- SQL: tables, primary/foreign keys, joins (MySQL)
- Git: commit, branch, push, pull request
- HTTP and REST: GET/POST/PUT/DELETE, status codes, JSON

**Goal:** write a small program and query a database by hand.

## Step 2 — Build one Spring Boot app (3–4 weeks)

- Spring Boot project setup (Maven)
- Controller → Service → Repository layers
- Spring Data JPA: entities and repositories
- DTOs (what the API sends) vs. entities (what the database stores)
- Validation (`@Valid`) and error handling (`@RestControllerAdvice`)
- Test with Postman, then write unit tests (JUnit + Mockito)

**Goal:** a CRUD API for products with MySQL.
**Look at:** `product-service/`

## Step 3 — Add login and security (1–2 weeks)

- Password hashing (BCrypt)
- JWT tokens: login returns a token, later requests send it
- Roles: `CUSTOMER` vs `ADMIN`

**Goal:** register, log in, and call a protected endpoint.
**Look at:** `user-service/`

## Step 4 — Split into several services (2 weeks)

- Create a second and third service (e.g. cart, order)
- Rule 1: each service has **its own database**
- Rule 2: never read another service's tables; ask its API instead
- Call another service with **OpenFeign**

**Goal:** `cart-service` fetches product info from `product-service`.
**Look at:** `cart-service/client/ProductClient.java`

## Step 5 — Service discovery and gateway (1 week)

Problem: how does one service find another, and what URL does the frontend call?

- **Eureka** (`discovery-server`): a phone book. Services register their names.
- **API Gateway** (`api-gateway`): the single front door. Frontend calls port `8080`; the gateway routes to the right service.

**Goal:** call every service only through `http://localhost:8080`.

## Step 6 — Events with Kafka (1–2 weeks)

Not everything needs an immediate answer. When an order is confirmed, the email can be sent later.

- Producer: `order-service` publishes "order confirmed"
- Consumer: `notification-service` reads it and sends the email
- If the email fails, the order still succeeds

**Rule of thumb:** need an answer now → HTTP. Just announcing something happened → event.

**Look at:** `notification-service/listener/`

## Step 7 — Docker (1–2 weeks)

- What an image and a container are
- Write a `Dockerfile` for one service
- Write `docker-compose.yml` to start MySQL, Kafka, and all services with one command

**Goal:** `docker compose up` runs the whole project.
*(Not yet in this repo — a great first contribution.)*

## Step 8 — Make it reliable (2 weeks)

Networks fail. Learn to survive it.

- Timeouts on every remote call
- Retries and circuit breakers (Resilience4j)
- Health checks (`/actuator/health`)
- Logs you can actually read

## Step 9 — Go deeper (ongoing)

| Topic | Why it matters |
|---|---|
| Saga pattern | Keeping data consistent across services without one big transaction |
| Outbox pattern | Never lose an event when the database commit succeeds |
| Flyway | Version-controlled database changes |
| Testcontainers | Integration tests with real MySQL/Kafka |
| Prometheus + Grafana + tracing | See what's happening in production |
| GitHub Actions | Automatic build and test |
| Kubernetes | Run containers at scale |

## Weekly study plan example

| Weeks | Focus |
|---|---|
| 1–4 | Java, SQL, Git, REST |
| 5–8 | Spring Boot CRUD app |
| 9–10 | JWT security |
| 11–12 | Multiple services + Feign |
| 13 | Eureka + Gateway |
| 14–15 | Kafka |
| 16–17 | Docker + compose |
| 18+ | Resilience, testing, CI/CD, Kubernetes |

## Common beginner mistakes

- Starting with microservices before learning a single Spring Boot app
- Sharing one database between services
- Making too many tiny services — start with 3–4
- Calling services in long chains (A → B → C → D); it's slow and fragile
- Committing passwords or API keys to Git (use environment variables)
- Skipping tests

## How to practice with this repo

1. Run one service alone (`./mvnw spring-boot:run` inside its folder)
2. Read its controller, then service, then repository
3. Start Eureka, then two services; watch them register at `http://localhost:8761`
4. Follow one request: gateway → service → database
5. Add a small feature (e.g. a "wishlist" endpoint) to one service
6. Write a test for it
