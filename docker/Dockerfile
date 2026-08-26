# clone -> build -> runtime (no host repo required)
# docker build -t polocloud -f docker/Dockerfile .

FROM alpine/git AS cloner

ARG TARGET_VERSION="master" # pin version via: "v3.0.2-alpha.2"
ARG REPO_URL="https://github.com/thePolocloud/polocloud.git" # optional for forks
RUN git clone "${REPO_URL}" /repo \
    && git -C /repo checkout "${TARGET_VERSION}"

FROM azul/zulu-openjdk:25 AS builder

ARG RUNNER_SEARCH_PATTERN="runner-*.local.jar" # replace if outdated

COPY --from=cloner /repo /repo
WORKDIR /repo

ENV GRADLE_OPTS="-Dorg.gradle.daemon=false"

RUN ./gradlew clean \
    && ./gradlew :node:test :cli:test \
    && ./gradlew :runner:jar --no-configure-on-demand \
    && mkdir -p /build \
    && find . -name "${RUNNER_SEARCH_PATTERN}" -exec cp {} /build/runner.jar \; \
    && test -f /build/runner.jar

FROM azul/zulu-openjdk:25-jre AS runtime

COPY --from=builder --chown=1000:1000 /build/runner.jar /app/runner.jar

ENV TERM="dumb"

WORKDIR /data
VOLUME /data
USER 1000:1000
ENTRYPOINT ["java", "-jar", "/app/runner.jar"]