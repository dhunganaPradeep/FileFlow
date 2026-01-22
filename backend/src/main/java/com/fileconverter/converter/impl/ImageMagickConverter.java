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
public class ImageMagickConverter implements Converter {

    private static final Set<String> INPUT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "image/bmp", "image/tiff");

    private static final Set<String> OUTPUT_FORMATS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "tiff");

    private final ProcessExecutor executor;

    public ImageMagickConverter(ProcessExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void convert(Path input, Path output, String targetFormat,
            Consumer<Double> progressCallback) throws Exception {

        List<String> command = new ArrayList<>();
        command.add("convert");
        command.add(input.toString());

        // Quality settings based on format
        switch (targetFormat.toLowerCase()) {
            case "jpg", "jpeg" -> {
                command.add("-quality");
                command.add("92");
            }
            case "png" -> {
                command.add("-quality");
                command.add("95");
            }
            case "webp" -> {
                command.add("-quality");
                command.add("90");
            }
        }

        command.add(output.toString());

        progressCallback.accept(0.3);

        int exitCode = executor.execute(command, input.getParent(), line -> {
            // ImageMagick doesn't output progress, but we can simulate
        });

        progressCallback.accept(1.0);

        if (exitCode != 0) {
            throw new RuntimeException("ImageMagick conversion failed with exit code: " + exitCode);
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
