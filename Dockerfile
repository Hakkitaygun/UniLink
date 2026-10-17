# ---- 1. aşama: derleme ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests package

# ---- 2. aşama: çalıştırma ----
FROM eclipse-temurin:25-jre
WORKDIR /app

RUN useradd --system --no-create-home appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]