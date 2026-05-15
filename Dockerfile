FROM ubuntu:latest

WORKDIR /app

COPY target/rate-limit-demo-api-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]