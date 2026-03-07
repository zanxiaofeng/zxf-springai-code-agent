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
public class ParsedClass {

    private String filePath;
    private String packageName;
    private String className;
    private String qualifiedName;
    private String classType;
    private List<String> annotations;
    private List<String> implementedInterfaces;
    private String superClass;
    private List<ParsedMethod> methods;
    private List<ParsedField> fields;
    private List<String> imports;
}
