FROM maven:3.9-eclipse-temurin-21
WORKDIR /app
COPY target/core-retail-ledger-engine-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080 8081 8082 8083 8084 8085 8086 8087
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
