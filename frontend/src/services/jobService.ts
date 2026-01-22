import { JobStatusResponse, ErrorResponse } from '../types';

const API_BASE = '/api';

export async function getJobStatus(jobId: string, token: string): Promise<JobStatusResponse> {
    const response = await fetch(`${API_BASE}/jobs/${jobId}`, {
        headers: {
            'Authorization': `Bearer ${token}`
        }
    });

    if (!response.ok) {
        const error = await response.json() as ErrorResponse;
        throw new Error(error.message);
    }

    return response.json();
}

export async function pollJobStatus(
    jobId: string,
    token: string,
    onUpdate: (status: JobStatusResponse) => void,
    intervalMs: number = 1000
): Promise<JobStatusResponse> {
    return new Promise((resolve, reject) => {
        const poll = async () => {
            try {
                const status = await getJobStatus(jobId, token);
                onUpdate(status);

                if (status.status === 'COMPLETED' || status.status === 'FAILED' || status.status === 'EXPIRED') {
                    resolve(status);
                } else {
                    setTimeout(poll, intervalMs);
                }
            } catch (error) {
                reject(error);
            }
        };

        poll();
    });
}

export async function deleteJob(jobId: string, token: string): Promise<void> {
    const response = await fetch(`${API_BASE}/jobs/${jobId}`, {
        method: 'DELETE',
        headers: {
            'Authorization': `Bearer ${token}`
        }
    });

    if (!response.ok && response.status !== 204) {
        const error = await response.json() as ErrorResponse;
        throw new Error(error.message);
    }
}
