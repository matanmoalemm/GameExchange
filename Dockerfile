# Build stage
FROM maven:4.0.0-rc-5-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

#Runtime stage
FROM amazoncorretto:21
ARG APP_VERSION=1.0.0



WORKDIR /app
COPY --from=build /build/target/Game_Exchange-*.jar /app/

EXPOSE 8080

ENV JAR_VERSION=${APP_VERSION}


CMD java -jar Game_Exchange-${JAR_VERSION}.jar