# clone -> build -> runtime (no host repo required)
# docker build -t polocloud -f docker/Dockerfile .

FROM alpine/git AS cloner

ARG REPO_URL=https://github.com/thePolocloud/polocloud.git # optional for forks
RUN git clone "${REPO_URL}" /repo

FROM azul/zulu-openjdk:25 AS builder

ARG RUNNER_SEARCH_PATTERN="runner-*.local.jar" # replace if outdated

COPY --from=cloner /repo /repo
WORKDIR /repo

RUN ./gradlew build \
      --no-daemon \
      -Porg.gradle.java.installations.paths=/opt/java/openjdk \
 && mkdir -p /build \
 && find . -name "${RUNNER_SEARCH_PATTERN}" -exec cp {} /build/runner.jar \; \
 && test -f /build/runner.jar

FROM azul/zulu-openjdk:25-jre AS runtime

COPY --from=builder --chown=1000:1000 /build/runner.jar /app/runner.jar

WORKDIR /data
VOLUME /data
USER 1000:1000
ENTRYPOINT ["java", "-jar", "/app/runner.jar"]