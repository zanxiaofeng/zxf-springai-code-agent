package com.codeinsight.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.codeinsight")
@EnableJpaRepositories("com.codeinsight.model.repository")
@EntityScan("com.codeinsight.model.entity")
@EnableScheduling
public class CodeInsightApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeInsightApplication.class, args);
    }
}
