package com.codeinsight.agent.tool;

import com.codeinsight.model.code.ParsedClass;
import com.codeinsight.model.code.ParsedMethod;
import com.codeinsight.parser.ast.JavaASTParser;
import com.codeinsight.parser.scanner.JavaFileScanner;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ASTAnalyzeTool {

    private final JavaASTParser astParser;
    private final JavaFileScanner fileScanner;

    @Value("${codeinsight.repo.base-path:./data/repos}")
    private String basePath;

    @Tool(description = "Analyze the structure of a Java class by its qualified name. Returns fields, methods, annotations, inheritance, and complexity metrics.")
    public String analyzeClass(
            @ToolParam(description = "Qualified class name, e.g. com.example.OrderService") String qualifiedClassName,
            @ToolParam(description = "Project ID") String projectId) {

        Path projectPath = resolveProjectPath(projectId);
        String relativePath = qualifiedClassName.replace('.', '/') + ".java";

        List<Path> candidates = scanProject(projectPath).stream()
                .filter(p -> p.toString().endsWith(relativePath))
                .toList();

        if (candidates.isEmpty()) {
            return "Class not found: " + qualifiedClassName;
        }

        Optional<ParsedClass> parsedOpt = astParser.parse(candidates.getFirst());
        if (parsedOpt.isEmpty()) {
            return "Failed to parse: " + qualifiedClassName;
        }

        return formatClassAnalysis(parsedOpt.get());
    }

    @Tool(description = "Get the call chain of a method - which methods it calls and its complexity.")
    public String getMethodInfo(
            @ToolParam(description = "Method name") String methodName,
            @ToolParam(description = "Qualified class name") String qualifiedClassName,
            @ToolParam(description = "Project ID") String projectId) {

        Path projectPath = resolveProjectPath(projectId);
        String relativePath = qualifiedClassName.replace('.', '/') + ".java";

        List<Path> candidates = scanProject(projectPath).stream()
                .filter(p -> p.toString().endsWith(relativePath))
                .toList();

        if (candidates.isEmpty()) {
            return "Class not found: " + qualifiedClassName;
        }

        Optional<ParsedClass> parsedOpt = astParser.parse(candidates.getFirst());
        if (parsedOpt.isEmpty()) {
            return "Failed to parse: " + qualifiedClassName;
        }

        return parsedOpt.get().getMethods().stream()
                .filter(m -> m.getName().equals(methodName))
                .findFirst()
                .map(this::formatMethodDetail)
                .orElse("Method not found: " + methodName + " in " + qualifiedClassName);
    }

    private String formatClassAnalysis(ParsedClass cls) {
        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(cls.getQualifiedName()).append("\n");
        sb.append("- Type: ").append(cls.getClassType()).append("\n");
        sb.append("- Package: ").append(cls.getPackageName()).append("\n");
        if (cls.getSuperClass() != null) {
            sb.append("- Extends: ").append(cls.getSuperClass()).append("\n");
        }
        if (cls.getImplementedInterfaces() != null && !cls.getImplementedInterfaces().isEmpty()) {
            sb.append("- Implements: ").append(String.join(", ", cls.getImplementedInterfaces())).append("\n");
        }
        if (cls.getAnnotations() != null && !cls.getAnnotations().isEmpty()) {
            sb.append("- Annotations: ").append(String.join(", ", cls.getAnnotations())).append("\n");
        }

        sb.append("\n### Fields (").append(cls.getFields().size()).append(")\n");
        cls.getFields().forEach(f ->
                sb.append("- ").append(f.getType()).append(" ").append(f.getName())
                        .append(f.isStatic() ? " [static]" : "")
                        .append(f.isFinal() ? " [final]" : "").append("\n"));

        sb.append("\n### Methods (").append(cls.getMethods().size()).append(")\n");
        cls.getMethods().forEach(m ->
                sb.append("- ").append(m.getReturnType()).append(" ").append(m.getName())
                        .append("() [complexity=").append(m.getComplexity())
                        .append(", lines ").append(m.getStartLine()).append("-").append(m.getEndLine())
                        .append("]\n"));

        return sb.toString();
    }

    private Path resolveProjectPath(String projectId) {
        var resolved = Path.of(basePath, projectId).normalize();
        if (!resolved.startsWith(Path.of(basePath).normalize())) {
            throw new IllegalArgumentException("Invalid project ID: " + projectId);
        }
        return resolved;
    }

    private List<Path> scanProject(Path projectPath) {
        try {
            return fileScanner.scan(projectPath);
        } catch (java.io.IOException e) {
            log.error("Failed to scan project directory: {}", projectPath, e);
            return List.of();
        }
    }

    private String formatMethodDetail(ParsedMethod method) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Method: ").append(method.getName()).append("\n");
        sb.append("- Return type: ").append(method.getReturnType()).append("\n");
        sb.append("- Lines: ").append(method.getStartLine()).append("-").append(method.getEndLine()).append("\n");
        sb.append("- Cyclomatic complexity: ").append(method.getComplexity()).append("\n");
        if (method.getAnnotations() != null && !method.getAnnotations().isEmpty()) {
            sb.append("- Annotations: ").append(String.join(", ", method.getAnnotations())).append("\n");
        }
        if (method.getCalledMethods() != null && !method.getCalledMethods().isEmpty()) {
            sb.append("- Calls: ").append(String.join(", ", method.getCalledMethods())).append("\n");
        }
        sb.append("\n```java\n").append(method.getSourceCode()).append("\n```\n");
        return sb.toString();
    }
}
