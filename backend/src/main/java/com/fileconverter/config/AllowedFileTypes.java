package com.fileconverter.config;

import java.util.Set;

public final class AllowedFileTypes {

    private AllowedFileTypes() {
    }

    // Image types
    public static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "image/bmp", "image/tiff", "image/svg+xml");

    // Document types
    public static final Set<String> DOCUMENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.oasis.opendocument.text",
            "application/vnd.oasis.opendocument.spreadsheet",
            "application/vnd.oasis.opendocument.presentation",
            "application/x-tika-ooxml",
            "application/x-tika-msoffice",
            "text/plain", "text/html", "text/markdown", "text/csv");

    // Audio/Video types
    public static final Set<String> MEDIA_TYPES = Set.of(
            "video/mp4", "video/webm", "video/avi", "video/quicktime",
            "video/x-msvideo", "video/x-matroska",
            "audio/mpeg", "audio/wav", "audio/ogg", "audio/flac",
            "audio/aac", "audio/mp4");

    // All allowed types
    public static final Set<String> ALL_ALLOWED;

    static {
        var all = new java.util.HashSet<String>();
        all.addAll(IMAGE_TYPES);
        all.addAll(DOCUMENT_TYPES);
        all.addAll(MEDIA_TYPES);
        ALL_ALLOWED = Set.copyOf(all);
    }

    public static boolean isAllowed(String mimeType) {
        return mimeType != null && ALL_ALLOWED.contains(mimeType.toLowerCase());
    }

    public static String getCategory(String mimeType) {
        if (mimeType == null)
            return "unknown";
        String lower = mimeType.toLowerCase();
        if (IMAGE_TYPES.contains(lower))
            return "image";
        if (DOCUMENT_TYPES.contains(lower))
            return "document";
        if (MEDIA_TYPES.contains(lower))
            return "media";
        return "unknown";
    }
}
