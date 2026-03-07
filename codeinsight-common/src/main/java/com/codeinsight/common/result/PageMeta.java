package com.codeinsight.common.result;

public record PageMeta(
        long total,
        int page,
        int size
) {
}
