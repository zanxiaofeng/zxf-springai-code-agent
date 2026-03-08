FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build
COPY pom.xml .
COPY codeinsight-common/pom.xml codeinsight-common/
COPY codeinsight-model/pom.xml codeinsight-model/
COPY codeinsight-parser/pom.xml codeinsight-parser/
COPY codeinsight-indexer/pom.xml codeinsight-indexer/
COPY codeinsight-agent-core/pom.xml codeinsight-agent-core/
COPY codeinsight-security/pom.xml codeinsight-security/
COPY codeinsight-service/pom.xml codeinsight-service/
COPY codeinsight-web/pom.xml codeinsight-web/

RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

COPY --from=builder /build/codeinsight-web/target/codeinsight-web-*.jar app.jar

RUN addgroup -S app && adduser -S app -G app
RUN mkdir -p /data/repos && chown app:app /data/repos

USER app

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
