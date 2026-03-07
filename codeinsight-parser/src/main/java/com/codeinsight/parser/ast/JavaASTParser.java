package com.codeinsight.parser.ast;

import com.codeinsight.model.code.*;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class JavaASTParser {

    private final JavaParser parser = new JavaParser();

    public Optional<ParsedClass> parse(Path filePath) {
        try {
            String source = Files.readString(filePath);
            return parse(filePath.toString(), source);
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
                .map(pd -> pd.getNameAsString())
                .orElse("");

        List<String> imports = cu.getImports().stream()
                .map(i -> i.getNameAsString())
                .toList();

        Optional<TypeDeclaration<?>> primaryType = cu.getTypes().stream()
                .filter(t -> t instanceof ClassOrInterfaceDeclaration || t instanceof EnumDeclaration || t instanceof RecordDeclaration)
                .findFirst();

        if (primaryType.isEmpty()) {
            return Optional.empty();
        }

        TypeDeclaration<?> type = primaryType.get();
        String className = type.getNameAsString();
        String qualifiedName = packageName.isEmpty() ? className : packageName + "." + className;

        ParsedClass.ParsedClassBuilder builder = ParsedClass.builder()
                .filePath(filePath)
                .packageName(packageName)
                .className(className)
                .qualifiedName(qualifiedName)
                .imports(imports)
                .annotations(type.getAnnotations().stream().map(a -> a.getNameAsString()).toList());

        if (type instanceof ClassOrInterfaceDeclaration cid) {
            builder.classType(cid.isInterface() ? "INTERFACE" : "CLASS");
            builder.superClass(cid.getExtendedTypes().stream().findFirst().map(t -> t.getNameAsString()).orElse(null));
            builder.implementedInterfaces(cid.getImplementedTypes().stream().map(t -> t.getNameAsString()).toList());
        } else if (type instanceof EnumDeclaration) {
            builder.classType("ENUM");
        } else if (type instanceof RecordDeclaration) {
            builder.classType("RECORD");
        }

        builder.methods(parseMethods(type));
        builder.fields(parseFields(type));

        return Optional.of(builder.build());
    }

    private List<ParsedMethod> parseMethods(TypeDeclaration<?> type) {
        List<ParsedMethod> methods = new ArrayList<>();

        type.getMethods().forEach(method -> {
            List<String> calledMethods = new ArrayList<>();
            method.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(MethodCallExpr n, Void arg) {
                    calledMethods.add(n.getNameAsString());
                    super.visit(n, arg);
                }
            }, null);

            methods.add(ParsedMethod.builder()
                    .name(method.getNameAsString())
                    .returnType(method.getTypeAsString())
                    .parameters(method.getParameters().stream()
                            .map(p -> new MethodParam(p.getNameAsString(), p.getTypeAsString()))
                            .toList())
                    .annotations(method.getAnnotations().stream().map(a -> a.getNameAsString()).toList())
                    .calledMethods(calledMethods)
                    .startLine(method.getBegin().map(p -> p.line).orElse(0))
                    .endLine(method.getEnd().map(p -> p.line).orElse(0))
                    .sourceCode(method.toString())
                    .complexity(calculateComplexity(method))
                    .build());
        });

        return methods;
    }

    private List<ParsedField> parseFields(TypeDeclaration<?> type) {
        List<ParsedField> fields = new ArrayList<>();

        type.getFields().forEach(field -> {
            field.getVariables().forEach(var -> {
                fields.add(ParsedField.builder()
                        .name(var.getNameAsString())
                        .type(var.getTypeAsString())
                        .annotations(field.getAnnotations().stream().map(a -> a.getNameAsString()).toList())
                        .isStatic(field.isStatic())
                        .isFinal(field.isFinal())
                        .build());
            });
        });

        return fields;
    }

    private int calculateComplexity(MethodDeclaration method) {
        int[] complexity = {1};

        method.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(IfStmt n, Void arg) {
                complexity[0]++;
                super.visit(n, arg);
            }

            @Override
            public void visit(ForStmt n, Void arg) {
                complexity[0]++;
                super.visit(n, arg);
            }

            @Override
            public void visit(ForEachStmt n, Void arg) {
                complexity[0]++;
                super.visit(n, arg);
            }

            @Override
            public void visit(WhileStmt n, Void arg) {
                complexity[0]++;
                super.visit(n, arg);
            }

            @Override
            public void visit(DoStmt n, Void arg) {
                complexity[0]++;
                super.visit(n, arg);
            }

            @Override
            public void visit(SwitchEntry n, Void arg) {
                if (!n.getLabels().isEmpty()) {
                    complexity[0]++;
                }
                super.visit(n, arg);
            }

            @Override
            public void visit(CatchClause n, Void arg) {
                complexity[0]++;
                super.visit(n, arg);
            }
        }, null);

        return complexity[0];
    }
}
