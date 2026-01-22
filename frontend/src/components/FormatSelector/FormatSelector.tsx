import { getOutputFormats } from '../../types';
import './styles.css';

interface FormatSelectorProps {
    selectedFormat: string;
    onSelect: (format: string) => void;
    sourceType: string;
    disabled?: boolean;
}

export function FormatSelector({
    selectedFormat,
    onSelect,
    sourceType,
    disabled
}: FormatSelectorProps) {
    const options = getOutputFormats(sourceType);

    if (options.length === 0) {
        return (
            <div className="format-selector">
                <label className="format-selector-label">Convert to:</label>
                <p className="text-muted">Select a source format first</p>
            </div>
        );
    }

    return (
        <div className="format-selector">
            <label className="format-selector-label">Convert to:</label>
            <div className="format-selector-grid">
                {options.map((option) => (
                    <button
                        key={option.value}
                        className={`format-option ${selectedFormat === option.value ? 'selected' : ''}`}
                        onClick={() => onSelect(option.value)}
                        disabled={disabled}
                        type="button"
                    >
                        {option.label}
                    </button>
                ))}
            </div>
        </div>
    );
}
