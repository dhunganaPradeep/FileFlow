import React, { useCallback, useState, useRef } from 'react';
import { formatFileSize } from '../../services/uploadService';
import './styles.css';

interface FileUploadProps {
    onFileSelect: (file: File) => void;
    selectedFile: File | null;
    onClear: () => void;
    disabled?: boolean;
    acceptedType?: string;
}

const MAX_SIZE = 500 * 1024 * 1024; // 500MB

export function FileUpload({ onFileSelect, selectedFile, onClear, disabled, acceptedType }: FileUploadProps) {
    const [isDragActive, setIsDragActive] = useState(false);
    const inputRef = useRef<HTMLInputElement>(null);

    const handleDrag = useCallback((e: React.DragEvent) => {
        e.preventDefault();
        e.stopPropagation();
        if (disabled) return;

        if (e.type === 'dragenter' || e.type === 'dragover') {
            setIsDragActive(true);
        } else if (e.type === 'dragleave') {
            setIsDragActive(false);
        }
    }, [disabled]);

    const handleDrop = useCallback((e: React.DragEvent) => {
        e.preventDefault();
        e.stopPropagation();
        setIsDragActive(false);

        if (disabled) return;

        const files = e.dataTransfer.files;
        if (files && files.length > 0) {
            validateAndSelect(files[0]);
        }
    }, [disabled]);

    const handleChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
        const files = e.target.files;
        if (files && files.length > 0) {
            validateAndSelect(files[0]);
        }
    }, []);

    const validateAndSelect = (file: File) => {
        if (file.size > MAX_SIZE) {
            alert(`File too large. Maximum size is ${formatFileSize(MAX_SIZE)}`);
            return;
        }
        onFileSelect(file);
    };

    const handleClick = () => {
        if (!disabled) {
            inputRef.current?.click();
        }
    };

    const getFileIcon = (type: string) => {
        if (type.startsWith('image/')) return '🖼️';
        if (type.startsWith('video/')) return '🎬';
        if (type.startsWith('audio/')) return '🎵';
        if (type.includes('pdf')) return '📄';
        if (type.includes('word') || type.includes('document')) return '📝';
        if (type.includes('excel') || type.includes('sheet')) return '📊';
        if (type.includes('powerpoint') || type.includes('presentation')) return '📽️';
        return '📁';
    };

    // Get accept attribute based on acceptedType
    const getAcceptAttribute = () => {
        if (!acceptedType) return '*/*';
        return acceptedType;
    };

    return (
        <div className="file-upload">
            <input
                ref={inputRef}
                type="file"
                accept={getAcceptAttribute()}
                onChange={handleChange}
                className="sr-only"
                disabled={disabled}
            />

            {!selectedFile ? (
                <div
                    className={`dropzone ${isDragActive ? 'active' : ''}`}
                    onDragEnter={handleDrag}
                    onDragLeave={handleDrag}
                    onDragOver={handleDrag}
                    onDrop={handleDrop}
                    onClick={handleClick}
                    role="button"
                    tabIndex={0}
                    aria-label="Upload file"
                >
                    <svg className="dropzone-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                            d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                    </svg>

                    <div className="dropzone-text">
                        <p className="dropzone-title">
                            {isDragActive ? 'Drop your file here' : 'Drag & drop your file'}
                        </p>
                        <p className="dropzone-subtitle">
                            or click to browse
                        </p>
                    </div>

                    <p className="dropzone-hint">
                        Max {formatFileSize(MAX_SIZE)}
                    </p>
                </div>
            ) : (
                <div className="file-preview">
                    <div className="file-preview-icon">
                        <span style={{ fontSize: '1.5rem' }}>{getFileIcon(selectedFile.type)}</span>
                    </div>
                    <div className="file-preview-info">
                        <p className="file-preview-name">{selectedFile.name}</p>
                        <p className="file-preview-meta">
                            {formatFileSize(selectedFile.size)}
                        </p>
                    </div>
                    <button
                        className="file-preview-remove"
                        onClick={(e) => { e.stopPropagation(); onClear(); }}
                        aria-label="Remove file"
                    >
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>
            )}
        </div>
    );
}
