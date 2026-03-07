package com.codeinsight.model.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public record ChatEvent(
        String type,
        Map<String, Object> data
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static ChatEvent metadata(Map<String, Object> data) {
        return new ChatEvent("metadata", data);
    }

    public static ChatEvent content(String text) {
        return new ChatEvent("content", Map.of("text", text));
    }

    public static ChatEvent done(Map<String, Object> data) {
        return new ChatEvent("done", data);
    }

    public static ChatEvent error(String message) {
        return new ChatEvent("error", Map.of("message", message));
    }

    public String toJson() {
        try {
            return MAPPER.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
