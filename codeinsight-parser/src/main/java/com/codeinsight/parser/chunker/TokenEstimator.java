package com.codeinsight.parser.chunker;

import org.springframework.stereotype.Component;

@Component
public class TokenEstimator {

    private static final double CHARS_PER_TOKEN = 4.0;

    public int estimate(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / CHARS_PER_TOKEN);
    }
}
