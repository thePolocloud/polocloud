package dev.httpmarco.polocloud.launcher.dependency.sub;

import dev.httpmarco.polocloud.launcher.dependency.Dependency;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class SubDependencyHelper {

    public @NotNull String getPomUrl(String groupId, String artifactId, String version) {
        return String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.pom", groupId.replace('.', '/'), artifactId, version, artifactId, version);
    }

    @SneakyThrows
    public List<Dependency> findSubDependencies(Dependency dependency) {
        var subDependencies = new ArrayList<Dependency>();

        var url = new URL(getPomUrl(dependency.groupId(), dependency.artifactId(), dependency.version()));
        var connection = (HttpURLConnection) url.openConnection();
        try (var inputStream = connection.getInputStream()) {
            var factory = DocumentBuilderFactory.newInstance();
            var builder = factory.newDocumentBuilder();
            var document = builder.parse(new InputSource(inputStream));

            var dependencyNodes = document.getElementsByTagName("dependency");
            for (int i = 0; i < dependencyNodes.getLength(); i++) {
                var dependencyElement = (Element) dependencyNodes.item(i);
                var groupIdValue = getTextContent(dependencyElement, "groupId");
                var artifactIdValue = getTextContent(dependencyElement, "artifactId");
                var versionValue = getTextContent(dependencyElement, "version");

                if (versionValue == null || versionValue.isEmpty() || (versionValue.startsWith("${") && versionValue.endsWith("}"))) {
                    versionValue = SubDependencyHelper.getLatestVersion(groupIdValue, artifactIdValue);
                }

                var subDependency = new Dependency(groupIdValue, artifactIdValue, versionValue);
                subDependencies.add(subDependency);
            }
        }

        return subDependencies;
    }

    public String getLatestVersion(String groupId, String artifactId) throws Exception {
        var metadataUrl = String.format("https://repo1.maven.org/maven2/%s/%s/maven-metadata.xml", groupId.replace(".", "/"), artifactId);
        var url = new URL(metadataUrl);
        var connection = (HttpURLConnection) url.openConnection();
        try (var inputStream = connection.getInputStream()) {
            var factory = DocumentBuilderFactory.newInstance();
            var builder = factory.newDocumentBuilder();
            var document = builder.parse(new InputSource(inputStream));

            var versioningNodes = document.getElementsByTagName("versioning");
            if (versioningNodes.getLength() > 0) {
                var versioningElement = (Element) versioningNodes.item(0);
                var releaseNodes = versioningElement.getElementsByTagName("release");
                if (releaseNodes.getLength() > 0) {
                    return releaseNodes.item(0).getTextContent();
                }
                var latestNodes = versioningElement.getElementsByTagName("latest");
                if (latestNodes.getLength() > 0) {
                    return latestNodes.item(0).getTextContent();
                }
            }
        }
        throw new IllegalStateException("Unable to determine latest version for " + groupId + ":" + artifactId);
    }

    private String getTextContent(Element element, String tagName) {
        var nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

}
