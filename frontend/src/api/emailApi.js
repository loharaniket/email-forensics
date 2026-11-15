import { apiFetch } from './client';

export async function uploadEmail(file) {
  const formData = new FormData();
  formData.append('file', file);

  return apiFetch('/emails/analyze', {
    method: 'POST',
    body: formData
  });
}

export async function getEmailById(id) {
  return apiFetch(`/emails/${id}`);
}

export async function getEmailIndicators(id) {
  return apiFetch(`/emails/${id}/indicators`);
}

export async function checkThreats(id) {
  return apiFetch(`/emails/${id}/check-threats`, {
    method: 'POST'
  });
}

export async function getRecentEmails() {
  return apiFetch('/emails');
}

export async function downloadOriginalEmail(id, fileName) {
  const response = await fetch(`/api/emails/${id}/original`);
  if (!response.ok) {
    throw new Error(`Failed to download file: ${response.statusText}`);
  }

  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName || 'original.eml';
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}
