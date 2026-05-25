# SwiftLink

A production-grade URL shortener API with click analytics, built with Java 25 + Spring Boot 3.4.x.
Deployed as a container on AWS Lambda + API Gateway. Cloud-portable by design.

## Features

- Create short URLs with optional custom aliases and expiry dates
- Instant redirect (302) with sub-millisecond cache lookup
- Async click analytics: referrer, device, browser, country breakdowns
- Atomic click counters (DynamoDB `ADD` — race-condition safe)
- Resilience4j circuit breaker + retry on all database calls
- Caffeine in-process cache (swap to Redis for multi-instance)
- OpenAPI 3 / Swagger UI at `/swagger-ui.html`
- Structured JSON logging for CloudWatch Insights
- Health/liveness/readiness probes at `/actuator/health`

## Tech stack

| Layer | Technology |
|-------|-----------|
| Language | Java 25 + virtual threads |
| Framework | Spring Boot 3.4.x |
| Database | AWS DynamoDB v2 (enhanced client) |
| Cache | Caffeine (in-process) |
| Resilience | Resilience4j CircuitBreaker + Retry |
| Metrics | Micrometer → CloudWatch |
| API Docs | SpringDoc OpenAPI 3 |
| Deployment | AWS Lambda (container) + API Gateway HTTP v2 |
| IaC | Terraform 1.9+ |
| CI/CD | GitHub Actions |
| Container | Docker (eclipse-temurin:25-jre-alpine + Lambda Web Adapter) |

---

## Running locally

### Prerequisites

- Java 25 (`sdk install java 25-tem` via SDKMAN, or download from [Adoptium](https://adoptium.net))
- Docker + Docker Compose
- Maven 3.9+ (or use `./mvnw`)

### Option 1 — Docker Compose (recommended)

```bash
git clone <this-repo>
cd swiftlink

docker compose up
```

This starts:
- **SwiftLink API** on `http://localhost:8080`
- **DynamoDB Local** on `http://localhost:8000`

To also start **DynamoDB Admin UI** (browse tables at `http://localhost:8001`):

```bash
docker compose --profile debug up
```

### Option 2 — Maven directly

Start DynamoDB Local first:

```bash
docker run -d -p 8000:8000 amazon/dynamodb-local:2.5.2 \
  -jar DynamoDBLocal.jar -sharedDb -inMemory
```

Then run the app:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Verify it's working

```bash
# Health check
curl http://localhost:8080/actuator/health

# Create a short URL
curl -X POST http://localhost:8080/api/v1/urls \
  -H 'Content-Type: application/json' \
  -d '{"longUrl":"https://www.example.com/some/long/path"}'

# Follow the redirect (copy shortCode from the response above)
curl -L http://localhost:8080/<shortCode>

# Get analytics
curl http://localhost:8080/api/v1/urls/<shortCode>/analytics

# Swagger UI
open http://localhost:8080/swagger-ui.html
```

---

## Running tests

```bash
./mvnw verify
```

Tests use Mockito for unit tests and `@WebMvcTest` for controller slice tests.
Integration tests (tagged `*IT.java`) use Testcontainers + LocalStack when available.

---

## Project structure

```
swiftlink/
├── src/
│   ├── main/java/com/swiftlink/
│   │   ├── config/          # AppProperties, DynamoDB config, cache, OpenAPI
│   │   ├── controller/      # UrlController, AnalyticsController
│   │   ├── service/         # UrlShortenerService, AnalyticsService
│   │   ├── repository/      # Cloud-agnostic interfaces
│   │   │   └── dynamodb/    # AWS DynamoDB implementations
│   │   ├── model/           # UrlMapping, ClickEvent (DynamoDB beans)
│   │   ├── dto/             # Request/response records
│   │   ├── exception/       # Domain exceptions + global handler
│   │   └── util/            # ShortCodeGenerator
│   └── main/resources/
│       ├── application.yml          # Base config
│       ├── application-local.yml    # Local dev overrides
│       └── application-aws.yml     # AWS Lambda overrides
├── infra/
│   ├── bootstrap/           # ONE-TIME state bucket setup
│   ├── modules/             # Reusable Terraform modules
│   │   ├── ecr/
│   │   ├── dynamodb/
│   │   ├── iam/
│   │   ├── lambda/
│   │   └── api_gateway/
│   └── environments/
│       ├── dev/
│       └── prod/
├── .github/workflows/
│   ├── ci.yml               # PR checks: test, build, security scan
│   ├── deploy-dev.yml       # Push to develop → deploy to dev Lambda
│   └── deploy-prod.yml      # Push to main/tag → deploy to prod Lambda
├── Dockerfile
├── docker-compose.yml
└── CLAUDE.md                # Full context for AI assistants
```

---

## API reference

### Create short URL
```http
POST /api/v1/urls
Content-Type: application/json

{
  "longUrl": "https://www.example.com/very/long/path",
  "customAlias": "my-link",          // optional
  "title": "My Link",                // optional
  "expiresAt": "2027-01-01T00:00:00Z", // optional ISO-8601
  "tags": ["marketing", "campaign"], // optional
  "createdBy": "user-id"            // optional
}
```

### Get URL info
```http
GET /api/v1/urls/{shortCode}
```

### Delete URL
```http
DELETE /api/v1/urls/{shortCode}
```

### Get analytics
```http
GET /api/v1/urls/{shortCode}/analytics
```

### Redirect
```http
GET /{shortCode}  → 302 redirect
```

---

## Deploying to AWS (first time)

### Prerequisites

- [AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html) configured with your account
- [Terraform 1.9+](https://developer.hashicorp.com/terraform/install)
- Docker (to build and push the container image)

### Step 1 — Bootstrap Terraform state

This creates the S3 bucket and DynamoDB table that Terraform uses to store state.
**Run this once per AWS account.**

```bash
cd infra/bootstrap
terraform init
terraform apply \
  -var="aws_region=us-east-1" \
  -var="project_name=swiftlink"
```

Note the output values:

```
state_bucket_name = "swiftlink-tf-state-<your-account-id>"
lock_table_name   = "swiftlink-tf-locks"
```

### Step 2 — Update backend configuration

Edit both `infra/environments/dev/backend.tf` and `infra/environments/prod/backend.tf`.
Replace the placeholder values with the outputs from Step 1:

```hcl
backend "s3" {
  bucket         = "swiftlink-tf-state-<your-account-id>"  # ← replace
  key            = "swiftlink/dev/terraform.tfstate"
  region         = "us-east-1"
  dynamodb_table = "swiftlink-tf-locks"                     # ← replace
  encrypt        = true
}
```

### Step 3 — Provision dev infrastructure

```bash
cd infra/environments/dev
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars with your values

terraform init
terraform plan
terraform apply
```

Note the output:
```
ecr_repository_url  = "123456789.dkr.ecr.us-east-1.amazonaws.com/swiftlink-dev"
lambda_function_name = "swiftlink-dev"
api_endpoint        = "https://xxxxxxxxxx.execute-api.us-east-1.amazonaws.com"
```

### Step 4 — Build and push the first image

```bash
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
AWS_REGION=us-east-1
ECR_URL="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/swiftlink-dev"

# Build JAR
./mvnw package -DskipTests

# Authenticate to ECR
aws ecr get-login-password --region $AWS_REGION | \
  docker login --username AWS --password-stdin "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

# Build and push
docker build -t "${ECR_URL}:dev-latest" .
docker push "${ECR_URL}:dev-latest"

# Update the Lambda function
aws lambda update-function-code \
  --function-name swiftlink-dev \
  --image-uri "${ECR_URL}:dev-latest" \
  --region $AWS_REGION
```

### Step 5 — Test the deployed API

```bash
API_URL="https://xxxxxxxxxx.execute-api.us-east-1.amazonaws.com"  # from Step 3

curl "${API_URL}/actuator/health"

curl -X POST "${API_URL}/api/v1/urls" \
  -H 'Content-Type: application/json' \
  -d '{"longUrl":"https://www.example.com"}'
```

### Step 6 — Set up GitHub Actions

Add these secrets in your GitHub repository settings (`Settings → Secrets and variables → Actions`):

| Secret | Description |
|--------|-------------|
| `AWS_ACCESS_KEY_ID` | IAM user access key (needs ECR push + Lambda update permissions) |
| `AWS_SECRET_ACCESS_KEY` | IAM user secret key |

After this, pushing to `develop` automatically builds and deploys to dev Lambda.

---

## Deploying to production

```bash
cd infra/environments/prod
cp terraform.tfvars.example terraform.tfvars
# Edit terraform.tfvars

terraform init
terraform apply
```

Production deployments are triggered by:
- Pushing to `main` branch
- Creating a git tag like `v1.2.3`
- Manually via `workflow_dispatch` (to promote a specific dev image)

The `prod` GitHub Environment requires **manual approval** — configure reviewers under
`Settings → Environments → prod`.

---

## Making it cloud-portable

The repository pattern means swapping cloud storage is a code change, not an architecture change:

### GCP (Cloud Run + Firestore)
1. Implement `FirestoreUrlRepository implements UrlRepository`
2. Implement `FirestoreAnalyticsRepository implements AnalyticsRepository`
3. Add `@Profile("gcp")` on both, `@Profile("aws")` on DynamoDB implementations
4. Create `application-gcp.yml`
5. Deploy the same Docker image to Cloud Run — no Lambda/API Gateway needed

### Azure (Container Apps + CosmosDB)
1. Implement `CosmosDbUrlRepository implements UrlRepository`
2. Add `@Profile("azure")` on implementation, `@Profile("aws")` on DynamoDB
3. Create `application-azure.yml`
4. Deploy to Azure Container Apps

The Docker image is platform-agnostic. The Lambda Web Adapter extension is harmless outside Lambda
(it simply won't receive invocations).

---

## Cold starts

Java on Lambda has cold starts (typically 3–8 s for this app on first invocation after idle).
Options to mitigate:

| Option | Cost | Notes |
|--------|------|-------|
| Provisioned concurrency | ~$15/month for 1 instance | Eliminates cold starts entirely |
| Increase memory to 1024 MB+ | ~2× compute cost | Faster CPU → shorter cold start |
| ARM architecture | ~20% cheaper | Change `architectures = ["arm64"]` and build with `--platform linux/arm64` |
| Switch to GraalVM native | Free | Native compile with `mvn -Pnative` — requires GraalVM; cold start < 200 ms |

For a free-tier account, cold starts are acceptable. Enable provisioned concurrency when you have
real users.

---

## Environment variables (Lambda)

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `aws` | Spring profile |
| `SWIFTLINK_BASE_URL` | — | Public API base URL |
| `SWIFTLINK_URL_TABLE` | `swiftlink-urls` | DynamoDB URL table name |
| `SWIFTLINK_ANALYTICS_TABLE` | `swiftlink-analytics` | DynamoDB analytics table name |
| `PORT` | `8080` | Port the app listens on (required by Lambda Web Adapter) |
| `READINESS_CHECK_PATH` | `/actuator/health/liveness` | Lambda Web Adapter readiness path |
