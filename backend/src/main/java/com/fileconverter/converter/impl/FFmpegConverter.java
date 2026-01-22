package com.fileconverter.converter.impl;

import com.fileconverter.converter.Converter;
import com.fileconverter.converter.ProcessExecutor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FFmpegConverter implements Converter {

    private static final Set<String> INPUT_TYPES = Set.of(
            "video/mp4", "video/webm", "video/avi", "video/quicktime",
            "video/x-msvideo", "video/x-matroska",
            "audio/mpeg", "audio/wav", "audio/ogg", "audio/flac",
            "audio/aac", "audio/mp4");

    private static final Set<String> OUTPUT_FORMATS = Set.of(
            "mp4", "webm", "avi", "mkv", "mov",
            "mp3", "wav", "ogg", "flac", "aac", "m4a");

    private static final Pattern PROGRESS_PATTERN = Pattern.compile("time=([0-9:.]+)");

    private final ProcessExecutor executor;

    public FFmpegConverter(ProcessExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void convert(Path input, Path output, String targetFormat,
            Consumer<Double> progressCallback) throws Exception {

        List<String> command = new ArrayList<>();
        command.add("ffmpeg");
        command.add("-y"); // Overwrite output
        command.add("-i");
        command.add(input.toString());

        // Format-specific settings
        switch (targetFormat.toLowerCase()) {
            case "mp4" -> {
                command.add("-c:v");
                command.add("libx264");
                command.add("-preset");
                command.add("fast");
                command.add("-crf");
                command.add("23");
                command.add("-c:a");
                command.add("aac");
            }
            case "webm" -> {
                command.add("-c:v");
                command.add("libvpx-vp9");
                command.add("-crf");
                command.add("30");
                command.add("-c:a");
                command.add("libopus");
            }
            case "mp3" -> {
                command.add("-vn");
                command.add("-c:a");
                command.add("libmp3lame");
                command.add("-q:a");
                command.add("2");
            }
            case "wav" -> {
                command.add("-vn");
                command.add("-c:a");
                command.add("pcm_s16le");
            }
        }

        command.add("-progress");
        command.add("pipe:1");
        command.add(output.toString());

        int exitCode = executor.execute(command, input.getParent(), line -> {
            Matcher matcher = PROGRESS_PATTERN.matcher(line);
            if (matcher.find()) {
                // Parse time and estimate progress (simplified)
                progressCallback.accept(0.5);
            }
        });

        progressCallback.accept(1.0);

        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg conversion failed with exit code: " + exitCode);
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
