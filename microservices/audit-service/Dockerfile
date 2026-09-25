FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/audit-service.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
