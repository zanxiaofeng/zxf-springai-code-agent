package com.codeinsight.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Component
@Slf4j
public class DependencyTreeTool {

    @Value("${codeinsight.repo.base-path:./data/repos}")
    private String basePath;

    @Tool(description = "Parse the Maven pom.xml of a project and return all declared dependencies with groupId, artifactId, and version.")
    public String parseDependencies(
            @ToolParam(description = "Project ID") String projectId) {

        Path pomPath = Path.of(basePath, projectId, "pom.xml");
        if (!Files.exists(pomPath)) {
            return "No pom.xml found in project " + projectId;
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(pomPath.toString()));

            List<String> deps = new ArrayList<>();
            NodeList depNodes = doc.getElementsByTagName("dependency");

            for (int i = 0; i < depNodes.getLength(); i++) {
                Element dep = (Element) depNodes.item(i);
                String groupId = getTagText(dep, "groupId");
                String artifactId = getTagText(dep, "artifactId");
                String version = getTagText(dep, "version");
                String scope = getTagText(dep, "scope");

                StringBuilder sb = new StringBuilder();
                sb.append("- ").append(groupId).append(":").append(artifactId);
                if (version != null && !version.isEmpty()) {
                    sb.append(":").append(version);
                }
                if (scope != null && !scope.isEmpty()) {
                    sb.append(" [").append(scope).append("]");
                }
                deps.add(sb.toString());
            }

            if (deps.isEmpty()) {
                return "No dependencies declared in pom.xml";
            }

            return "## Dependencies for project " + projectId + "\n\n" + String.join("\n", deps);
        } catch (Exception e) {
            log.error("Failed to parse pom.xml for project {}: {}", projectId, e.getMessage());
            return "Failed to parse pom.xml: " + e.getMessage();
        }
    }

    private String getTagText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return null;
    }
}
