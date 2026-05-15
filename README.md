# Spring Boot + Envoy Rate Limiting Demo

A demo project showing how to enforce API rate limiting at the proxy layer using **Envoy** and the **Lyft Rate Limit service**, with a Spring Boot backend. The app itself has zero rate-limiting logic — all enforcement happens in Envoy before requests ever reach Spring Boot.

## Architecture

```
Client (Postman / curl)
        |
        v
  Envoy :8090  ──gRPC──►  Rate Limit Service :8081
        |                         |
        |                       Redis
        v
  Spring Boot :8080
```

| Component | Role |
|---|---|
| **Envoy** | Public entry point. Intercepts every request, calls the rate limit service, forwards or rejects. |
| **Rate Limit Service** | Lyft's open-source gRPC service. Reads `limits.yaml`, checks counters in Redis, returns `ALLOW` or `OVER_LIMIT`. |
| **Redis** | Counter store for the rate limit service. |
| **Spring Boot** | API backend. Never sees blocked requests. |

## Rate Limit Rules

Defined in `ratelimit/config/limits.yaml`:

| Rule | Limit |
|---|---|
| Per IP address (any path) | 10 requests / minute |
| `/api/v1/hello-world` path | 5 requests / minute |

The stricter per-path rule means `/api/v1/hello-world` is effectively capped at 5/min even if your IP hasn't hit 10.

## Prerequisites

- Docker Desktop
- Maven (or use the included `mvnw`)
- Java 25

## Running

**1. Build the JAR**

```bash
./mvnw clean package -DskipTests
```

**2. Start the stack**

```bash
docker compose up --build
```

| Port | What |
|---|---|
| `8090` | Envoy (send all traffic here) |
| `8080` | Spring Boot direct (bypasses Envoy) |
| `9901` | Envoy admin dashboard |

## API

### `GET /api/v1/hello-world`

```bash
curl http://localhost:8090/api/v1/hello-world
```

```json
{
  "message": "Hello World",
  "client_ip": "172.18.0.1"
}
```

Rate limit headers are returned on every response:

```
x-ratelimit-limit: 5
x-ratelimit-remaining: 4
x-ratelimit-reset: 42
```

Once the limit is hit, Envoy returns `429 Too Many Requests` — no request reaches Spring Boot.

### `GET /actuator/health` (direct)

```bash
curl http://localhost:8080/actuator/health
```

Health check that bypasses Envoy. Useful to verify Spring Boot is up independently.

## Envoy Admin

```bash
# All metrics
curl http://localhost:9901/stats

# Rate limit counters only
curl -s http://localhost:9901/stats | grep ratelimit

# Full resolved config
curl http://localhost:9901/config_dump
```

## Testing with Postman

Import `spring-boot-envoy-ratelimit.postman_collection.json`. The collection includes:

- **Hello World** — basic request through Envoy
- **Hello World — Rate Limit Header Check** — asserts all three `x-ratelimit-*` headers are present
- **Trigger 429** — run via Collection Runner with 10 iterations, 0ms delay; requests 1–5 return 200, 6–10 return 429
- **Health Check (direct)** — hits Spring Boot on 8080, bypasses Envoy
- **Envoy Admin — Stats / Config Dump**

## Tech Stack

| | |
|---|---|
| Spring Boot | 4.0.6 |
| Java | 25 |
| Envoy | v1.29 |
| Lyft Rate Limit | `envoyproxy/ratelimit:master` |
| Redis | 7 Alpine |
