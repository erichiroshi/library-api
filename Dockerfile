# ---------- Build Stage ----------
FROM gradle:9.2.1-jdk25 AS build

WORKDIR /app

COPY . .

RUN gradle clean bootJar --no-daemon

# ---------- Runtime Stage ----------
FROM eclipse-temurin:25-jre

WORKDIR /app

RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

#		docker build -t library-api .

#		docker run -d --network library-api_backend -p 8080:8080 -e SPRING_PROFILES_ACTIVE=prod library-api

#		docker compose -f docker-compose.dev.yml up -d

#		alias resetdb="docker-compose down -v && docker compose -f docker-compose.dev.yml up -d"

