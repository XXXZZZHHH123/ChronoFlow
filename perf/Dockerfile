FROM maven:3.9.6-eclipse-temurin-17 AS deps
WORKDIR /workspace/perf

COPY perf/pom.xml .
RUN mvn -B -U dependency:go-offline

FROM maven:3.9.6-eclipse-temurin-17 AS runner
WORKDIR /workspace/perf

COPY --from=deps /root/.m2 /root/.m2
COPY perf/ /workspace/perf/

RUN chmod +x /workspace/perf/entrypoint.sh

ENTRYPOINT ["/workspace/perf/entrypoint.sh"]
