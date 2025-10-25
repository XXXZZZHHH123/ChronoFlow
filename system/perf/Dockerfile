FROM maven:3.9.6-eclipse-temurin-17 AS deps
WORKDIR /workspace

COPY pom.xml .
COPY system/pom.xml system/pom.xml
COPY common/pom.xml common/pom.xml
COPY framework/pom.xml framework/pom.xml

RUN mvn -B -f pom.xml -pl system -am dependency:go-offline -DskipTests

COPY . .

RUN mvn -B -pl system -am -DskipTests install

FROM maven:3.9.6-eclipse-temurin-17 AS runner
WORKDIR /workspace

COPY --from=deps /workspace /workspace
COPY --from=deps /root/.m2 /root/.m2
COPY system/perf/entrypoint.sh /entrypoint.sh

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
RUN chmod +x /entrypoint.sh

ENV MAVEN_OPTS="-XX:+UseContainerSupport"

ENTRYPOINT ["/entrypoint.sh"]
