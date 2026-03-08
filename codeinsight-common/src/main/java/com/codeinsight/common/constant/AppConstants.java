package com.codeinsight.common.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AppConstants {

    public static final String API_PREFIX = "/api/v1";
    public static final String AUTH_PREFIX = API_PREFIX + "/auth";
    public static final String PROJECTS_PREFIX = API_PREFIX + "/projects";
    public static final String CHAT_PREFIX = API_PREFIX + "/chat";
    public static final String TASKS_PREFIX = API_PREFIX + "/tasks";

    public static final String REDIS_STREAM_INDEXING = "codeinsight:task:indexing";
    public static final String REDIS_CONSUMER_GROUP = "indexing-group";

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    public static final int MAX_CHUNK_TOKENS = 512;
    public static final int OVERLAP_LINES = 3;
}
