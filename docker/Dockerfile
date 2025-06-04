FROM eclipse-temurin:23-jre

WORKDIR /cloud

COPY ../agent/build/libs/polocloud-agent.jar /cloud/cloud-agent.jar

ENTRYPOINT ["java", "-jar", "/cloud/cloud-agent.jar"]