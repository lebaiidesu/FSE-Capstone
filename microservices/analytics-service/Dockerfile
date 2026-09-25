FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/target/analytics-service.jar app.jar
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.3.0/opentelemetry-javaagent.jar otel-agent.jar
EXPOSE 8088
ENTRYPOINT ["java", "-javaagent:/app/otel-agent.jar", "-Dotel.service.name=analytics-service", "-Dotel.exporter.otlp.endpoint=http://otel-collector:4317", "-Dotel.exporter.otlp.protocol=grpc", "-Dotel.metrics.exporter=otlp", "-Dotel.logs.exporter=otlp", "-jar", "app.jar"]
