# Etapa de build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copiamos pom y código
COPY pom.xml .
COPY src ./src

# Compilar el jar
RUN mvn clean package -DskipTests

# Etapa de runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiamos el jar generado en la etapa de build
COPY --from=build /app/target/simucredito-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
