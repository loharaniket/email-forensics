import {
  getAuthToken,
  removeCachedToken,
  clearAllCachedTokens,
  fetchGmailThread,
  fetchRawMessage,
  fetchMessageMinimal,
  extractMessageMetadata,
  getEffectiveOAuthConfig
} from './services/gmail.js';
import {
  getBackendUrl,
  getDashboardUrl,
  testBackendConnection,
  uploadEvidence,
  pollAnalysisStatus,
  fetchAllAnalysisData,
  generateAiAssessment
} from './services/backend.js';

function base64UrlToUint8Array(base64Url) {
  if (!base64Url || typeof base64Url !== 'string') {
    throw new Error('Invalid email payload: base64url string is empty or missing.');
  }

  let base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
  const remainder = base64.length % 4;
  if (remainder === 2) {
    base64 += '==';
  } else if (remainder === 3) {
    base64 += '=';
  } else if (remainder === 1) {
    throw new Error('Malformed base64url string: invalid remainder length.');
  }

  const binaryString = atob(base64);
  const bytes = new Uint8Array(binaryString.length);
  for (let i = 0; i < binaryString.length; i++) {
    bytes[i] = binaryString.charCodeAt(i);
  }
  return bytes;
}

function createEmlBlob(rawBase64Url, filename = 'email.eml') {
  const bytes = base64UrlToUint8Array(rawBase64Url);
  if (bytes.length === 0) {
    throw new Error('The decoded email file is empty (0 bytes).');
  }
  return new Blob([bytes], { type: 'message/rfc822' });
}

function sanitizeFilename(raw, fallback = 'email-evidence') {
  if (!raw || typeof raw !== 'string') {
    return fallback;
  }
  const clean = raw
    .trim()
    .replace(/[<>:"/\\|?*\x00-\x1F]/g, '_')
    .replace(/\s+/g, '_')
    .substring(0, 60);

  return clean.length > 0 ? clean : fallback;
}

async function setupBackendCorsRules(customUrl = null) {
  if (!chrome.declarativeNetRequest || !chrome.declarativeNetRequest.updateDynamicRules) {
    return;
  }

  try {
    const base = customUrl || (await getBackendUrl());
    const cleanBase = base.trim().replace(/\/+$/, '');

    const rules = [
      {
        id: 1001,
        priority: 1,
        action: {
          type: 'modifyHeaders',
          requestHeaders: [
            {
              header: 'origin',
              operation: 'set',
              value: 'http://localhost:5173'
            }
          ]
        },
        condition: {
          urlFilter: `${cleanBase}/api/*`,
          resourceTypes: ['xmlhttprequest']
        }
      },
      {
        id: 1002,
        priority: 1,
        action: {
          type: 'modifyHeaders',
          requestHeaders: [
            {
              header: 'origin',
              operation: 'set',
              value: 'http://localhost:5173'
            }
          ]
        },
        condition: {
          urlFilter: 'http://localhost:8080/api/*',
          resourceTypes: ['xmlhttprequest']
        }
      },
      {
        id: 1003,
        priority: 1,
        action: {
          type: 'modifyHeaders',
          requestHeaders: [
            {
              header: 'origin',
              operation: 'set',
              value: 'http://127.0.0.1:5173'
            }
          ]
        },
        condition: {
          urlFilter: 'http://127.0.0.1:8080/api/*',
          resourceTypes: ['xmlhttprequest']
        }
      }
    ];

    await chrome.declarativeNetRequest.updateDynamicRules({
      removeRuleIds: [1001, 1002, 1003],
      addRules: rules
    });
  } catch (err) {
    console.warn('[Email Forensics] Could not update dynamic CORS rules:', err);
  }
}

setupBackendCorsRules();
if (chrome.runtime?.onInstalled) {
  chrome.runtime.onInstalled.addListener(() => setupBackendCorsRules());
}
if (chrome.runtime?.onStartup) {
  chrome.runtime.onStartup.addListener(() => setupBackendCorsRules());
}

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  const { action, payload } = message || {};

  switch (action) {
    case 'PING':
      sendResponse({ status: 'PONG', timestamp: Date.now() });
      return false;

    case 'CLEAR_AUTH_CACHE':
      clearAllCachedTokens()
        .then(() => sendResponse({ success: true }))
        .catch((err) => sendResponse({ success: false, error: err.message }));
      return true;

    case 'GET_AUTH_STATUS':
      handleGetAuthStatus()
        .then((result) => sendResponse(result))
        .catch((err) => sendResponse({ signedIn: false, error: err.message }));
      return true;

    case 'SIGN_IN':
      handleSignIn(payload)
        .then((result) => sendResponse(result))
        .catch((err) => sendResponse({ success: false, error: err.message }));
      return true;

    case 'SIGN_OUT':
      handleSignOut()
        .then((result) => sendResponse(result))
        .catch((err) => sendResponse({ success: false, error: err.message }));
      return true;

    case 'GET_BACKEND_STATUS':
      handleGetBackendStatus(payload?.backendUrl)
        .then((result) => sendResponse(result))
        .catch((err) => sendResponse({ ok: false, error: err.message }));
      return true;

    case 'GET_CONFIG':
      handleGetConfig()
        .then((config) => sendResponse(config))
        .catch((err) => sendResponse({ error: err.message }));
      return true;

    case 'SET_CONFIG':
      handleSetConfig(payload)
        .then((result) => sendResponse(result))
        .catch((err) => sendResponse({ success: false, error: err.message }));
      return true;

    case 'GET_THREAD_MESSAGES':
      handleGetThreadMessages(payload)
        .then((result) => sendResponse(result))
        .catch((err) => sendResponse({ success: false, error: err.message }));
      return true;

    case 'ANALYZE_GMAIL_MESSAGE':
      handleAnalyzeMessage(payload, sender?.tab?.id)
        .then((result) => sendResponse(result))
        .catch((err) => sendResponse({ success: false, error: err.message }));
      return true;

    default:
      sendResponse({ error: `Unknown action: ${action}` });
      return false;
  }
});

async function handleGetAuthStatus() {
  const oauthConfig = await getEffectiveOAuthConfig();
  const redirectUri = chrome.identity?.getRedirectURL ? chrome.identity.getRedirectURL() : `https://${oauthConfig.extensionId}.chromiumapp.org/`;

  if (!oauthConfig.isConfigured) {
    return {
      signedIn: false,
      configured: false,
      extensionId: oauthConfig.extensionId,
      clientId: oauthConfig.clientId,
      authMethod: oauthConfig.authMethod,
      redirectUri,
      error: 'Google OAuth Client ID is not configured yet. Please open Settings.'
    };
  }

  try {
    const token = await getAuthToken(false);
    return {
      signedIn: Boolean(token),
      configured: true,
      extensionId: oauthConfig.extensionId,
      clientId: oauthConfig.clientId,
      authMethod: oauthConfig.authMethod,
      redirectUri,
      error: null
    };
  } catch (err) {
    return {
      signedIn: false,
      configured: true,
      extensionId: oauthConfig.extensionId,
      clientId: oauthConfig.clientId,
      authMethod: oauthConfig.authMethod,
      redirectUri,
      error: err.message
    };
  }
}

async function handleSignIn(payload) {
  try {
    const token = await getAuthToken(true, payload?.authMethod);
    return { success: Boolean(token), error: null };
  } catch (err) {
    return { success: false, error: err.message };
  }
}

async function handleSignOut() {
  try {
    await clearAllCachedTokens();
    const token = await getAuthToken(false).catch(() => null);
    if (token) {
      await removeCachedToken(token);
    }
    return { success: true };
  } catch (err) {
    return { success: false, error: err.message };
  }
}

async function handleGetBackendStatus(backendUrl) {
  return await testBackendConnection(backendUrl);
}

async function handleGetConfig() {
  const backendUrl = await getBackendUrl();
  const dashboardUrl = await getDashboardUrl();
  const oauthConfig = await getEffectiveOAuthConfig();
  const redirectUri = chrome.identity?.getRedirectURL ? chrome.identity.getRedirectURL() : `https://${oauthConfig.extensionId}.chromiumapp.org/`;

  return {
    backendUrl,
    dashboardUrl,
    googleClientId: oauthConfig.clientId,
    authMethod: oauthConfig.authMethod,
    extensionId: oauthConfig.extensionId,
    redirectUri
  };
}

async function handleSetConfig(payload) {
  if (!payload) return { success: false, error: 'No config provided' };

  return new Promise((resolve) => {
    const updates = {};
    if (payload.backendUrl !== undefined) {
      updates.backendUrl = payload.backendUrl.trim().replace(/\/+$/, '');
    }
    if (payload.dashboardUrl !== undefined) {
      updates.dashboardUrl = payload.dashboardUrl.trim().replace(/\/+$/, '');
    }
    if (payload.googleClientId !== undefined) {
      updates.googleClientId = payload.googleClientId.trim();
    }
    if (payload.authMethod !== undefined) {
      updates.authMethod = payload.authMethod;
    }

    chrome.storage.local.set(updates, () => {
      if (updates.backendUrl) {
        setupBackendCorsRules(updates.backendUrl);
      }
      resolve({ success: true, config: updates });
    });
  });
}

async function handleGetThreadMessages(payload) {
  const threadId = typeof payload === 'string' ? payload : payload?.threadId;
  const messageId = typeof payload === 'object' ? payload?.messageId : null;
  const subject = typeof payload === 'object' ? payload?.subject : '';
  const senderEmail = typeof payload === 'object' ? payload?.senderEmail : '';

  if (!threadId && !messageId) {
    throw new Error('Email thread ID or message ID is required.');
  }

  const token = await getAuthToken(true);

  let targetThreadId = threadId;

  if ((!targetThreadId || !/^[0-9a-f]{16}$/i.test(targetThreadId)) && messageId && /^[0-9a-f]{16}$/i.test(messageId)) {
    const minimal = await fetchMessageMinimal(messageId, token);
    if (minimal?.threadId && /^[0-9a-f]{16}$/i.test(minimal.threadId)) {
      targetThreadId = minimal.threadId;
    }
  }

  const threadData = await fetchGmailThread(targetThreadId, token, subject, senderEmail);
  const rawMessages = threadData.messages || [];
  const parsedMessages = rawMessages.map(extractMessageMetadata);

  return {
    success: true,
    threadId: threadData.id,
    historyId: threadData.historyId,
    messages: parsedMessages
  };
}

function sendProgressToTab(tabId, stage, label, percent = 0) {
  if (!tabId) return;
  chrome.tabs.sendMessage(tabId, {
    action: 'ANALYSIS_PROGRESS',
    payload: { stage, label, percent }
  }).catch(() => {});
}

async function handleAnalyzeMessage(payload, tabId) {
  const { messageId, threadId, subject } = payload || {};

  if (!messageId) {
    throw new Error('Message ID is required to analyze email.');
  }

  const backendUrl = await getBackendUrl();
  const dashboardUrl = await getDashboardUrl();

  sendProgressToTab(tabId, 'CONNECTING_BACKEND', 'Connecting to backend server...', 10);
  const health = await testBackendConnection(backendUrl);
  if (!health.ok) {
    throw new Error(`Backend server is not reachable at ${backendUrl}. Please make sure it is running.`);
  }

  sendProgressToTab(tabId, 'AUTHENTICATING', 'Signing in to Gmail...', 20);
  const token = await getAuthToken(true);

  sendProgressToTab(tabId, 'FETCHING_RAW_EMAIL', 'Downloading original email from Gmail...', 30);
  const rawBase64Url = await fetchRawMessage(messageId, token);

  sendProgressToTab(tabId, 'PREPARING_EML', 'Preparing email file...', 40);
  const safeSubj = sanitizeFilename(subject || messageId);
  const filename = `email-forensics-${safeSubj}.eml`;
  const emlBlob = createEmlBlob(rawBase64Url, filename);

  sendProgressToTab(tabId, 'UPLOADING_EVIDENCE', 'Uploading email to backend...', 50);
  const uploadResponse = await uploadEvidence(emlBlob, filename, backendUrl);

  const emailId = uploadResponse.emailId;
  if (!emailId) {
    throw new Error('Upload failed: no email ID was returned by backend.');
  }

  sendProgressToTab(tabId, 'PIPELINE_RUNNING', 'Running forensic analysis...', 65);
  await pollAnalysisStatus(
    emailId,
    backendUrl,
    (statusDto) => {
      const stage = statusDto.currentStage || statusDto.status || 'PROCESSING';
      sendProgressToTab(tabId, stage, `Analysis Stage: ${stage}`, 75);
    },
    2500,
    90000
  );

  sendProgressToTab(tabId, 'FETCHING_INTELLIGENCE', 'Fetching analysis results...', 85);
  const analysisData = await fetchAllAnalysisData(emailId, backendUrl);

  if (!analysisData.aiAssessment || analysisData.aiAssessment.status === 'NOT_GENERATED') {
    sendProgressToTab(tabId, 'GENERATING_AI', 'Generating AI summary...', 92);
    try {
      const aiResult = await generateAiAssessment(emailId, false, backendUrl);
      analysisData.aiAssessment = aiResult;
    } catch (aiErr) {
      console.warn('AI assessment skipped:', aiErr.message);
    }
  }

  sendProgressToTab(tabId, 'COMPLETED', 'Analysis Complete!', 100);

  return {
    success: true,
    emailId,
    uploadResponse,
    analysisData,
    ...analysisData,
    backendUrl,
    dashboardUrl,
    dashboardViewUrl: `${dashboardUrl}/evidence/${emailId}`
  };
}
