FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/analytics-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
