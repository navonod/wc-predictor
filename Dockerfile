FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw && ./mvnw dependency:resolve
COPY src src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/data && apt-get update -qq && apt-get install -y -qq curl && rm -rf /var/lib/apt/lists/*
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
