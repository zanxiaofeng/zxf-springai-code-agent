package com.codeinsight.parser.git;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Slf4j
public class ArchiveExtractService {

    @Value("${codeinsight.repo.base-path:./data/repos}")
    private String basePath;

    public Path extractZip(InputStream inputStream, String projectId) throws IOException {
        Path targetDir = Path.of(basePath, projectId);
        Files.createDirectories(targetDir);

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(inputStream))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName()).normalize();
                if (!entryPath.startsWith(targetDir)) {
                    throw new IOException("Zip entry outside target directory: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (OutputStream os = Files.newOutputStream(entryPath)) {
                        zis.transferTo(os);
                    }
                }
                zis.closeEntry();
            }
        }

        log.info("Extracted archive to {}", targetDir);
        return targetDir;
    }
}
