package com.codeinsight.parser.git;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@Slf4j
public class ArchiveExtractService {

    @Value("${codeinsight.repo.base-path:./data/repos}")
    private String basePath;

    public Path extract(InputStream inputStream, String projectId, String filename) throws IOException {
        String ext = FilenameUtils.getExtension(filename).toLowerCase();
        return switch (ext) {
            case "gz", "tgz" -> extractTarGz(inputStream, projectId);
            default -> extractZip(inputStream, projectId);
        };
    }

    public Path extractZip(InputStream inputStream, String projectId) throws IOException {
        Path targetDir = resolveTargetDir(projectId);

        try (var zis = new ZipInputStream(new BufferedInputStream(inputStream))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = resolveAndValidate(targetDir, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    writeFile(entryPath, zis);
                }
                zis.closeEntry();
            }
        }

        log.info("Extracted ZIP archive to {}", targetDir);
        return targetDir;
    }

    public Path extractTarGz(InputStream inputStream, String projectId) throws IOException {
        Path targetDir = resolveTargetDir(projectId);

        try (var gzis = new GZIPInputStream(new BufferedInputStream(inputStream));
             var tais = new TarArchiveInputStream(gzis)) {
            TarArchiveEntry entry;
            while ((entry = tais.getNextEntry()) != null) {
                Path entryPath = resolveAndValidate(targetDir, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    writeFile(entryPath, tais);
                }
            }
        }

        log.info("Extracted TAR.GZ archive to {}", targetDir);
        return targetDir;
    }

    private Path resolveTargetDir(String projectId) throws IOException {
        Path targetDir = Path.of(basePath, projectId);
        Files.createDirectories(targetDir);
        return targetDir;
    }

    private Path resolveAndValidate(Path targetDir, String entryName) throws IOException {
        Path entryPath = targetDir.resolve(entryName).normalize();
        if (!entryPath.startsWith(targetDir)) {
            throw new IOException("Archive entry outside target directory: " + entryName);
        }
        return entryPath;
    }

    private void writeFile(Path path, InputStream source) throws IOException {
        Files.createDirectories(path.getParent());
        try (var os = Files.newOutputStream(path)) {
            source.transferTo(os);
        }
    }
}
