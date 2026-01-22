package com.fileconverter.converter.impl;

import com.fileconverter.converter.Converter;
import com.fileconverter.converter.ProcessExecutor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@Component
public class PandocConverter implements Converter {

    private static final Set<String> INPUT_TYPES = Set.of(
            "text/markdown", "text/html", "text/plain",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private static final Set<String> OUTPUT_FORMATS = Set.of(
            "html", "pdf", "docx", "md", "txt", "rst", "epub");

    private final ProcessExecutor executor;

    public PandocConverter(ProcessExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void convert(Path input, Path output, String targetFormat,
            Consumer<Double> progressCallback) throws Exception {

        List<String> command = new ArrayList<>();
        command.add("pandoc");
        command.add(input.toString());
        command.add("-o");
        command.add(output.toString());

        // PDF requires a PDF engine
        if ("pdf".equals(targetFormat)) {
            command.add("--pdf-engine=pdflatex");
        }

        progressCallback.accept(0.3);

        int exitCode = executor.execute(command, input.getParent(), line -> {
        });

        progressCallback.accept(1.0);

        if (exitCode != 0) {
            throw new RuntimeException("Pandoc conversion failed with exit code: " + exitCode);
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
