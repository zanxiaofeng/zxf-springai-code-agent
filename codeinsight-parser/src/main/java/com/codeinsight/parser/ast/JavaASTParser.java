package com.codeinsight.parser.ast;

import com.codeinsight.model.code.*;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class JavaASTParser {

    private final JavaParser parser = new JavaParser();

    public Optional<ParsedClass> parse(Path filePath) {
        try {
            return parse(filePath.toString(), Files.readString(filePath));
        } catch (IOException e) {
            log.warn("Failed to read file: {}", filePath, e);
            return Optional.empty();
        }
    }

    public Optional<ParsedClass> parse(String filePath, String source) {
        ParseResult<CompilationUnit> result = parser.parse(source);
        if (!result.isSuccessful() || result.getResult().isEmpty()) {
            log.warn("Failed to parse: {}", filePath);
            return Optional.empty();
        }

        CompilationUnit cu = result.getResult().get();
        String packageName = cu.getPackageDeclaration()
                .map(PackageDeclaration::getNameAsString)
                .orElse("");

        List<String> imports = cu.getImports().stream()
                .map(id -> id.getNameAsString())
                .toList();

        return cu.getTypes().stream()
                .filter(t -> t instanceof ClassOrInterfaceDeclaration || t instanceof EnumDeclaration || t instanceof RecordDeclaration)
                .findFirst()
                .map(type -> buildParsedClass(type, filePath, packageName, imports));
    }

    private ParsedClass buildParsedClass(TypeDeclaration<?> type, String filePath, String packageName, List<String> imports) {
        String className = type.getNameAsString();
        String qualifiedName = StringUtils.isEmpty(packageName) ? className : "%s.%s".formatted(packageName, className);

        var builder = ParsedClass.builder()
                .filePath(filePath)
                .packageName(packageName)
                .className(className)
                .qualifiedName(qualifiedName)
                .imports(imports)
                .annotations(annotationNames(type))
                .methods(parseMethods(type))
                .fields(parseFields(type));

        if (type instanceof ClassOrInterfaceDeclaration cid) {
            builder.classType(cid.isInterface() ? "INTERFACE" : "CLASS")
                    .superClass(cid.getExtendedTypes().stream().findFirst().map(t -> t.getNameAsString()).orElse(null))
                    .implementedInterfaces(cid.getImplementedTypes().stream().map(t -> t.getNameAsString()).toList());
        } else if (type instanceof EnumDeclaration) {
            builder.classType("ENUM");
        } else if (type instanceof RecordDeclaration) {
            builder.classType("RECORD");
        }

        return builder.build();
    }

    private List<ParsedMethod> parseMethods(TypeDeclaration<?> type) {
        return type.getMethods().stream().map(method -> {
            List<String> calledMethods = new ArrayList<>();
            method.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(MethodCallExpr n, Void arg) {
                    calledMethods.add(n.getNameAsString());
                    super.visit(n, arg);
                }
            }, null);

            return ParsedMethod.builder()
                    .name(method.getNameAsString())
                    .returnType(method.getTypeAsString())
                    .parameters(method.getParameters().stream()
                            .map(p -> new MethodParam(p.getNameAsString(), p.getTypeAsString()))
                            .toList())
                    .annotations(annotationNames(method))
                    .calledMethods(calledMethods)
                    .startLine(method.getBegin().map(p -> p.line).orElse(0))
                    .endLine(method.getEnd().map(p -> p.line).orElse(0))
                    .sourceCode(method.toString())
                    .complexity(calculateComplexity(method))
                    .build();
        }).toList();
    }

    private List<ParsedField> parseFields(TypeDeclaration<?> type) {
        return type.getFields().stream()
                .flatMap(field -> field.getVariables().stream().map(var ->
                        ParsedField.builder()
                                .name(var.getNameAsString())
                                .type(var.getTypeAsString())
                                .annotations(annotationNames(field))
                                .isStatic(field.isStatic())
                                .isFinal(field.isFinal())
                                .build()))
                .toList();
    }

    private List<String> annotationNames(NodeWithAnnotations<?> node) {
        return node.getAnnotations().stream().map(AnnotationExpr::getNameAsString).toList();
    }

    private int calculateComplexity(MethodDeclaration method) {
        AtomicInteger complexity = new AtomicInteger(1);

        method.accept(new VoidVisitorAdapter<Void>() {
            @Override public void visit(IfStmt n, Void arg) { complexity.incrementAndGet(); super.visit(n, arg); }
            @Override public void visit(ForStmt n, Void arg) { complexity.incrementAndGet(); super.visit(n, arg); }
            @Override public void visit(ForEachStmt n, Void arg) { complexity.incrementAndGet(); super.visit(n, arg); }
            @Override public void visit(WhileStmt n, Void arg) { complexity.incrementAndGet(); super.visit(n, arg); }
            @Override public void visit(DoStmt n, Void arg) { complexity.incrementAndGet(); super.visit(n, arg); }
            @Override public void visit(CatchClause n, Void arg) { complexity.incrementAndGet(); super.visit(n, arg); }
            @Override public void visit(SwitchEntry n, Void arg) {
                if (!n.getLabels().isEmpty()) complexity.incrementAndGet();
                super.visit(n, arg);
            }
        }, null);

        return complexity.get();
    }
}
