export interface UploadResponse {
    jobId: string;
    token: string;
    fileName: string;
    mimeType: string;
    fileSize: number;
    targetFormat: string;
    createdAt: string;
    expiresAt: string;
}

export interface JobStatusResponse {
    jobId: string;
    status: 'QUEUED' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'EXPIRED';
    fileName: string;
    sourceFormat: string;
    targetFormat: string;
    progress: number;
    errorMessage: string | null;
    createdAt: string;
    completedAt: string | null;
    downloadUrl: string | null;
}

export interface ErrorResponse {
    status: number;
    error: string;
    message: string;
    path: string;
    timestamp: string;
}

export interface ConversionJob {
    id: string;
    token: string;
    fileName: string;
    sourceFormat: string;
    targetFormat: string;
    status: JobStatusResponse['status'];
    progress: number;
    error?: string;
}

export type FileCategory = 'image' | 'document' | 'media' | 'all';

export interface FormatOption {
    value: string;
    label: string;
    mimeTypes: string[];
}

// Input format options (what formats can be converted FROM)
export const INPUT_FORMATS: FormatOption[] = [
    // Images
    { value: 'image/jpeg', label: 'JPEG Image', mimeTypes: ['image/jpeg'] },
    { value: 'image/png', label: 'PNG Image', mimeTypes: ['image/png'] },
    { value: 'image/gif', label: 'GIF Image', mimeTypes: ['image/gif'] },
    { value: 'image/webp', label: 'WebP Image', mimeTypes: ['image/webp'] },
    { value: 'image/bmp', label: 'BMP Image', mimeTypes: ['image/bmp'] },
    { value: 'image/tiff', label: 'TIFF Image', mimeTypes: ['image/tiff'] },
    // Documents
    { value: 'application/pdf', label: 'PDF Document', mimeTypes: ['application/pdf'] },
    { value: 'application/msword', label: 'Word (DOC)', mimeTypes: ['application/msword'] },
    { value: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', label: 'Word (DOCX)', mimeTypes: ['application/vnd.openxmlformats-officedocument.wordprocessingml.document'] },
    { value: 'application/vnd.ms-excel', label: 'Excel (XLS)', mimeTypes: ['application/vnd.ms-excel'] },
    { value: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', label: 'Excel (XLSX)', mimeTypes: ['application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'] },
    { value: 'application/vnd.ms-powerpoint', label: 'PowerPoint (PPT)', mimeTypes: ['application/vnd.ms-powerpoint'] },
    { value: 'application/vnd.openxmlformats-officedocument.presentationml.presentation', label: 'PowerPoint (PPTX)', mimeTypes: ['application/vnd.openxmlformats-officedocument.presentationml.presentation'] },
    { value: 'text/plain', label: 'Text File', mimeTypes: ['text/plain'] },
    { value: 'text/html', label: 'HTML File', mimeTypes: ['text/html'] },
    { value: 'text/markdown', label: 'Markdown', mimeTypes: ['text/markdown'] },
    { value: 'text/csv', label: 'CSV File', mimeTypes: ['text/csv'] },
    // Media
    { value: 'video/mp4', label: 'MP4 Video', mimeTypes: ['video/mp4'] },
    { value: 'video/webm', label: 'WebM Video', mimeTypes: ['video/webm'] },
    { value: 'video/quicktime', label: 'MOV Video', mimeTypes: ['video/quicktime'] },
    { value: 'video/x-msvideo', label: 'AVI Video', mimeTypes: ['video/x-msvideo'] },
    { value: 'video/x-matroska', label: 'MKV Video', mimeTypes: ['video/x-matroska'] },
    { value: 'audio/mpeg', label: 'MP3 Audio', mimeTypes: ['audio/mpeg'] },
    { value: 'audio/wav', label: 'WAV Audio', mimeTypes: ['audio/wav'] },
    { value: 'audio/ogg', label: 'OGG Audio', mimeTypes: ['audio/ogg'] },
    { value: 'audio/flac', label: 'FLAC Audio', mimeTypes: ['audio/flac'] },
];

// Output format options (what formats can be converted TO)
export const OUTPUT_FORMATS: Record<string, string[]> = {
    // Images -> various image formats + PDF
    'image/jpeg': ['png', 'gif', 'webp', 'bmp', 'tiff', 'pdf'],
    'image/png': ['jpg', 'gif', 'webp', 'bmp', 'tiff', 'pdf'],
    'image/gif': ['jpg', 'png', 'webp', 'bmp', 'tiff', 'pdf'],
    'image/webp': ['jpg', 'png', 'gif', 'bmp', 'tiff', 'pdf'],
    'image/bmp': ['jpg', 'png', 'gif', 'webp', 'tiff', 'pdf'],
    'image/tiff': ['jpg', 'png', 'gif', 'webp', 'bmp', 'pdf'],
    // Documents
    'application/pdf': ['docx', 'txt', 'html'],
    'application/msword': ['pdf', 'docx', 'txt', 'html', 'odt'],
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['pdf', 'doc', 'txt', 'html', 'odt'],
    'application/vnd.ms-excel': ['pdf', 'xlsx', 'csv', 'ods'],
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': ['pdf', 'xls', 'csv', 'ods'],
    'application/vnd.ms-powerpoint': ['pdf', 'pptx', 'odp'],
    'application/vnd.openxmlformats-officedocument.presentationml.presentation': ['pdf', 'ppt', 'odp'],
    'text/plain': ['pdf', 'html', 'docx'],
    'text/html': ['pdf', 'docx', 'txt', 'md'],
    'text/markdown': ['pdf', 'html', 'docx', 'txt'],
    'text/csv': ['xlsx', 'xls', 'pdf'],
    // Video
    'video/mp4': ['webm', 'avi', 'mkv', 'mov', 'mp3', 'wav'],
    'video/webm': ['mp4', 'avi', 'mkv', 'mov', 'mp3', 'wav'],
    'video/quicktime': ['mp4', 'webm', 'avi', 'mkv', 'mp3', 'wav'],
    'video/x-msvideo': ['mp4', 'webm', 'mkv', 'mov', 'mp3', 'wav'],
    'video/x-matroska': ['mp4', 'webm', 'avi', 'mov', 'mp3', 'wav'],
    // Audio
    'audio/mpeg': ['wav', 'ogg', 'flac', 'aac', 'm4a'],
    'audio/wav': ['mp3', 'ogg', 'flac', 'aac', 'm4a'],
    'audio/ogg': ['mp3', 'wav', 'flac', 'aac', 'm4a'],
    'audio/flac': ['mp3', 'wav', 'ogg', 'aac', 'm4a'],
};

export const FORMAT_LABELS: Record<string, string> = {
    jpg: 'JPEG', jpeg: 'JPEG', png: 'PNG', gif: 'GIF', webp: 'WebP', bmp: 'BMP', tiff: 'TIFF',
    pdf: 'PDF', doc: 'DOC', docx: 'DOCX', xls: 'XLS', xlsx: 'XLSX', ppt: 'PPT', pptx: 'PPTX',
    odt: 'ODT', ods: 'ODS', odp: 'ODP', txt: 'TXT', html: 'HTML', md: 'Markdown', csv: 'CSV',
    mp4: 'MP4', webm: 'WebM', avi: 'AVI', mkv: 'MKV', mov: 'MOV',
    mp3: 'MP3', wav: 'WAV', ogg: 'OGG', flac: 'FLAC', aac: 'AAC', m4a: 'M4A',
    rst: 'RST', epub: 'EPUB',
};

export function getOutputFormats(inputMimeType: string): { value: string; label: string }[] {
    const formats = OUTPUT_FORMATS[inputMimeType] || [];
    return formats.map(f => ({ value: f, label: FORMAT_LABELS[f] || f.toUpperCase() }));
}

export function getFileCategory(mimeType: string): FileCategory {
    if (mimeType.startsWith('image/')) return 'image';
    if (mimeType.startsWith('video/') || mimeType.startsWith('audio/')) return 'media';
    return 'document';
}
