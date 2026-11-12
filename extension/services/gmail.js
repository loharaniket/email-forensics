const GMAIL_API_BASE = 'https://gmail.googleapis.com/gmail/v1/users/me';

let memoryToken = null;
let memoryExpiry = 0;

export function getOAuthConfig() {
  const manifest = (typeof chrome !== 'undefined' && chrome.runtime?.getManifest)
    ? chrome.runtime.getManifest()
    : null;
  const clientId = manifest?.oauth2?.client_id || '';
  const extensionId = (typeof chrome !== 'undefined' && chrome.runtime?.id)
    ? chrome.runtime.id
    : '';

  const isConfigured = Boolean(
    clientId &&
    !clientId.includes('YOUR_GOOGLE_CLIENT_ID') &&
    !clientId.includes('YOUR_ACTUAL_CLIENT_ID') &&
    !clientId.startsWith('YOUR_') &&
    clientId.endsWith('.apps.googleusercontent.com')
  );

  return {
    isConfigured,
    clientId,
    extensionId,
    scopes: manifest?.oauth2?.scopes || ['https://www.googleapis.com/auth/gmail.readonly']
  };
}

export async function getEffectiveOAuthConfig() {
  const baseConfig = getOAuthConfig();

  return new Promise((resolve) => {
    if (!chrome.storage?.local) {
      resolve({
        ...baseConfig,
        authMethod: 'auto',
        isCustom: false
      });
      return;
    }

    chrome.storage.local.get(['googleClientId', 'authMethod'], (res) => {
      const customId = (res?.googleClientId || '').trim();
      const authMethod = res?.authMethod || 'auto';
      const clientId = customId || baseConfig.clientId;

      const isConfigured = Boolean(
        clientId &&
        !clientId.includes('YOUR_GOOGLE_CLIENT_ID') &&
        !clientId.includes('YOUR_ACTUAL_CLIENT_ID') &&
        !clientId.startsWith('YOUR_') &&
        clientId.endsWith('.apps.googleusercontent.com')
      );

      resolve({
        ...baseConfig,
        clientId,
        authMethod,
        isConfigured,
        isCustom: Boolean(customId)
      });
    });
  });
}

export async function getStoredToken() {
  if (memoryToken && Date.now() < memoryExpiry) {
    return memoryToken;
  }

  return new Promise((resolve) => {
    if (!chrome.storage?.local) {
      resolve(null);
      return;
    }

    chrome.storage.local.get(['google_access_token', 'google_token_expiry'], (res) => {
      const token = res?.google_access_token;
      const expiry = res?.google_token_expiry || 0;

      if (token && Date.now() < expiry) {
        memoryToken = token;
        memoryExpiry = expiry;
        resolve(token);
      } else {
        resolve(null);
      }
    });
  });
}

export async function saveStoredToken(token, expiresInSeconds = 3500) {
  memoryToken = token;
  memoryExpiry = Date.now() + Math.max(60, expiresInSeconds - 60) * 1000;

  return new Promise((resolve) => {
    if (!chrome.storage?.local) {
      resolve();
      return;
    }

    chrome.storage.local.set({
      google_access_token: token,
      google_token_expiry: memoryExpiry
    }, () => resolve());
  });
}

let activeWebAuthPromise = null;

export async function getAuthTokenViaWebFlow(interactive = true, config = null) {
  if (activeWebAuthPromise) {
    return activeWebAuthPromise;
  }

  activeWebAuthPromise = (async () => {
    const oauthConfig = config || (await getEffectiveOAuthConfig());

    if (!oauthConfig.isConfigured) {
      throw new Error(
        `Google OAuth Client ID is not configured.\nYour Extension ID is: ${oauthConfig.extensionId}\n` +
        `Please configure your OAuth Client ID in Extension Settings.`
      );
    }

    if (!chrome.identity || !chrome.identity.launchWebAuthFlow) {
      throw new Error('chrome.identity.launchWebAuthFlow API is not available in this browser context.');
    }

    const redirectUri = chrome.identity.getRedirectURL ? chrome.identity.getRedirectURL() : `https://${oauthConfig.extensionId}.chromiumapp.org/`;
    const scopes = oauthConfig.scopes?.length ? oauthConfig.scopes : ['https://www.googleapis.com/auth/gmail.readonly'];

    const authUrl = new URL('https://accounts.google.com/o/oauth2/v2/auth');
    authUrl.searchParams.set('client_id', oauthConfig.clientId);
    authUrl.searchParams.set('response_type', 'token');
    authUrl.searchParams.set('redirect_uri', redirectUri);
    authUrl.searchParams.set('scope', scopes.join(' '));
    authUrl.searchParams.set('prompt', 'select_account');

    return new Promise((resolve, reject) => {
      chrome.identity.launchWebAuthFlow(
        {
          url: authUrl.toString(),
          interactive: Boolean(interactive)
        },
        (responseUrl) => {
          if (chrome.runtime.lastError) {
            const rawErr = chrome.runtime.lastError.message || 'WebAuthFlow failed.';
            if (rawErr.includes('Only one web auth flow is allowed')) {
              reject(new Error(
                'A Google login window is already open.\n\n' +
                'Please check your Windows taskbar or press Alt+Tab to find the existing Google sign-in window, complete the login (or close it), and try again.'
              ));
              return;
            }
            reject(new Error(rawErr));
            return;
          }

          if (!responseUrl) {
            reject(new Error('No response URL received from Google OAuth flow. Window may have been closed.'));
            return;
          }

          try {
            const urlObj = new URL(responseUrl);
            const rawHash = urlObj.hash.startsWith('#') ? urlObj.hash.slice(1) : urlObj.hash;
            const hashParams = new URLSearchParams(rawHash);
            const accessToken = hashParams.get('access_token');
            const expiresIn = parseInt(hashParams.get('expires_in') || '3500', 10);
            const error = hashParams.get('error') || urlObj.searchParams.get('error');

            if (error) {
              const errorDesc = hashParams.get('error_description') || urlObj.searchParams.get('error_description') || error;
              reject(new Error(`Google OAuth returned error: ${errorDesc}`));
              return;
            }

            if (!accessToken) {
              reject(new Error('OAuth response URL did not contain an access_token.'));
              return;
            }

            saveStoredToken(accessToken, expiresIn).catch(() => {});
            resolve(accessToken);
          } catch (parseErr) {
            reject(new Error(`Failed to parse OAuth redirect URL: ${parseErr.message}`));
          }
        }
      );
    });
  })();

  try {
    return await activeWebAuthPromise;
  } finally {
    activeWebAuthPromise = null;
  }
}

export async function getAuthTokenViaIdentity(interactive = true) {
  return new Promise((resolve, reject) => {
    if (!chrome.identity || !chrome.identity.getAuthToken) {
      reject(new Error('Chrome Identity API (getAuthToken) is not available.'));
      return;
    }

    chrome.identity.getAuthToken({ interactive: Boolean(interactive) }, (token) => {
      if (chrome.runtime.lastError) {
        const rawErr = chrome.runtime.lastError.message || 'OAuth authentication failed.';
        reject(new Error(rawErr));
        return;
      }

      if (!token) {
        reject(new Error('No Google OAuth access token was returned.'));
        return;
      }

      saveStoredToken(token, 3500).catch(() => {});
      resolve(token);
    });
  });
}

export async function getAuthToken(interactive = true, preferredMethod = null) {
  const cached = await getStoredToken();
  if (cached) {
    return cached;
  }

  if (!interactive) {
    return null;
  }

  const oauthConfig = await getEffectiveOAuthConfig();
  if (!oauthConfig.isConfigured) {
    throw new Error(
      `Google OAuth Client ID is not configured.\nYour Extension ID is: ${oauthConfig.extensionId}\n` +
      `Please configure your OAuth Client ID in Extension Settings.`
    );
  }

  const method = preferredMethod || (oauthConfig.authMethod === 'chrome' ? 'chrome' : 'web');

  if (method === 'chrome') {
    try {
      return await getAuthTokenViaIdentity(true);
    } catch (err) {
      const msg = err.message || '';
      if (msg.includes('Custom scheme') || msg.includes('Custom URI scheme') || msg.includes('WEB') || msg.includes('400')) {
        throw new Error('Please select "Web Sign-in" in Extension Settings to connect your Google account.');
      }
      throw err;
    }
  }

  try {
    return await getAuthTokenViaWebFlow(true, oauthConfig);
  } catch (err) {
    const msg = err.message || '';
    if (msg.includes('redirect_uri_mismatch')) {
      throw new Error(
        `Google OAuth Error: redirect_uri_mismatch.\n\n` +
        `The Authorized Redirect URI is missing or does not match in Google Cloud Console.\n` +
        `1. Go to Google Cloud Console > APIs & Services > Credentials.\n` +
        `2. Click your Web Application Client ID.\n` +
        `3. Under "Authorized redirect URIs", ensure this exact URI is added:\n` +
        `   https://${oauthConfig.extensionId}.chromiumapp.org/\n` +
        `4. Click Save and try again.`
      );
    }
    if (msg.includes('access_denied') || msg.includes('verification process') || msg.includes('403')) {
      throw new Error(
        `Google OAuth Error 403 (access_denied):\n` +
        `Your Gmail address is not listed under Test Users in Google Cloud Console.\n\n` +
        `Fix: In Google Cloud Console > APIs & Services > Audience (or OAuth consent screen), scroll down to "Test users", click "+ Add users", and enter your email address.`
      );
    }
    throw err;
  }
}

export async function clearAllCachedTokens() {
  memoryToken = null;
  memoryExpiry = 0;

  return new Promise((resolve) => {
    if (chrome.storage?.local) {
      chrome.storage.local.remove(['google_access_token', 'google_token_expiry'], () => {});
    }

    if (chrome.identity && chrome.identity.clearAllCachedAuthTokens) {
      chrome.identity.clearAllCachedAuthTokens(() => resolve());
    } else {
      resolve();
    }
  });
}

export async function removeCachedToken(token) {
  memoryToken = null;
  memoryExpiry = 0;

  return new Promise((resolve) => {
    if (chrome.storage?.local) {
      chrome.storage.local.remove(['google_access_token', 'google_token_expiry'], () => {});
    }

    if (!token || !chrome.identity || !chrome.identity.removeCachedAuthToken) {
      resolve();
      return;
    }

    chrome.identity.removeCachedAuthToken({ token }, () => {
      resolve();
    });
  });
}

export async function resolveThreadIdViaSearch(rawId, subject, senderEmail, token) {
  try {
    const queryParts = [];

    if (subject && subject !== '(No Subject)' && subject !== 'Email Analysis') {
      const cleanSubj = subject
        .replace(/^(?:re|fwd|fw):\s*/i, '')
        .replace(/["\\]/g, ' ')
        .trim();
      if (cleanSubj) {
        queryParts.push(`subject:("${cleanSubj}")`);
      }
    }

    if (senderEmail && typeof senderEmail === 'string' && senderEmail.includes('@')) {
      const cleanSender = senderEmail.replace(/[<>"'\\]/g, '').trim();
      if (cleanSender) {
        queryParts.push(`from:(${cleanSender})`);
      }
    }

    if (queryParts.length === 0) {
      return null;
    }

    const query = queryParts.join(' ');
    const searchUrl = `${GMAIL_API_BASE}/threads?maxResults=5&q=${encodeURIComponent(query)}`;
    const res = await fetch(searchUrl, {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: 'application/json'
      }
    });

    if (res.ok) {
      const data = await res.json();
      const threads = data.threads || [];
      if (threads.length > 0 && threads[0].id) {
        return threads[0].id;
      }
    }

    if (queryParts.length > 1) {
      const subjQuery = queryParts[0];
      const subjRes = await fetch(`${GMAIL_API_BASE}/threads?maxResults=5&q=${encodeURIComponent(subjQuery)}`, {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`,
          Accept: 'application/json'
        }
      });

      if (subjRes.ok) {
        const data = await subjRes.json();
        const threads = data.threads || [];
        if (threads.length > 0 && threads[0].id) {
          return threads[0].id;
        }
      }
    }
  } catch (err) {
    console.warn('[Gmail Service] Thread resolution search failed:', err.message);
  }

  return null;
}

export async function fetchMessageMinimal(messageId, token) {
  if (!messageId || !/^[0-9a-f]{16}$/i.test(String(messageId).trim())) {
    return null;
  }

  try {
    const url = `${GMAIL_API_BASE}/messages/${encodeURIComponent(messageId)}?format=minimal`;
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: 'application/json'
      }
    });

    if (response.ok) {
      return await response.json();
    }
  } catch (err) {
    console.warn('[Gmail Service] fetchMessageMinimal failed:', err.message);
  }

  return null;
}

export async function fetchGmailThread(threadId, token, subject = '', senderEmail = '') {
  if (!threadId) {
    throw new Error('A valid Gmail thread ID is required to fetch conversation.');
  }

  let targetThreadId = String(threadId).trim();
  if (!/^[0-9a-f]{16}$/i.test(targetThreadId)) {
    const resolvedId = await resolveThreadIdViaSearch(targetThreadId, subject, senderEmail, token);
    if (resolvedId && /^[0-9a-f]{16}$/i.test(resolvedId)) {
      targetThreadId = resolvedId;
    } else {
      throw new Error(
        `Could not locate the conversation thread in your Gmail mailbox.\n` +
        `Subject: "${subject || 'Unknown'}"\n\n` +
        `Please ensure the email is open on screen and try again.`
      );
    }
  }

  const url = `${GMAIL_API_BASE}/threads/${encodeURIComponent(targetThreadId)}?format=full`;

  const response = await fetch(url, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: 'application/json'
    }
  });

  if (response.status === 401) {
    await removeCachedToken(token);
    throw new Error('AUTH_EXPIRED: Google OAuth token has expired or is invalid. Please click Analyze Email again to refresh.');
  }

  if (response.status === 403) {
    const errorJson = await response.json().catch(() => null);
    const googleMsg = errorJson?.error?.message || '';

    if (googleMsg.toLowerCase().includes('not been used') || googleMsg.toLowerCase().includes('disabled') || googleMsg.toLowerCase().includes('enable it')) {
      throw new Error(
        `Gmail API is not enabled in your Google Cloud Project.\n\n` +
        `How to fix this in 30 seconds:\n` +
        `1. Open: https://console.cloud.google.com/apis/library/gmail.googleapis.com\n` +
        `2. Click the blue "ENABLE" button.\n` +
        `3. Wait 30 seconds and click Analyze Email again.`
      );
    }

    if (googleMsg.toLowerCase().includes('insufficient') || googleMsg.toLowerCase().includes('permission')) {
      await removeCachedToken(token);
      throw new Error(
        `Gmail Read-Only Permission was not granted during sign-in.\n\n` +
        `When the Google permissions prompt appears, ensure the checkbox for "View your email messages and settings" is checked.\n` +
        `Token cache has been reset. Please click Analyze Email again and check the box.`
      );
    }

    throw new Error(`Gmail API Access Forbidden (HTTP 403): ${googleMsg || 'Check that Gmail API is enabled in Google Cloud Console.'}`);
  }

  if (response.status === 400) {
    const errorJson = await response.json().catch(() => null);
    const googleMsg = errorJson?.error?.message || '';
    throw new Error(`Gmail API Error (HTTP 400): ${googleMsg || 'Invalid conversation thread ID format. Ensure an email is open.'}`);
  }

  if (response.status === 404) {
    throw new Error(`Gmail thread "${targetThreadId}" was not found.`);
  }

  if (!response.ok) {
    const errorJson = await response.json().catch(() => null);
    const googleMsg = errorJson?.error?.message || response.statusText;
    throw new Error(`Gmail API request failed with HTTP ${response.status}: ${googleMsg}`);
  }

  const data = await response.json();
  return data;
}

export async function fetchRawMessage(messageId, token) {
  if (!messageId) {
    throw new Error('A valid Gmail message ID is required to fetch raw email.');
  }

  const url = `${GMAIL_API_BASE}/messages/${encodeURIComponent(messageId)}?format=raw`;

  const response = await fetch(url, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: 'application/json'
    }
  });

  if (response.status === 401) {
    await removeCachedToken(token);
    throw new Error('AUTH_EXPIRED: Google OAuth token has expired or is invalid. Please click Analyze Email again.');
  }

  if (response.status === 403) {
    const errorJson = await response.json().catch(() => null);
    const googleMsg = errorJson?.error?.message || '';
    if (googleMsg.toLowerCase().includes('not been used') || googleMsg.toLowerCase().includes('disabled')) {
      throw new Error('Gmail API is not enabled in your Google Cloud Project. Please enable it at https://console.cloud.google.com/apis/library/gmail.googleapis.com');
    }
    throw new Error(`Gmail API Access Forbidden (HTTP 403): ${googleMsg || 'Check that Gmail API is enabled and read permissions are granted.'}`);
  }

  if (!response.ok) {
    throw new Error(`Failed to fetch raw email from Gmail (HTTP ${response.status}).`);
  }

  const data = await response.json();

  if (!data.raw) {
    throw new Error('The Gmail API returned an empty raw email body.');
  }

  return data.raw;
}

export function extractMessageMetadata(message) {
  if (!message) return null;

  const headers = message.payload?.headers || [];
  const getHeader = (name) => {
    const found = headers.find((h) => h.name && h.name.toLowerCase() === name.toLowerCase());
    return found ? found.value : '';
  };

  return {
    id: message.id,
    threadId: message.threadId,
    snippet: message.snippet || '',
    subject: getHeader('Subject') || '(No Subject)',
    from: getHeader('From') || '(Unknown Sender)',
    to: getHeader('To') || '',
    date: getHeader('Date') || '',
    messageIdHeader: getHeader('Message-ID') || '',
    internalDate: message.internalDate ? parseInt(message.internalDate, 10) : Date.now()
  };
}
