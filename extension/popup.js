document.addEventListener('DOMContentLoaded', async () => {
  const googleDot = document.getElementById('google-dot');
  const googleText = document.getElementById('google-text');
  const backendDot = document.getElementById('backend-dot');
  const backendText = document.getElementById('backend-text');

  const tabHeading = document.getElementById('tab-heading');
  const tabDesc = document.getElementById('tab-desc');
  const actionBtn = document.getElementById('action-btn');
  const openDashboardBtn = document.getElementById('open-dashboard-btn');
  const openOptionsLink = document.getElementById('open-options-link');

  let dashboardUrl = 'http://localhost:5173';

  chrome.runtime.sendMessage({ action: 'GET_CONFIG' }, (config) => {
    if (config?.dashboardUrl) {
      dashboardUrl = config.dashboardUrl;
    }
  });

  chrome.runtime.sendMessage({ action: 'GET_AUTH_STATUS' }, (res) => {
    if (res?.signedIn) {
      googleDot.className = 'status-dot online';
      googleText.className = 'status-val online';
      googleText.textContent = 'Connected';
    } else if (res?.configured === false) {
      googleDot.className = 'status-dot offline';
      googleText.className = 'status-val offline';
      googleText.textContent = 'Setup Required';
      googleText.style.cursor = 'pointer';
      googleText.title = 'Click to configure Google OAuth in Extension Settings';
      googleText.addEventListener('click', () => {
        if (chrome.runtime.openOptionsPage) {
          chrome.runtime.openOptionsPage();
        } else {
          window.open(chrome.runtime.getURL('options/options.html'));
        }
      });
    } else {
      googleDot.className = 'status-dot offline';
      googleText.className = 'status-val offline';
      googleText.textContent = 'Not Signed In';
      googleText.style.cursor = 'pointer';
      googleText.title = 'Click to sign in';
      googleText.addEventListener('click', () => {
        chrome.runtime.sendMessage({ action: 'SIGN_IN' }, (signInRes) => {
          if (signInRes?.success) {
            googleDot.className = 'status-dot online';
            googleText.className = 'status-val online';
            googleText.textContent = 'Connected';
          } else {
            alert(signInRes?.error || 'Sign-in failed.');
          }
        });
      });
    }
  });

  chrome.runtime.sendMessage({ action: 'GET_BACKEND_STATUS' }, (res) => {
    if (res?.ok) {
      backendDot.className = 'status-dot online';
      backendText.className = 'status-val online';
      backendText.textContent = 'Online (8080)';
    } else {
      backendDot.className = 'status-dot offline';
      backendText.className = 'status-val offline';
      backendText.textContent = 'Offline';
    }
  });

  chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
    const activeTab = tabs && tabs[0];
    const url = activeTab?.url || '';

    if (url.startsWith('https://mail.google.com/')) {
      tabHeading.textContent = 'Gmail Active';

      const hash = url.split('#')[1] || '';
      const isConversation = /#(?:[^\/]+\/)+(?:[^\/]+\/)?([a-zA-Z0-9_\-]+)$/.test('#' + hash);

      if (isConversation) {
        tabDesc.textContent = 'Email conversation detected. Ready to run forensic pipeline.';
        actionBtn.style.display = 'flex';
        actionBtn.innerHTML = '<span>Analyze Open Email</span>';
        actionBtn.addEventListener('click', () => {
          chrome.tabs.sendMessage(activeTab.id, { action: 'ANALYZE_TRIGGER' }).catch(() => {});
          window.close();
        });
      } else {
        tabDesc.textContent = 'Open an email conversation in Gmail to run digital forensics.';
        actionBtn.style.display = 'none';
      }
    } else {
      tabHeading.textContent = 'Gmail Inactive';
      tabDesc.textContent = 'Open Google Mail to analyze incoming emails with digital forensics.';
      actionBtn.style.display = 'flex';
      actionBtn.innerHTML = '<span>Open Gmail</span>';
      actionBtn.addEventListener('click', () => {
        chrome.tabs.create({ url: 'https://mail.google.com/' });
        window.close();
      });
    }
  });

  openDashboardBtn.addEventListener('click', () => {
    chrome.tabs.create({ url: dashboardUrl });
  });

  openOptionsLink.addEventListener('click', () => {
    if (chrome.runtime.openOptionsPage) {
      chrome.runtime.openOptionsPage();
    } else {
      window.open(chrome.runtime.getURL('options/options.html'));
    }
  });
});
