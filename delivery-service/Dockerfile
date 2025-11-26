# Multi-stage build for Delivery Service
FROM eclipse-temurin:17-jdk as builder
WORKDIR /app
COPY target/delivery-service-1.0.0.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./

EXPOSE 8088

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
