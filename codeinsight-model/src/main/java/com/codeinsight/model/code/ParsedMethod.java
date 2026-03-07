package com.codeinsight.model.code;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedMethod {

    private String name;
    private String returnType;
    private List<MethodParam> parameters;
    private List<String> annotations;
    private List<String> calledMethods;
    private int startLine;
    private int endLine;
    private String sourceCode;
    private int complexity;
}
