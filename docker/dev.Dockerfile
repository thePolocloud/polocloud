# build -> runtime (requires repo as build context)
# docker build -t polocloud-dev -f docker/dev.Dockerfile .

FROM azul/zulu-openjdk:25 AS builder

ARG RUNNER_SEARCH_PATTERN="runner-*.local.jar" # replace if outdated

COPY . /repo
WORKDIR /repo

RUN ./gradlew clean --no-daemon -Porg.gradle.java.installations.paths=/opt/java/openjdk \
 && ./gradlew :node:test :cli:test --no-daemon -Porg.gradle.java.installations.paths=/opt/java/openjdk \
 && ./gradlew :runner:jar --no-configure-on-demand --no-daemon -Porg.gradle.java.installations.paths=/opt/java/openjdk \
 && mkdir -p /build \
 && find . -name "${RUNNER_SEARCH_PATTERN}" -exec cp {} /build/runner.jar \; \
 && test -f /build/runner.jar

FROM azul/zulu-openjdk:25-jre AS runtime

COPY --from=builder --chown=1000:1000 /build/runner.jar /app/runner.jar

WORKDIR /data
USER 1000:1000
ENTRYPOINT ["java", "-jar", "/app/runner.jar"]