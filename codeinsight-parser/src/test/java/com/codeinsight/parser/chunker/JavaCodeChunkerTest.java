package com.codeinsight.parser.chunker;

import com.codeinsight.model.code.CodeChunk;
import com.codeinsight.model.code.ParsedClass;
import com.codeinsight.model.code.ParsedField;
import com.codeinsight.model.code.ParsedMethod;
import com.codeinsight.model.enums.ChunkType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JavaCodeChunkerTest {

    private JavaCodeChunker chunker;

    @BeforeEach
    void setUp() {
        chunker = new JavaCodeChunker(new TokenEstimator());
    }

    @Test
    void shouldCreateClassAndMethodChunks() {
        ParsedClass parsedClass = ParsedClass.builder()
                .packageName("com.example")
                .className("UserService")
                .qualifiedName("com.example.UserService")
                .classType("CLASS")
                .annotations(List.of("Service"))
                .implementedInterfaces(List.of())
                .fields(List.of(
                        ParsedField.builder().name("repo").type("UserRepo").annotations(List.of()).build()
                ))
                .methods(List.of(
                        ParsedMethod.builder()
                                .name("findAll")
                                .returnType("List")
                                .annotations(List.of("Override"))
                                .calledMethods(List.of("findAll"))
                                .startLine(10)
                                .endLine(15)
                                .sourceCode("public List findAll() {\n    return repo.findAll();\n}")
                                .complexity(1)
                                .parameters(List.of())
                                .build()
                ))
                .imports(List.of())
                .build();

        List<CodeChunk> chunks = chunker.chunkJavaFile("UserService.java", "", parsedClass);

        assertThat(chunks).hasSize(2);

        CodeChunk classChunk = chunks.getFirst();
        assertThat(classChunk.getChunkType()).isEqualTo(ChunkType.CLASS);
        assertThat(classChunk.getClassName()).isEqualTo("com.example.UserService");
        assertThat(classChunk.getContent()).contains("UserService");

        CodeChunk methodChunk = chunks.get(1);
        assertThat(methodChunk.getChunkType()).isEqualTo(ChunkType.METHOD);
        assertThat(methodChunk.getMethodName()).isEqualTo("findAll");
        assertThat(methodChunk.getStartLine()).isEqualTo(10);
    }

    @Test
    void shouldSplitLongMethod() {
        StringBuilder longMethod = new StringBuilder("public void longMethod() {\n");
        for (int i = 0; i < 100; i++) {
            longMethod.append("    System.out.println(\"line ").append(i).append("\");\n");
        }
        longMethod.append("}");

        ParsedClass parsedClass = ParsedClass.builder()
                .packageName("com.example")
                .className("BigClass")
                .qualifiedName("com.example.BigClass")
                .classType("CLASS")
                .annotations(List.of())
                .implementedInterfaces(List.of())
                .fields(List.of())
                .methods(List.of(
                        ParsedMethod.builder()
                                .name("longMethod")
                                .returnType("void")
                                .annotations(List.of())
                                .calledMethods(List.of())
                                .startLine(1)
                                .endLine(102)
                                .sourceCode(longMethod.toString())
                                .complexity(1)
                                .parameters(List.of())
                                .build()
                ))
                .imports(List.of())
                .build();

        List<CodeChunk> chunks = chunker.chunkJavaFile("BigClass.java", "", parsedClass);

        assertThat(chunks.size()).isGreaterThan(2); // class chunk + multiple method parts
        assertThat(chunks.stream().filter(c -> c.getChunkType() == ChunkType.METHOD_PART).count())
                .isGreaterThan(1);
    }

    @Test
    void shouldHandleEmptyMethods() {
        ParsedClass parsedClass = ParsedClass.builder()
                .packageName("com.example")
                .className("Empty")
                .qualifiedName("com.example.Empty")
                .classType("CLASS")
                .annotations(List.of())
                .implementedInterfaces(List.of())
                .fields(List.of())
                .methods(List.of())
                .imports(List.of())
                .build();

        List<CodeChunk> chunks = chunker.chunkJavaFile("Empty.java", "", parsedClass);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().getChunkType()).isEqualTo(ChunkType.CLASS);
    }
}
