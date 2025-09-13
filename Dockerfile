FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

ARG REVISION=1.0.0
ARG MODULE=system

COPY . .
RUN mvn -B -Drevision=${REVISION} -pl ${MODULE} -am clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

ARG REVISION=1.0.0
ARG MODULE=system

COPY --from=build /app/${MODULE}/target/${MODULE}-${REVISION}.jar app.jar

ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-jar", "/app/app.jar"]