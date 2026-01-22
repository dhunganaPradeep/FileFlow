package com.fileconverter.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ConverterRegistry {

    private static final Logger log = LoggerFactory.getLogger(ConverterRegistry.class);

    private final List<Converter> converters;

    public ConverterRegistry(List<Converter> converters) {
        this.converters = converters;
        log.info("Registered {} converters", converters.size());
        converters.forEach(c -> log.info("  - {}: {} -> {}",
                c.getClass().getSimpleName(),
                c.getSupportedInputTypes(),
                c.getSupportedOutputFormats()));
    }

    public Converter getConverter(String inputMimeType, String outputFormat) {
        return converters.stream()
                .filter(c -> c.supports(inputMimeType, outputFormat))
                .findFirst()
                .orElse(null);
    }

    public Optional<Converter> findConverter(String inputMimeType, String outputFormat) {
        return Optional.ofNullable(getConverter(inputMimeType, outputFormat));
    }

    public List<String> getSupportedOutputFormats(String inputMimeType) {
        return converters.stream()
                .filter(c -> c.getSupportedInputTypes().contains(inputMimeType))
                .flatMap(c -> c.getSupportedOutputFormats().stream())
                .distinct()
                .sorted()
                .toList();
    }

    public boolean isConversionSupported(String inputMimeType, String outputFormat) {
        return getConverter(inputMimeType, outputFormat) != null;
    }
}
