package dev.httpmarco.polocloud.launcher.dependency.sub;

import dev.httpmarco.polocloud.launcher.dependency.Dependency;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
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

        var document = fetchPomXmlDocument(dependency.groupId(), dependency.artifactId(), dependency.version());
        var dependencyNodes = document.getElementsByTagName("dependency");

        for (int i = 0; i < dependencyNodes.getLength(); i++) {
            var dependencyElement = (Element) dependencyNodes.item(i);
            var groupIdValue = getTextContent(dependencyElement, "groupId");
            var artifactIdValue = getTextContent(dependencyElement, "artifactId");
            var versionValue = getTextContent(dependencyElement, "version");

            if (versionValue == null || versionValue.isEmpty() || isPlaceholder(versionValue)) {
                versionValue = getLatestStableVersion(groupIdValue, artifactIdValue);
            }

            var subDependency = new Dependency(groupIdValue, artifactIdValue, versionValue);
            subDependencies.add(subDependency);
        }

        return subDependencies;
    }

    @SneakyThrows
    public String getLatestStableVersion(String groupId, String artifactId) {
        var document = fetchMetadataXmlDocument(groupId, artifactId);

        var versionsElement = (Element) document.getElementsByTagName("versions").item(0);
        var versionNodes = versionsElement.getElementsByTagName("version");

        String latestStableVersion = null;
        for (int i = 0; i < versionNodes.getLength(); i++) {
            String version = versionNodes.item(i).getTextContent();
            if (isStableVersion(version)) {
                latestStableVersion = version;
            }
        }

        if (latestStableVersion != null) {
            return latestStableVersion;
        }

        throw new IllegalStateException("Unable to determine latest stable version for " + groupId + ":" + artifactId);
    }

    @SneakyThrows
    private Document fetchPomXmlDocument(String groupId, String artifactId, String version) {
        var url = new URL(getPomUrl(groupId, artifactId, version));
        return fetchXmlDocument(url);
    }

    @SneakyThrows
    private Document fetchMetadataXmlDocument(String groupId, String artifactId) {
        var metadataUrl = new URL(String.format("https://repo1.maven.org/maven2/%s/%s/maven-metadata.xml", groupId.replace(".", "/"), artifactId));
        return fetchXmlDocument(metadataUrl);
    }

    @SneakyThrows
    private Document fetchXmlDocument(URL url) {
        var connection = (HttpURLConnection) url.openConnection();
        try (var inputStream = connection.getInputStream()) {
            var factory = DocumentBuilderFactory.newInstance();
            var builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(inputStream));
        }
    }

    private boolean isPlaceholder(String versionValue) {
        return versionValue.startsWith("${") && versionValue.endsWith("}");
    }

    private boolean isStableVersion(String version) {
        return !(version.contains("alpha") || version.contains("beta"));
    }

    private String getTextContent(Element element, String tagName) {
        var nodeList = element.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }
}
