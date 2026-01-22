import { useState, useCallback } from 'react';
import { FileUpload } from './components/FileUpload';
import { ProgressBar } from './components/ProgressBar';
import { ConversionStatus } from './components/ConversionStatus';
import { uploadFile, UploadProgress } from './services/uploadService';
import { pollJobStatus } from './services/jobService';
import { ConversionJob, INPUT_FORMATS, getOutputFormats } from './types';
import './App.css';

type AppState = 'idle' | 'uploading' | 'processing' | 'done';

function App() {
    const [state, setState] = useState<AppState>('idle');
    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [sourceFormat, setSourceFormat] = useState<string>('');
    const [targetFormat, setTargetFormat] = useState<string>('');
    const [uploadProgress, setUploadProgress] = useState<UploadProgress | null>(null);
    const [currentJob, setCurrentJob] = useState<ConversionJob | null>(null);
    const [error, setError] = useState<string | null>(null);

    const outputFormats = sourceFormat ? getOutputFormats(sourceFormat) : [];

    const handleSourceFormatChange = (value: string) => {
        setSourceFormat(value);
        setTargetFormat('');
        setSelectedFile(null);
    };

    const handleFileSelect = useCallback((file: File) => {
        setSelectedFile(file);
        setError(null);
    }, []);

    const handleClear = useCallback(() => {
        setSelectedFile(null);
        setError(null);
    }, []);

    const handleConvert = async () => {
        if (!selectedFile || !targetFormat) return;

        setError(null);
        setState('uploading');
        setUploadProgress({ loaded: 0, total: selectedFile.size, percentage: 0 });

        try {
            const uploadResponse = await uploadFile(
                selectedFile,
                targetFormat,
                (progress) => setUploadProgress(progress)
            );

            const job: ConversionJob = {
                id: uploadResponse.jobId,
                token: uploadResponse.token,
                fileName: uploadResponse.fileName,
                sourceFormat: uploadResponse.mimeType,
                targetFormat: uploadResponse.targetFormat,
                status: 'QUEUED',
                progress: 0
            };

            setCurrentJob(job);
            setState('processing');

            await pollJobStatus(
                job.id,
                job.token,
                (status) => {
                    setCurrentJob(prev => prev ? {
                        ...prev,
                        status: status.status,
                        progress: status.progress,
                        error: status.errorMessage || undefined
                    } : null);
                },
                1000
            );

            setState('done');

        } catch (err) {
            setError(err instanceof Error ? err.message : 'Conversion failed');
            setState('idle');
        }
    };

    const handleNewConversion = () => {
        setState('idle');
        setSelectedFile(null);
        setSourceFormat('');
        setTargetFormat('');
        setCurrentJob(null);
        setUploadProgress(null);
        setError(null);
    };

    const isConverting = state === 'uploading' || state === 'processing';
    const canConvert = selectedFile && targetFormat && !isConverting;

    return (
        <div className="app">
            <nav className="navbar">
                <div className="container">
                    <div className="logo">
                        <div className="logo-icon">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                                <path d="M7 16V4m0 0L3 8m4-4l4 4M17 8v12m0 0l4-4m-4 4l-4-4" />
                            </svg>
                        </div>
                        <span className="logo-text">FileFlow</span>
                    </div>
                </div>
            </nav>

            <main className="main">
                <div className="container">
                    <div className="hero">
                        <h1>Convert Files Fast & Securely</h1>
                        <p>High-quality file conversion for images, documents, and media.</p>
                    </div>

                    <div className="tool-card animate-fadeIn">
                        {state === 'idle' && (
                            <div className="converter-layout">
                                <div className="selection-grid">
                                    <div className="input-group">
                                        <label>From</label>
                                        <div className="select-wrapper">
                                            <select
                                                value={sourceFormat}
                                                onChange={(e) => handleSourceFormatChange(e.target.value)}
                                            >
                                                <option value="">Select Format</option>
                                                <optgroup label="Images">
                                                    {INPUT_FORMATS.filter(f => f.value.startsWith('image/')).map(f => (
                                                        <option key={f.value} value={f.value}>{f.label}</option>
                                                    ))}
                                                </optgroup>
                                                <optgroup label="Documents">
                                                    {INPUT_FORMATS.filter(f => f.value.startsWith('application/') || f.value.startsWith('text/')).map(f => (
                                                        <option key={f.value} value={f.value}>{f.label}</option>
                                                    ))}
                                                </optgroup>
                                                <optgroup label="Video/Audio">
                                                    {INPUT_FORMATS.filter(f => f.value.startsWith('video/') || f.value.startsWith('audio/')).map(f => (
                                                        <option key={f.value} value={f.value}>{f.label}</option>
                                                    ))}
                                                </optgroup>
                                            </select>
                                        </div>
                                    </div>

                                    <div className="divider">
                                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M5 12h14M12 5l7 7-7 7" />
                                        </svg>
                                    </div>

                                    <div className="input-group">
                                        <label>To</label>
                                        <div className="select-wrapper">
                                            <select
                                                value={targetFormat}
                                                onChange={(e) => setTargetFormat(e.target.value)}
                                                disabled={!sourceFormat}
                                            >
                                                <option value="">Select Format</option>
                                                {outputFormats.map(f => (
                                                    <option key={f.value} value={f.value}>{f.label}</option>
                                                ))}
                                            </select>
                                        </div>
                                    </div>
                                </div>

                                {sourceFormat && targetFormat && (
                                    <div className="upload-section animate-fadeIn">
                                        <FileUpload
                                            onFileSelect={handleFileSelect}
                                            selectedFile={selectedFile}
                                            onClear={handleClear}
                                            disabled={isConverting}
                                            acceptedType={sourceFormat}
                                        />
                                    </div>
                                )}

                                {error && (
                                    <div className="alert-error animate-fadeIn">
                                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                        </svg>
                                        {error}
                                    </div>
                                )}

                                {selectedFile && (
                                    <button
                                        className="btn btn-primary btn-xl convert-btn"
                                        onClick={handleConvert}
                                        disabled={!canConvert}
                                    >
                                        {isConverting ? 'Processing...' : 'Convert Now'}
                                    </button>
                                )}
                            </div>
                        )}

                        {state === 'uploading' && uploadProgress && (
                            <div className="processing-view">
                                <div className="spinner-large"></div>
                                <h2>Uploading your file...</h2>
                                <div className="progress-container">
                                    <ProgressBar
                                        progress={uploadProgress.percentage}
                                        label={selectedFile?.name}
                                    />
                                </div>
                            </div>
                        )}

                        {(state === 'processing' || state === 'done') && currentJob && (
                            <div className="processing-view">
                                <ConversionStatus
                                    job={currentJob}
                                    onNewConversion={handleNewConversion}
                                />
                            </div>
                        )}
                    </div>

                    <div className="footer-content animate-fadeIn">
                        <p className="footer-promo">Convert to anything from anything (limited to supported files)</p>
                        <div className="footer-tags">
                            <span className="tag">Open Source</span>
                            <span className="tag">Privacy First</span>
                            <span className="tag">Unlimited Files</span>
                        </div>
                    </div>
                </div>
            </main>

            <footer className="footer">
                <div className="container">
                    <p style={{ color: 'green', fontSize: '12px', fontWeight: 'bold', marginTop: '10px' }}>Files are automatically deleted after 10 minutes.</p>
                    <p>© {new Date().getFullYear()} Made by Pradip Dhungana</p>
                </div>
            </footer>
        </div>
    );
}

export default App;
