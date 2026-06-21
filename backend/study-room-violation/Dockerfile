FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/study-room-violation-1.0.0.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-Xms48m", "-Xmx80m", "-XX:+UseG1GC", "-jar", "app.jar"]
