# Etapa de compilación
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa de ejecución
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/simucredito-0.0.1-SNAPSHOT.jar app.jar
# Copiamos el archivo de firebase si es necesario, aunque idealmente se usa como variable de entorno
COPY src/main/resources/firebase-service-account.json /app/firebase-service-account.json

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
