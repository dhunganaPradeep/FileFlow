import './styles.css';

interface ProgressBarProps {
    progress: number;
    label?: string;
    showValue?: boolean;
    indeterminate?: boolean;
}

export function ProgressBar({
    progress,
    label,
    showValue = true,
    indeterminate = false
}: ProgressBarProps) {
    const clampedProgress = Math.min(100, Math.max(0, progress));

    return (
        <div className={`progress-bar-container ${indeterminate ? 'progress-bar-indeterminate' : ''}`}>
            {(label || showValue) && (
                <div className="progress-bar-header">
                    {label && <span className="progress-bar-label">{label}</span>}
                    {showValue && !indeterminate && (
                        <span className="progress-bar-value">{clampedProgress}%</span>
                    )}
                </div>
            )}
            <div className="progress-bar-track">
                <div
                    className="progress-bar-fill"
                    style={{ width: indeterminate ? undefined : `${clampedProgress}%` }}
                    role="progressbar"
                    aria-valuenow={clampedProgress}
                    aria-valuemin={0}
                    aria-valuemax={100}
                />
            </div>
        </div>
    );
}
