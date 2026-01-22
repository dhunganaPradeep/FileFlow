const API_BASE = '/api';

export async function downloadFile(jobId: string, token: string, fileName: string): Promise<void> {
    const response = await fetch(`${API_BASE}/jobs/${jobId}/download`, {
        headers: {
            'Authorization': `Bearer ${token}`
        }
    });

    if (!response.ok) {
        throw new Error('Download failed');
    }

    // Stream to blob
    const blob = await response.blob();

    // Create download link
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();

    // Cleanup
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
}

export async function streamDownload(
    jobId: string,
    token: string,
    fileName: string,
    onProgress?: (loaded: number, total: number) => void
): Promise<void> {
    const response = await fetch(`${API_BASE}/jobs/${jobId}/download`, {
        headers: {
            'Authorization': `Bearer ${token}`
        }
    });

    if (!response.ok) {
        throw new Error('Download failed');
    }

    const contentLength = response.headers.get('Content-Length');
    const total = contentLength ? parseInt(contentLength, 10) : 0;

    const reader = response.body?.getReader();
    if (!reader) {
        throw new Error('Streaming not supported');
    }

    const chunks: Uint8Array[] = [];
    let loaded = 0;

    while (true) {
        const { done, value } = await reader.read();

        if (done) break;

        chunks.push(value);
        loaded += value.length;

        if (onProgress && total) {
            onProgress(loaded, total);
        }
    }

    // Combine chunks and download
    const blob = new Blob(chunks as BlobPart[]);
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
}
