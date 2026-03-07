package com.codeinsight.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.codeinsight")
@EnableScheduling
public class CodeInsightApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeInsightApplication.class, args);
    }
}
