FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn -B -DskipTests -pl :system -am clean package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/system/target/system.jar /app/app.jar
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["sh","-c","java -Dserver.port=${PORT} -jar /app/app.jar"]