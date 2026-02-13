# ---------- Build Stage ----------
FROM gradle:9.2.1-jdk25 AS build

WORKDIR /app

COPY . .

RUN gradle clean bootJar --no-daemon

# ---------- Runtime Stage ----------
FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
