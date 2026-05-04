FROM eclipse-temurin:21-jdk
COPY target/*.jar app.jar
ADD target/Game_Exchange-0.0.1-SNAPSHOT.jar Game_Exchange-0.0.1-SNAPSHOT.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]