document.addEventListener('DOMContentLoaded', () => {
  const backendInput = document.getElementById('backend-url-input');
  const dashboardInput = document.getElementById('dashboard-url-input');
  const authMethodSelect = document.getElementById('auth-method-select');
  const clientIdInput = document.getElementById('client-id-input');

  const extIdInput = document.getElementById('extension-id-display');
  const redirectUriInput = document.getElementById('redirect-uri-display');
  const manifestClientInput = document.getElementById('manifest-client-id-display');

  const googleDot = document.getElementById('google-status-dot');
  const googleTitle = document.getElementById('google-status-title');
  const googleDesc = document.getElementById('google-status-desc');
  const testResultBox = document.getElementById('backend-test-result');
  const saveStatusMsg = document.getElementById('save-status-msg');

  const testBackendBtn = document.getElementById('test-backend-btn');
  const copyExtIdBtn = document.getElementById('copy-ext-id-btn');
  const copyRedirectUriBtn = document.getElementById('copy-redirect-uri-btn');
  const clearCacheBtn = document.getElementById('clear-cache-btn');
  const googleAuthBtn = document.getElementById('google-auth-btn');
  const saveBtn = document.getElementById('save-settings-btn');

  if (chrome.runtime?.id) {
    extIdInput.value = chrome.runtime.id;
    redirectUriInput.value = `https://${chrome.runtime.id}.chromiumapp.org/`;
  }

  const manifest = chrome.runtime?.getManifest ? chrome.runtime.getManifest() : null;
  if (manifest?.oauth2?.client_id) {
    manifestClientInput.textContent = manifest.oauth2.client_id;
  }

  chrome.runtime.sendMessage({ action: 'GET_CONFIG' }, (config) => {
    if (!config) return;
    backendInput.value = config.backendUrl || 'http://localhost:8080';
    dashboardInput.value = config.dashboardUrl || 'http://localhost:5173';
    if (authMethodSelect && config.authMethod) {
      authMethodSelect.value = config.authMethod;
    }
    if (clientIdInput && config.googleClientId) {
      clientIdInput.value = config.googleClientId;
    }
  });

  checkGoogleStatus();

  if (copyExtIdBtn) {
    copyExtIdBtn.addEventListener('click', () => {
      navigator.clipboard.writeText(extIdInput.value).then(() => {
        copyExtIdBtn.textContent = 'Copied!';
        setTimeout(() => { copyExtIdBtn.textContent = 'Copy ID'; }, 2000);
      });
    });
  }

  if (copyRedirectUriBtn) {
    copyRedirectUriBtn.addEventListener('click', () => {
      navigator.clipboard.writeText(redirectUriInput.value).then(() => {
        copyRedirectUriBtn.textContent = 'Copied!';
        setTimeout(() => { copyRedirectUriBtn.textContent = 'Copy URI'; }, 2000);
      });
    });
  }

  if (clearCacheBtn) {
    clearCacheBtn.addEventListener('click', () => {
      clearCacheBtn.disabled = true;
      clearCacheBtn.textContent = 'Clearing...';
      chrome.runtime.sendMessage({ action: 'CLEAR_AUTH_CACHE' }, (res) => {
        clearCacheBtn.disabled = false;
        clearCacheBtn.textContent = 'Reset Sign-in';
        if (res?.success) {
          alert('Sign-in cache cleared. You can now sign in again.');
          checkGoogleStatus();
        }
      });
    });
  }

  testBackendBtn.addEventListener('click', () => {
    const targetUrl = (backendInput.value || 'http://localhost:8080').trim().replace(/\/+$/, '');
    testBackendBtn.disabled = true;
    testBackendBtn.textContent = 'Testing...';
    testResultBox.style.display = 'none';

    chrome.runtime.sendMessage({
      action: 'GET_BACKEND_STATUS',
      payload: { backendUrl: targetUrl }
    }, (res) => {
      testBackendBtn.disabled = false;
      testBackendBtn.textContent = 'Test Connection';
      testResultBox.style.display = 'block';

      if (res?.ok) {
        testResultBox.className = 'test-result-box success';
        testResultBox.textContent = `✓ Connected to backend at ${targetUrl}`;
      } else {
        testResultBox.className = 'test-result-box error';
        testResultBox.textContent = `❌ Cannot reach backend at ${targetUrl}. Make sure the server is running.`;
      }
    });
  });

  googleAuthBtn.addEventListener('click', () => {
    const isOnline = googleDot.classList.contains('online');

    if (isOnline) {
      googleAuthBtn.disabled = true;
      googleAuthBtn.textContent = 'Signing out...';
      chrome.runtime.sendMessage({ action: 'SIGN_OUT' }, () => {
        googleAuthBtn.disabled = false;
        checkGoogleStatus();
      });
    } else {
      const method = authMethodSelect ? authMethodSelect.value : 'web';
      googleAuthBtn.disabled = true;
      googleAuthBtn.textContent = 'Connecting...';
      chrome.runtime.sendMessage({
        action: 'SIGN_IN',
        payload: { authMethod: method }
      }, (res) => {
        googleAuthBtn.disabled = false;
        if (res?.success) {
          checkGoogleStatus();
        } else {
          alert(`Sign-in message: ${res?.error || 'Sign-in was not completed.'}`);
          checkGoogleStatus();
        }
      });
    }
  });

  function checkGoogleStatus() {
    chrome.runtime.sendMessage({ action: 'GET_AUTH_STATUS' }, (res) => {
      if (res?.signedIn) {
        googleDot.className = 'status-dot online';
        googleTitle.textContent = 'Connected to Google';
        googleDesc.textContent = 'Gmail access is active and ready.';
        googleAuthBtn.textContent = 'Sign Out';
        googleAuthBtn.className = 'btn btn-secondary';
      } else {
        googleDot.className = 'status-dot offline';
        googleTitle.textContent = 'Not Signed In';
        googleDesc.textContent = res?.error || 'Click Sign In with Google to connect your account.';
        googleAuthBtn.textContent = 'Sign In with Google';
        googleAuthBtn.className = 'btn btn-primary';
      }
    });
  }

  saveBtn.addEventListener('click', () => {
    const backendUrl = (backendInput.value || 'http://localhost:8080').trim().replace(/\/+$/, '');
    const dashboardUrl = (dashboardInput.value || 'http://localhost:5173').trim().replace(/\/+$/, '');

    try {
      new URL(backendUrl);
      new URL(dashboardUrl);
    } catch (_) {
      showStatus('Please enter valid URLs (starting with http:// or https://)', false);
      return;
    }

    saveBtn.disabled = true;
    saveBtn.textContent = 'Saving...';

    chrome.runtime.sendMessage({
      action: 'SET_CONFIG',
      payload: {
        backendUrl,
        dashboardUrl,
        googleClientId: (clientIdInput?.value || '').trim(),
        authMethod: authMethodSelect?.value || 'web'
      }
    }, (res) => {
      saveBtn.disabled = false;
      saveBtn.textContent = 'Save Settings';

      if (res?.success) {
        showStatus('✓ Settings saved successfully!', true);
        checkGoogleStatus();
      } else {
        showStatus('❌ Failed to save settings.', false);
      }
    });
  });

  function showStatus(text, isSuccess) {
    saveStatusMsg.style.display = 'block';
    saveStatusMsg.className = `save-status ${isSuccess ? 'success' : 'error'}`;
    saveStatusMsg.textContent = text;
    setTimeout(() => {
      saveStatusMsg.style.display = 'none';
    }, 3000);
  }
});
