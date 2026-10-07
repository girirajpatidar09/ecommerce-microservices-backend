# Microservices Roadmap (From Scratch)

A phased path for building this e-commerce backend from an empty folder to a production-ready system. Each phase ends with something runnable. Status reflects the current repository: ✅ done · 🟡 partial · ⬜ not started.

## Phase 0 — Foundations (1 week)

Learn/confirm before writing services.

- Java 17, Spring Boot, REST, JPA/Hibernate, SQL
- Maven multi-module vs. separate repos (this repo: one folder per service, each with its own `pom.xml`)
- Git branching, Docker basics
- Domain modelling: split by business capability (user, product, cart, order, payment, notification), not by technical layer

**Exit:** a one-page service map with each service's responsibility and data it owns.

## Phase 1 — Build a single service well (1–2 weeks)

Start with `user-service` as a normal Spring Boot app.

- Layers: controller → service → repository, DTOs + MapStruct
- Validation, global exception handler, consistent `ApiResponse`/`ErrorResponse`
- Own database (`user_db`), no shared tables
- Unit tests (JUnit 5, Mockito)

Status: ✅

## Phase 2 — Add the remaining core services (2–3 weeks)

Repeat the Phase 1 template for `product-service`, `cart-service`, `order-service`, `payment-service`.

- One database per service
- Never join across services — call an API or consume an event
- Add `/actuator/health` to each

Status: ✅

## Phase 3 — Service discovery and gateway (1 week)

- `discovery-server` (Eureka): services register by name
- `api-gateway` (Spring Cloud Gateway): single entry point, route by path
- Services stop using hard-coded host:port

Status: ✅

## Phase 4 — Security (1 week)

- JWT issued by `user-service` (BCrypt passwords)
- Gateway validates tokens and enforces `CUSTOMER` / `ADMIN` rules
- Propagate user identity to downstream services via headers
- Secrets from environment variables, never committed

Status: ✅ (🟡 secrets: move all DB/JWT/Razorpay values to env/secret store)

## Phase 5 — Synchronous communication (1 week)

- OpenFeign clients (`cart → product`, `cart → user`, `order → payment`)
- Timeouts and error mapping to `ExternalServiceException`
- Concurrency-safe stock decrement (locking) in `product-service`

Status: ✅ (⬜ retries/circuit breakers — see Phase 8)

## Phase 6 — Asynchronous communication (1–2 weeks)

- Kafka: `order-service` publishes `OrderConfirmed`; `notification-service` consumes and emails
- Define event schemas and version them
- Idempotent consumers, dead-letter topic

Status: 🟡 (flow works; ⬜ DLQ, schema versioning, outbox pattern)

## Phase 7 — Distributed data consistency (2 weeks)

Checkout spans cart, product, order, payment — no distributed transactions.

- Saga (orchestrated by `order-service`): reserve stock → create payment → confirm; compensate on failure (release stock, refund)
- Transactional outbox so DB write + event publish can't diverge
- Idempotency keys on payment and order creation endpoints

Status: 🟡 (orchestration + refunds exist; ⬜ outbox, idempotency keys)

## Phase 8 — Resilience (1 week)

- Resilience4j: circuit breaker, retry with backoff, bulkhead, rate limiter on the gateway
- Fallbacks for non-critical calls (e.g. product images)
- Graceful degradation: notification failure must never fail an order

Status: ⬜

## Phase 9 — Containerize and run locally (1 week)

- Dockerfile per service (multi-stage, non-root)
- `docker-compose.yml`: MySQL (one schema per service), Kafka, all services, startup ordering via health checks
- `.env.example`

Status: ⬜ **← recommended next step**

## Phase 10 — Configuration and API contracts (1 week)

- Centralized config (Spring Cloud Config or env-per-profile)
- OpenAPI/Swagger per service, aggregated at the gateway
- DB migrations with Flyway/Liquibase instead of `ddl-auto`
- Consumer-driven contract tests (Spring Cloud Contract / Pact)

Status: ⬜

## Phase 11 — Observability (1 week)

- Structured JSON logs with correlation/trace ID
- Micrometer + Prometheus + Grafana dashboards
- Distributed tracing (OpenTelemetry → Zipkin/Tempo)
- Alerts on error rate, latency, Kafka consumer lag

Status: 🟡 (Actuator only)

## Phase 12 — Testing strategy (ongoing)

- Unit tests (✅ started)
- Integration tests with Testcontainers (MySQL, Kafka)
- End-to-end checkout test through the gateway
- Load test with k6/Gatling (stock race conditions)

Status: 🟡

## Phase 13 — CI/CD (1 week)

- GitHub Actions: build + test per changed service, Docker image push
- Dependency and image scanning, secret scanning
- Deploy to a staging environment automatically; manual gate for prod

Status: ⬜

## Phase 14 — Kubernetes and production (2+ weeks)

- Deployments, Services, Ingress, ConfigMaps/Secrets, HPA, readiness/liveness probes
- Managed MySQL and Kafka; backups and restore drills
- Replace Eureka with Kubernetes service discovery (optional)
- Rollouts: rolling/canary, rollback plan

Status: ⬜

## Phase 15 — Feature growth

Add new services only when a clear boundary exists:

| Candidate | Why |
|---|---|
| Inventory service | Split stock from catalogue when write load differs |
| Search service | Elasticsearch/OpenSearch for catalogue search |
| Review/Rating service | Independent scaling and data |
| Coupon/Promotion service | Pricing rules evolving separately |
| Shipping/Tracking service | Carrier integrations, event-driven |
| Admin/BFF layer | Tailored APIs for web vs. mobile |

## Suggested order from here

1. Docker + docker-compose (Phase 9)
2. Flyway migrations + env-based secrets (Phases 4/10)
3. Resilience4j (Phase 8)
4. Outbox + idempotency + DLQ (Phases 6/7)
5. Testcontainers integration tests (Phase 12)
6. Observability (Phase 11)
7. CI/CD (Phase 13), then Kubernetes (Phase 14)

## Rules of thumb

- One service = one owner of its data; share contracts, not databases.
- Prefer events for "something happened", HTTP for "I need an answer now".
- Every remote call needs a timeout, and every consumer must be idempotent.
- Don't add a service until a monolith boundary hurts; don't add a tool until a problem needs it.
