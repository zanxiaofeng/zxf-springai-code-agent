package com.codeinsight.parser.chunker;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class TokenEstimator {

    private static final double CHARS_PER_TOKEN = 4.0;

    public int estimate(String text) {
        return StringUtils.isEmpty(text) ? 0 : (int) Math.ceil(text.length() / CHARS_PER_TOKEN);
    }
}
