package com.fileconverter.converter;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;

public interface Converter {

    /**
     * Convert a file from one format to another.
     * 
     * @param input            Source file path
     * @param output           Destination file path
     * @param targetFormat     Target format extension
     * @param progressCallback Callback for progress updates (0.0 to 1.0)
     * @throws Exception if conversion fails
     */
    void convert(Path input, Path output, String targetFormat,
            Consumer<Double> progressCallback) throws Exception;

    /**
     * @return Set of MIME types this converter can handle as input
     */
    Set<String> getSupportedInputTypes();

    /**
     * @return Set of output format extensions this converter can produce
     */
    Set<String> getSupportedOutputFormats();

    /**
     * @return true if this converter can handle the given input/output combination
     */
    default boolean supports(String inputMimeType, String outputFormat) {
        return getSupportedInputTypes().contains(inputMimeType) &&
                getSupportedOutputFormats().contains(outputFormat);
    }
}
