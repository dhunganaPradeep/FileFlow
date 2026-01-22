package com.fileconverter.converter.impl;

import com.fileconverter.converter.Converter;
import com.fileconverter.converter.ProcessExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Component
public class LibreOfficeConverter implements Converter {

    private static final Logger log = LoggerFactory.getLogger(LibreOfficeConverter.class);

    private static final Set<String> INPUT_TYPES = Set.of(
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.oasis.opendocument.text",
            "application/vnd.oasis.opendocument.spreadsheet",
            "application/vnd.oasis.opendocument.presentation",
            "application/pdf",
            "application/x-tika-ooxml",
            "application/x-tika-msoffice",
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp", "image/tiff",
            "text/plain", "text/html", "text/csv");

    private static final Set<String> OUTPUT_FORMATS = Set.of(
            "pdf", "docx", "doc", "xlsx", "xls", "pptx", "ppt",
            "odt", "ods", "odp", "txt", "html", "csv");

    private final ProcessExecutor executor;

    public LibreOfficeConverter(ProcessExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void convert(Path input, Path output, String targetFormat,
            Consumer<Double> progressCallback) throws Exception {

        Path tempDir = Files.createTempDirectory("libreoffice-");

        try {
            List<String> command = new ArrayList<>();
            command.add("soffice");
            command.add("--headless");

            if (input.toString().toLowerCase().endsWith(".pdf")) {
                command.add("--infilter=writer_pdf_import");
                command.add("--convert-to");
                command.add("docx");
            } else {
                command.add("--convert-to");
                command.add(targetFormat);
            }

            command.add("--outdir");
            command.add(tempDir.toString());
            command.add(input.toString());

            progressCallback.accept(0.2);

            int exitCode = executor.execute(command, input.getParent(), line -> {
            });

            if (exitCode != 0) {
                throw new RuntimeException("LibreOffice conversion failed with exit code: " + exitCode);
            }

            progressCallback.accept(0.8);

            // Find output file in temp dir
            String inputFileName = input.getFileName().toString();
            String baseName = inputFileName.contains(".")
                    ? inputFileName.substring(0, inputFileName.lastIndexOf('.'))
                    : inputFileName;
            Path convertedFile = tempDir.resolve(baseName + "." + targetFormat);

            if (!Files.exists(convertedFile)) {
                log.warn("Converted file not found at expected path: {}. Searching directory...", convertedFile);
                try (var stream = Files.list(tempDir)) {
                    List<Path> files = stream.filter(Files::isRegularFile).toList();
                    if (files.isEmpty()) {
                        throw new RuntimeException("LibreOffice produced no output files in " + tempDir);
                    }
                    if (files.size() == 1) {
                        convertedFile = files.get(0);
                        log.info("Found single output file, using it: {}", convertedFile);
                    } else {
                        // Try to find a file with the correct extension
                        convertedFile = files.stream()
                                .filter(p -> p.toString().endsWith("." + targetFormat))
                                .findFirst()
                                .orElseThrow(() -> new RuntimeException(
                                        "Multiple files found but none match type " + targetFormat));
                        log.info("Found multiple files, selected best match: {}", convertedFile);
                    }
                }
            }

            Files.move(convertedFile, output);
            progressCallback.accept(1.0);

        } finally {
            // Cleanup temp directory
            try {
                Files.walk(tempDir)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (Exception ignored) {
                            }
                        });
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public Set<String> getSupportedInputTypes() {
        return INPUT_TYPES;
    }

    @Override
    public Set<String> getSupportedOutputFormats() {
        return OUTPUT_FORMATS;
    }
}
