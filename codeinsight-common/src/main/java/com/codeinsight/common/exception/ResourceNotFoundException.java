package com.codeinsight.common.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceType, String id) {
        super(resourceType + " not found: " + id);
    }
}
