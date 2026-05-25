# ─── Stage 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /build

# Copy dependency manifests first for layer caching
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

COPY src ./src
RUN ./mvnw package -DskipTests -q

# Extract layers for optimised Docker image
RUN java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted

# ─── Stage 2: Lambda Web Adapter ─────────────────────────────────────────────
FROM public.ecr.aws/awsguru/aws-lambda-adapter:0.8.4 AS lambda-adapter

# ─── Stage 3: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine AS runtime

# Bring in the Lambda Web Adapter extension
COPY --from=lambda-adapter /lambda-adapter /opt/extensions/lambda-adapter

RUN addgroup -S swiftlink && adduser -S swiftlink -G swiftlink

WORKDIR /app

# Layered copy for better cache utilisation
COPY --from=builder /build/target/extracted/dependencies/ ./
COPY --from=builder /build/target/extracted/spring-boot-loader/ ./
COPY --from=builder /build/target/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/target/extracted/application/ ./

# Lambda Web Adapter needs the app to signal readiness on this path
ENV PORT=8080
ENV READINESS_CHECK_PATH=/actuator/health/liveness

USER swiftlink

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseZGC", \
    "-XX:+ZGenerational", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "org.springframework.boot.loader.launch.JarLauncher"]
