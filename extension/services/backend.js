export const DEFAULT_BACKEND_URL = 'http://localhost:8080';
export const DEFAULT_DASHBOARD_URL = 'http://localhost:5173';

export async function getBackendUrl() {
  return new Promise((resolve) => {
    if (!chrome.storage?.local) {
      resolve(DEFAULT_BACKEND_URL);
      return;
    }
    chrome.storage.local.get(['backendUrl'], (result) => {
      let url = result.backendUrl || DEFAULT_BACKEND_URL;
      resolve(url.trim().replace(/\/+$/, ''));
    });
  });
}

export async function getDashboardUrl() {
  return new Promise((resolve) => {
    if (!chrome.storage?.local) {
      resolve(DEFAULT_DASHBOARD_URL);
      return;
    }
    chrome.storage.local.get(['dashboardUrl'], (result) => {
      let url = result.dashboardUrl || DEFAULT_DASHBOARD_URL;
      resolve(url.trim().replace(/\/+$/, ''));
    });
  });
}

export async function testBackendConnection(backendUrl) {
  const base = backendUrl ? backendUrl.trim().replace(/\/+$/, '') : await getBackendUrl();
  const url = `${base}/api/v1/health`;

  try {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 6000);

    const res = await fetch(url, {
      method: 'GET',
      headers: { Accept: 'application/json' },
      signal: controller.signal
    });
    clearTimeout(timer);

    if (!res.ok) {
      return { ok: false, error: `Server returned HTTP ${res.status}` };
    }

    const data = await res.json();
    return { ok: true, status: data.status || 'ok' };
  } catch (err) {
    return {
      ok: false,
      error: err.name === 'AbortError' ? 'Connection timed out' : err.message
    };
  }
}

export async function uploadEvidence(emlBlob, filename, backendUrl) {
  const base = backendUrl ? backendUrl.trim().replace(/\/+$/, '') : await getBackendUrl();
  const url = `${base}/api/v1/evidence/upload`;

  const formData = new FormData();
  formData.append('file', emlBlob, filename);

  const response = await fetch(url, {
    method: 'POST',
    body: formData
  });

  if (!response.ok) {
    let detail = `HTTP ${response.status}`;
    try {
      const errJson = await response.json();
      if (errJson.message) detail = errJson.message;
    } catch (_) {}
    throw new Error(`Upload failed: ${detail}`);
  }

  return await response.json();
}

export async function getEvidenceStatus(emailId, backendUrl) {
  const base = backendUrl ? backendUrl.trim().replace(/\/+$/, '') : await getBackendUrl();
  const url = `${base}/api/v1/evidence/${encodeURIComponent(emailId)}/status`;

  const response = await fetch(url, {
    method: 'GET',
    headers: { Accept: 'application/json' }
  });

  if (!response.ok) {
    throw new Error(`Status check failed: HTTP ${response.status}`);
  }

  return await response.json();
}

export async function pollAnalysisStatus(emailId, backendUrl, onProgress = null, intervalMs = 2500, timeoutMs = 90000) {
  const startTime = Date.now();

  while (Date.now() - startTime < timeoutMs) {
    const statusDto = await getEvidenceStatus(emailId, backendUrl);
    const state = (statusDto.status || '').toUpperCase();

    if (typeof onProgress === 'function') {
      onProgress(statusDto);
    }

    if (state === 'COMPLETED') {
      return statusDto;
    }

    if (state === 'FAILED') {
      throw new Error(statusDto.errorMessage || 'Analysis failed on backend server.');
    }

    await new Promise((r) => setTimeout(r, intervalMs));
  }

  throw new Error('Analysis timed out. Please try again.');
}

async function fetchSafeJson(url) {
  try {
    const res = await fetch(url, {
      method: 'GET',
      headers: { Accept: 'application/json' }
    });
    if (res.ok) {
      return await res.json();
    }
    return null;
  } catch (_) {
    return null;
  }
}

export async function fetchAllAnalysisData(emailId, backendUrl) {
  const base = backendUrl ? backendUrl.trim().replace(/\/+$/, '') : await getBackendUrl();
  const eid = encodeURIComponent(emailId);

  const [
    investigation,
    headers,
    hops,
    urls,
    infrastructure,
    visuals,
    qrCodes,
    contentAnalysis,
    riskBreakdown,
    correlations,
    aiAssessment
  ] = await Promise.all([
    fetchSafeJson(`${base}/api/v1/evidence/${eid}/investigation`),
    fetchSafeJson(`${base}/api/v1/evidence/${eid}/headers`),
    fetchSafeJson(`${base}/api/v1/evidence/${eid}/hops`),
    fetchSafeJson(`${base}/api/v1/evidence/${eid}/urls`),
    fetchSafeJson(`${base}/api/v1/evidence/${eid}/infrastructure`),
    fetchSafeJson(`${base}/api/v1/evidence/${eid}/images`),
    fetchSafeJson(`${base}/api/v1/evidence/${eid}/qr`),
    fetchSafeJson(`${base}/api/v1/evidence/${eid}/content-analysis`),
    fetchSafeJson(`${base}/api/v1/evidence/${eid}/risk-breakdown`),
    fetchSafeJson(`${base}/api/v1/evidence/${eid}/correlations`),
    fetchSafeJson(`${base}/api/v1/evidence/${eid}/ai-assessment`)
  ]);

  return {
    emailId,
    investigation: investigation || {},
    headers: headers || {},
    hops: Array.isArray(hops) ? hops : [],
    urls: Array.isArray(urls) ? urls : [],
    infrastructure: Array.isArray(infrastructure) ? infrastructure : [],
    visuals: Array.isArray(visuals) ? visuals : [],
    qrCodes: Array.isArray(qrCodes) ? qrCodes : [],
    contentAnalysis: contentAnalysis || {},
    riskBreakdown: riskBreakdown || {},
    correlations: Array.isArray(correlations) ? correlations : [],
    aiAssessment: aiAssessment || null
  };
}

export async function generateAiAssessment(emailId, force = false, backendUrl) {
  const base = backendUrl ? backendUrl.trim().replace(/\/+$/, '') : await getBackendUrl();
  const url = `${base}/api/v1/evidence/${encodeURIComponent(emailId)}/ai-assessment/generate`;

  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'application/json'
    },
    body: JSON.stringify({ forceRegenerate: Boolean(force) })
  });

  if (!response.ok) {
    throw new Error(`AI assessment request failed (HTTP ${response.status})`);
  }

  return await response.json();
}
