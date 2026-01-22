import { ConversionJob } from '../../types';
import { ProgressBar } from '../ProgressBar';
import { downloadFile } from '../../services/downloadService';
import './styles.css';

interface ConversionStatusProps {
    job: ConversionJob;
    onNewConversion: () => void;
}

export function ConversionStatus({ job, onNewConversion }: ConversionStatusProps) {
    const handleDownload = async () => {
        try {
            const outputExt = job.targetFormat;
            const baseName = job.fileName.replace(/\.[^/.]+$/, '');
            const outputName = `${baseName}.${outputExt}`;
            await downloadFile(job.id, job.token, outputName);
        } catch (error) {
            console.error('Download failed:', error);
        }
    };

    const getStatusIcon = () => {
        switch (job.status) {
            case 'QUEUED':
                return '⏳';
            case 'PROCESSING':
                return '⚡';
            case 'COMPLETED':
                return '✓';
            case 'FAILED':
            case 'EXPIRED':
                return '✕';
            default:
                return '•';
        }
    };

    const getStatusText = () => {
        switch (job.status) {
            case 'QUEUED':
                return 'Queued';
            case 'PROCESSING':
                return 'Converting...';
            case 'COMPLETED':
                return 'Conversion Complete';
            case 'FAILED':
                return 'Conversion Failed';
            case 'EXPIRED':
                return 'Expired';
            default:
                return 'Unknown';
        }
    };

    const isProcessing = job.status === 'QUEUED' || job.status === 'PROCESSING';

    return (
        <div className="conversion-status">
            <div className="status-card">
                <div className="status-header">
                    <div className={`status-icon ${job.status.toLowerCase()}`}>
                        {isProcessing ? (
                            <svg className="status-spinner" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor">
                                <path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" strokeWidth="2" strokeLinecap="round" />
                            </svg>
                        ) : (
                            <span>{getStatusIcon()}</span>
                        )}
                    </div>
                    <div className="status-info">
                        <h3 className="status-title">{getStatusText()}</h3>
                        <p className="status-subtitle">
                            {job.fileName} → {job.targetFormat.toUpperCase()}
                        </p>
                    </div>
                </div>

                {isProcessing && (
                    <ProgressBar
                        progress={job.progress}
                        label="Progress"
                        indeterminate={job.status === 'QUEUED'}
                    />
                )}

                {job.status === 'FAILED' && job.error && (
                    <div className="error-message">
                        {job.error}
                    </div>
                )}

                <div className="status-actions">
                    {job.status === 'COMPLETED' && (
                        <button className="btn btn-primary" onClick={handleDownload}>
                            <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                            </svg>
                            Download
                        </button>
                    )}
                    <button className="btn btn-secondary" onClick={onNewConversion}>
                        {job.status === 'COMPLETED' ? 'Convert Another' : 'Start Over'}
                    </button>
                </div>
            </div>
        </div>
    );
}
