FROM eclipse-temurin:21-jre

WORKDIR /cloud

COPY ../agent/build/libs/agent-1.0-SNAPSHOT.jar /cloud/cloud-agent.jar

ENTRYPOINT ["java", "-jar", "/cloud/cloud-agent.jar"]