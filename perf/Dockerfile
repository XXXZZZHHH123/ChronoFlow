FROM maven:3.9.6-eclipse-temurin-17 AS deps
WORKDIR /workspace/perf

COPY perf/pom.xml .
RUN mvn -B -U dependency:go-offline

FROM maven:3.9.6-eclipse-temurin-17 AS runner
WORKDIR /workspace/perf

COPY --from=deps /root/.m2 /root/.m2
COPY perf/ /workspace/perf/

ENV CLOUDSDK_CORE_DISABLE_PROMPTS=1

RUN apt-get update \
    && apt-get install -y apt-transport-https ca-certificates curl gnupg \
    && mkdir -p /etc/apt/keyrings \
    && curl -fsSL https://packages.cloud.google.com/apt/doc/apt-key.gpg | gpg --dearmor -o /etc/apt/keyrings/cloud.google.gpg \
    && echo "deb [signed-by=/etc/apt/keyrings/cloud.google.gpg] https://packages.cloud.google.com/apt cloud-sdk main" > /etc/apt/sources.list.d/google-cloud-sdk.list \
    && apt-get update \
    && apt-get install -y google-cloud-cli \
    && rm -rf /var/lib/apt/lists/*

RUN chmod +x /workspace/perf/entrypoint.sh

ENTRYPOINT ["/workspace/perf/entrypoint.sh"]
