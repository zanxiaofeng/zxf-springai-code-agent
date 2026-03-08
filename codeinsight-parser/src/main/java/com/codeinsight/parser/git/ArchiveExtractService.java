package com.codeinsight.parser.git;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

@Service
@Slf4j
public class ArchiveExtractService {

    @Value("${codeinsight.repo.base-path:./data/repos}")
    private String basePath;

    public Path extract(InputStream inputStream, String projectId, String filename) throws IOException {
        if (filename.endsWith(".tar.gz") || filename.endsWith(".tgz")) {
            return extractTarGz(inputStream, projectId);
        }
        return extractZip(inputStream, projectId);
    }

    public Path extractZip(InputStream inputStream, String projectId) throws IOException {
        Path targetDir = Path.of(basePath, projectId);
        Files.createDirectories(targetDir);

        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(inputStream))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName()).normalize();
                validatePathTraversal(targetDir, entryPath, entry.getName());
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

        log.info("Extracted ZIP archive to {}", targetDir);
        return targetDir;
    }

    public Path extractTarGz(InputStream inputStream, String projectId) throws IOException {
        Path targetDir = Path.of(basePath, projectId);
        Files.createDirectories(targetDir);

        try (GZIPInputStream gzis = new GZIPInputStream(new BufferedInputStream(inputStream));
             TarArchiveInputStream tais = new TarArchiveInputStream(gzis)) {
            TarArchiveEntry entry;
            while ((entry = tais.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName()).normalize();
                validatePathTraversal(targetDir, entryPath, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (OutputStream os = Files.newOutputStream(entryPath)) {
                        tais.transferTo(os);
                    }
                }
            }
        }

        log.info("Extracted TAR.GZ archive to {}", targetDir);
        return targetDir;
    }

    private void validatePathTraversal(Path targetDir, Path entryPath, String entryName) throws IOException {
        if (!entryPath.startsWith(targetDir)) {
            throw new IOException("Archive entry outside target directory: " + entryName);
        }
    }
}
