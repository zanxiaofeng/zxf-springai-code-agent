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
public class ParsedField {

    private String name;
    private String type;
    private List<String> annotations;
    private boolean isStatic;
    private boolean isFinal;
}
