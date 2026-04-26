# API Gateway

Spring Cloud Gateway (reactive / WebFlux) fronting all 7 backend microservices on **port 8080**. Handles JWT validation, CORS, aggregate Swagger UI, and a handful of cross-cutting utility endpoints (health/metrics/dashboard). Every `/api/**` request hits the gateway first — downstream services only accept requests from it in production.

---

## At a glance
| | |
|---|---|
| **Port** | 8080 |
| **Type** | Reactive (Spring WebFlux — Netty under the hood) |
| **Database** | none |
| **Kafka** | none (gateway doesn't produce/consume domain events) |
| **Aggregated Swagger UI** | http://localhost:8080/swagger-ui.html |
| **OpenAPI JSON (gateway itself)** | http://localhost:8080/v3/api-docs |
| **Downstream specs (proxied)** | http://localhost:8080/v3/api-docs/{auth,user,ticket,solution,knowledge,reward,notification}-service |
| **Java** | 21 (Temurin) |
| **Spring Boot** | 3.3.5 |
| **Spring Cloud** | 2023.0.3 |

---

## What it does
1. **Route** `/api/auth/**`, `/api/users/**`, `/api/tickets/**`, `/api/solutions/**`, `/api/knowledge/**`, `/api/rewards/**`, `/api/notifications/**` to the corresponding service ports 8081–8087.
2. **Authenticate** every `/api/**` call except `POST /api/auth/login` and `POST /api/auth/register` — see `AuthFilter`. The filter calls `auth-service` `GET /api/auth/validate`, parses the response, and injects `X-User-Id` + `X-User-Role` headers onto the forwarded request (using a `ServerHttpRequestDecorator` so Netty's read-only headers aren't mutated).
3. **CORS** — `CorsConfig` allows origins `localhost:3000`, `localhost:5173`, `127.0.0.1:3000`, `127.0.0.1:5173`, `ticketing.local`, and `ticketing.local:*`, with credentials + `Authorization` exposed. Preflight cached 1 hour.
4. **Aggregate Swagger UI** — one dropdown at `:8080/swagger-ui.html` lets you browse every service's OpenAPI spec without visiting each port. See the *Swagger aggregator* section below for how the plumbing works.
5. **Observability** — `/actuator/health`, `/actuator/metrics`, `/actuator/gateway` + custom `/api/health`, `/api/metrics`, `/api/dashboard`.

---

## Route table (from `application-local.yaml`)

### API routes (each requires JWT except `/api/auth/**`)
| ID | Predicate | Upstream | Filters |
|---|---|---|---|
| `auth-service` | `Path=/api/auth/**` | `http://localhost:8081` | `StripPrefix=0` |
| `user-service` | `Path=/api/users/**` | `http://localhost:8082` | `StripPrefix=0`, `AuthFilter` |
| `ticket-service` | `Path=/api/tickets/**` | `http://localhost:8083` | `StripPrefix=0`, `AuthFilter` |
| `solution-service` | `Path=/api/solutions/**` | `http://localhost:8084` | `StripPrefix=0`, `AuthFilter` |
| `knowledge-service` | `Path=/api/knowledge/**` | `http://localhost:8085` | `StripPrefix=0`, `AuthFilter` |
| `reward-service` | `Path=/api/rewards/**` | `http://localhost:8086` | `StripPrefix=0`, `AuthFilter` |
| `notification-service` | `Path=/api/notifications/**` | `http://localhost:8087` | `StripPrefix=0`, `AuthFilter` |

### Swagger aggregator routes (public)
| ID | Predicate | Upstream | Filter |
|---|---|---|---|
| `auth-service-docs` | `/v3/api-docs/auth-service` | `:8081` | `SetPath=/v3/api-docs` |
| `user-service-docs` | `/v3/api-docs/user-service` | `:8082` | `SetPath=/v3/api-docs` |
| `ticket-service-docs` | `/v3/api-docs/ticket-service` | `:8083` | `SetPath=/v3/api-docs` |
| `solution-service-docs` | `/v3/api-docs/solution-service` | `:8084` | `SetPath=/v3/api-docs` |
| `knowledge-service-docs` | `/v3/api-docs/knowledge-service` | `:8085` | `SetPath=/v3/api-docs` |
| `reward-service-docs` | `/v3/api-docs/reward-service` | `:8086` | `SetPath=/v3/api-docs` |
| `notification-service-docs` | `/v3/api-docs/notification-service` | `:8087` | `SetPath=/v3/api-docs` |

Override upstream URIs via env vars (`AUTH_SERVICE_URI`, `USER_SERVICE_URI`, …, `NOTIFICATION_SERVICE_URI`) when deploying to k8s / Docker Compose.

---

## AuthFilter — how JWT propagation works
```text
┌────────┐  Bearer <jwt>   ┌─────────┐  Bearer <jwt>     ┌──────────────┐
│ Client │ ───────────────▶│ Gateway │ ─────────────────▶│ auth-service │
└────────┘                 │         │                   │ /validate    │
                           │         │ ◀─ userId, role ──└──────────────┘
                           │         │
                           │         │  X-User-Id: 42
                           │         │  X-User-Role: ENGINEER
                           │         │ ─────────────────▶  downstream
                           └─────────┘
```
**Implementation notes:**
- `AuthFilter` is an `AbstractGatewayFilterFactory` — declared per-route in YAML under `filters: - AuthFilter`.
- Open endpoints (`/api/auth/login`, `/api/auth/register`) skip validation.
- If `Authorization` header missing or not `Bearer …`, returns `401 {"success":false,"message":"Missing or invalid Authorization header","data":null}`.
- If auth-service unreachable, returns `401` with `"Auth service unavailable"`.
- Downstream controllers read `X-User-Id` / `X-User-Role` (not the JWT directly) — simpler, and keeps auth logic in one place.

---

## Swagger aggregator — how it works
1. Each downstream service exposes its own `/v3/api-docs` and `/swagger-ui.html` (springdoc-openapi).
2. Gateway's `SwaggerUiConfigProperties` bean (in `OpenApiConfig`) declares **7 named URLs**, one per service, each pointing at `/v3/api-docs/<service-name>`.
3. Gateway's route table proxies each `/v3/api-docs/<service>` → downstream `/v3/api-docs` via `SetPath`.
4. Result: one UI at `:8080/swagger-ui.html`, pick any service from the dropdown (top-right corner).

```
http://localhost:8080/swagger-ui.html?urls.primaryName=ticket-service   # deep-link
```

---

## Configuration
| Env var | Yaml key | Default | Purpose |
|---|---|---|---|
| `SERVER_PORT` | `server.port` | `8080` | |
| `JWT_SECRET` | `jwt.secret` | (shared) | HS384 secret — must match every backend service |
| `JWT_EXPIRATION` | `jwt.expiration` | `86400000` | Token TTL (ms) |
| `AUTH_SERVICE_URI` | `auth-service.url` | `http://localhost:8081` | Used by AuthFilter to call `/api/auth/validate` |
| `USER_SERVICE_URI` | | `http://localhost:8082` | |
| `TICKET_SERVICE_URI` | | `http://localhost:8083` | |
| `SOLUTION_SERVICE_URI` | | `http://localhost:8084` | |
| `KNOWLEDGE_SERVICE_URI` | | `http://localhost:8085` | |
| `REWARD_SERVICE_URI` | | `http://localhost:8086` | |
| `NOTIFICATION_SERVICE_URI` | | `http://localhost:8087` | |

---

## Utility endpoints (Gateway-local controllers)
| Path | Purpose |
|---|---|
| `GET /api/health` | Lightweight health probe for front-end / monitoring |
| `GET /api/metrics` | Request/latency counters aggregated by `MetricsController` |
| `GET /api/dashboard` | Combined summary (used by home-page admin widget) |
| `GET /actuator/health` | Spring Boot actuator |
| `GET /actuator/metrics` | Micrometer registry |
| `GET /actuator/gateway/routes` | Runtime view of all gateway routes |

---

## Build & run
```bash
./services.sh start api-gateway
```
or:
```bash
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.11
cd api_gateway
mvn -DskipTests -Dmaven.test.skip=true spring-boot:run
```

## Docker / K8s
- Manifest: `k8s/api-gateway.yaml` (ClusterIP + Ingress `ticketing.local` → `:8080`)
- Service: `api-gateway`

---

## Troubleshooting

**All downstream routes return 401 even with a valid JWT**
Check `jwt.secret` matches across gateway **and** auth-service **and** every backend. They all sign/verify with the same HS384 key.

**CORS preflight fails in the browser console**
`CorsConfig` whitelists explicit origins. If the frontend runs on a non-listed port, add the origin pattern there — `globalcors` in yaml alone is not enough because `CorsWebFilter` takes precedence.

**Swagger UI dropdown is empty / broken**
Verify each downstream service's `/v3/api-docs` returns JSON: `curl http://localhost:8081/v3/api-docs | head`. Then test the gateway proxy: `curl http://localhost:8080/v3/api-docs/auth-service | head`. If the proxy fails, the service is likely down — start it with `./services.sh start auth-service`.

**`AuthFilter parse error`**
Auth-service's `/api/auth/validate` response shape changed. Filter expects `{userId, role}` at top level **or** nested under `{data: {userId, role}}`. If the shape changed again, extend `extractField()` in `AuthFilter.java`.

**Route not matching a request**
Inspect `/actuator/gateway/routes` (requires `management.endpoints.web.exposure.include: gateway`). Confirm predicates match your path. Order matters if multiple routes overlap.

---

## Tech stack
- Java 21 (Temurin)
- Spring Boot 3.3.5 + Spring Cloud 2023.0.3
- Spring Cloud Gateway (WebFlux reactive)
- Spring Security (reactive)
- springdoc-openapi-starter-webflux-ui 2.6.0
- JJWT (`jjwt-api`, `jjwt-impl`, `jjwt-jackson` 0.11.5)
- Caffeine cache (rate-limit store)
- Lombok 1.18.34
- `com.kva:common-library` 1.0.0
