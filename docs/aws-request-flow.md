# SwiftLink — AWS Request Flow

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           SwiftLink — AWS Request Flow                       │
└─────────────────────────────────────────────────────────────────────────────┘

  CLIENT
    │
    │  HTTPS request
    ▼
┌─────────────────────────────┐
│   API Gateway HTTP API v2   │  ← throttle: 50 rps / 200 burst
│   (swiftlink-dev)           │    CORS headers applied here
│   catch-all route $default  │    access logs → CloudWatch Logs
└─────────────┬───────────────┘      /aws/apigateway/swiftlink-dev
              │
              │  Lambda proxy integration
              │  payload format 2.0
              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  Lambda Function: swiftlink-dev  (1024 MB / 60s / x86_64 / container)      │
│                                                                              │
│  ┌──────────────────────────┐   COLD START only                             │
│  │  Lambda Web Adapter      │ ◄─────────────────── ECR  (image pull)        │
│  │  (extension /opt)        │                   265193792851.dkr.ecr…       │
│  │  polls runtime API       │                                                │
│  │  translates Lambda ↔ HTTP│                                                │
│  └──────────┬───────────────┘                                                │
│             │  HTTP on localhost:8080                                         │
│             ▼                                                                 │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  Spring Boot 3.5 / Java 25                                           │   │
│  │                                                                      │   │
│  │  UrlController / AnalyticsController                                 │   │
│  │       │                                                              │   │
│  │       ▼                                                              │   │
│  │  UrlShortenerService / AnalyticsService                              │   │
│  │       │                                                              │   │
│  │       ├─► Caffeine Cache (5 min TTL, 10k entries) ──► HIT: return   │   │
│  │       │    key: shortCode  /  shortCode-info             immediately │   │
│  │       │                                                              │   │
│  │       └─► Resilience4j CircuitBreaker + Retry ─────────────────┐    │   │
│  │            (name: dynamodb)                                     │    │   │
│  └─────────────────────────────────────────────────────────────────┼────┘   │
│                                                                     │        │
└─────────────────────────────────────────────────────────────────────┼────────┘
                                                                      │
                            ┌─────────────────────────────────────────┘
                            │  AWS SDK calls (IAM role: swiftlink-dev-lambda-role)
                            │
              ┌─────────────┴──────────────────────────────────┐
              │                                                  │
              ▼                                                  ▼
┌─────────────────────────┐                      ┌─────────────────────────────┐
│  DynamoDB               │                      │  DynamoDB                   │
│  swiftlink-dev-urls     │                      │  swiftlink-dev-analytics    │
│                         │                      │                             │
│  PK: shortCode (S)      │                      │  PK: shortCode (S)          │
│  originalUrl            │                      │  SK: sortKey (S)            │
│  clickCount (atomic ADD)│                      │      epoch#uuid             │
│  createdAt / expiresAt  │                      │  timestamp / userAgent      │
│  customAlias            │                      │  ipAddress / referer        │
└─────────────────────────┘                      └─────────────────────────────┘


─────────────────────────── OBSERVABILITY (all paths) ──────────────────────────

  Lambda stdout  ──►  CloudWatch Logs: /aws/lambda/swiftlink-dev
  API Gateway    ──►  CloudWatch Logs: /aws/apigateway/swiftlink-dev
  Spring Actuator──►  CloudWatch Metrics namespace: SwiftLink  (1m interval)
  Lambda runtime ──►  CloudWatch Metrics namespace: AWS/Lambda
  X-Ray tracing  ──►  AWS X-Ray  (active mode)

  Alarms (→ SNS in prod):
    swiftlink-dev-errors    Lambda errors  > 0  (1 min)
    swiftlink-dev-duration  Lambda p95     > threshold (3 min)
    swiftlink-dev-5xx       API GW 5xx     > 0  (1 min)
    swiftlink-dev-urls-throttle  DynamoDB throttles

─────────────────────────── COLD START PATH ────────────────────────────────────

  API GW → Lambda (COLD) → pull image from ECR → Lambda Web Adapter init
        → Spring Boot init (lazy: ~2-3s) → ready → serve request

─────────────────────────── REDIRECT PATH (hot) ────────────────────────────────

  Client → API GW → Lambda → Caffeine HIT → 302 (no DynamoDB)
                                ↓ async (@Async thread pool)
                              DynamoDB analytics write + clickCount ADD

─────────────────────────── IAM BOUNDARY ───────────────────────────────────────

  Lambda execution role (swiftlink-dev-lambda-role) grants:
    dynamodb:GetItem/PutItem/UpdateItem/DeleteItem/Query/Scan  → both tables
    ecr:GetDownloadUrlForLayer / BatchGetImage                 → swiftlink-dev repo
    logs:CreateLogGroup / PutLogEvents                         → /aws/lambda/*
    cloudwatch:PutMetricData                                   → namespace SwiftLink
```
