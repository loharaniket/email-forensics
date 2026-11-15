import { apiFetch } from './client';

export async function fetchHealth() {
  return apiFetch('/health');
}

export async function truncateDatabaseAndEvidence() {
  return apiFetch('/system/truncate-all', { method: 'POST' });
}
