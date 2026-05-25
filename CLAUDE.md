# SwiftLink — Context for Claude

## What this is
SwiftLink is a production-grade URL shortener REST API built with Java 25 + Spring Boot 3.4.x,
deployed as a container on AWS Lambda + API Gateway (HTTP API v2). Analytics (click tracking) are
stored in a separate DynamoDB table. The architecture is intentionally cloud-portable via repository
interfaces.

## Package structure
```
com.swiftlink
├── config/         # Spring config, DynamoDB client, cache, OpenAPI, app properties
├── controller/     # UrlController (CRUD + redirect), AnalyticsController
├── service/        # UrlShortenerService, AnalyticsService
├── repository/     # Interfaces: UrlRepository, AnalyticsRepository
│   └── dynamodb/   # AWS DynamoDB implementations
├── model/          # DynamoDB beans: UrlMapping, ClickEvent
├── dto/            # Records: Create/Info/Analytics responses, ErrorResponse
├── exception/      # UrlNotFoundException, UrlExpiredException, ShortCodeConflict, GlobalExceptionHandler
└── util/           # ShortCodeGenerator (SecureRandom, base-62)
```

## Key design decisions
- **Repository abstraction**: `UrlRepository` and `AnalyticsRepository` are interfaces. To swap clouds,
  implement `FirestoreUrlRepository` (GCP) or `CosmosDbUrlRepository` (Azure) and change the `@Primary` bean.
- **Cloud deployment**: Container image deployed to Lambda via AWS Lambda Web Adapter (not Spring Cloud Function).
  The same image runs on Cloud Run (GCP) or Container Apps (Azure) without code changes.
- **Caching**: Caffeine in-process cache (5 min TTL, 10k entries) for URL resolution. Cache is evicted
  on delete and click-count update. For multi-instance scenarios, swap to Redis via Spring Cache abstraction.
- **Click recording**: async via `@Async` thread pool — never blocks the redirect response.
- **Atomic click counts**: `DynamoDbUrlRepository.incrementClickCount` uses DynamoDB `ADD` expression for
  concurrency-safe increments.
- **Resilience**: Resilience4j CircuitBreaker + Retry decorates all DynamoDB calls (name: `dynamodb`).
- **Profiles**: `local` (DynamoDB-local endpoint, DEBUG logging, auto-creates tables), `aws` (structured
  JSON logs, CloudWatch metrics enabled, reads env vars for table names and base URL).

## API endpoints
| Method | Path | Description |
|--------|------|-------------|
| POST | /api/v1/urls | Create short URL |
| GET | /api/v1/urls/{code} | Get URL info |
| DELETE | /api/v1/urls/{code} | Delete URL |
| GET | /api/v1/urls/{code}/analytics | Click analytics |
| GET | /{code} | Redirect (302) to original URL |
| GET | /actuator/health | Health (liveness + readiness probes) |
| GET | /swagger-ui.html | Swagger UI |

## DynamoDB tables
| Table | PK | SK | Purpose |
|-------|----|----|---------|
| swiftlink-urls | shortCode (S) | — | URL mappings |
| swiftlink-analytics | shortCode (S) | sortKey (S) epoch#uuid | Click events |

TTL is stored in `ttlEpoch` attribute (not yet populated — add if you want automatic DynamoDB TTL cleanup).

## Infra layout
```
infra/
├── bootstrap/          # ONE-TIME: creates S3 state bucket + DynamoDB lock table
├── modules/
│   ├── ecr/            # ECR repository + lifecycle policy
│   ├── dynamodb/       # Both app tables + alarms
│   ├── iam/            # Lambda execution role, DynamoDB + ECR + CloudWatch policies
│   ├── lambda/         # Lambda function (container image), log group, error/duration alarms
│   └── api_gateway/    # HTTP API v2, catch-all route → Lambda, throttle, CORS, access logs
└── environments/
    ├── dev/            # 512 MB Lambda, no PITR, 7-day logs, no alarms email
    └── prod/           # 1024 MB Lambda, PITR on, 30-day logs, SNS alarm email
```

## First-time AWS setup sequence
1. Run `infra/bootstrap` to create state bucket + lock table.
2. Update `bucket` and `dynamodb_table` in `infra/environments/*/backend.tf`.
3. Run `terraform init && terraform apply` in each environment.
4. Add GitHub Actions secrets: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`.
5. Push to `develop` → triggers `deploy-dev.yml`.

## Extending to GCP / Azure
- **GCP Cloud Run**: same Docker image, no changes. Use Firestore + implement `FirestoreUrlRepository`.
  Switch profile from `aws` to `gcp`. Add `application-gcp.yml` similar to `application-aws.yml`.
- **Azure Container Apps**: same Docker image. Use CosmosDB + implement `CosmosDbUrlRepository`.
- Terraform: each cloud has its own provider modules; the module interfaces (ECR→Artifact Registry,
  DynamoDB→Firestore, Lambda→Cloud Run) map 1:1 in concept.

## Local dev
```bash
docker compose up            # starts app + dynamodb-local
# or
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## Running tests
```bash
mvn verify
```

## Build commands
```bash
mvn package -DskipTests     # fast build
mvn verify                  # full build + tests
```

## Conventions
- Java records for all DTOs and config properties.
- No comments unless the WHY is non-obvious.
- Lombok only on `@DynamoDbBean` classes (records can't be beans).
- All validation at controller boundary via Jakarta Validation; no defensive checks inside services.
- Errors follow `ErrorResponse` record with consistent `error` code strings.
