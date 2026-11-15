import { spawn } from 'child_process';
import fs from 'fs';
import path from 'path';

const chromePath = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';
const profileDir = 'C:\\Users\\Aniket Lohar\\AppData\\Local\\Google\\Chrome\\DevToolsProfile';

if (!fs.existsSync(profileDir)) {
  fs.mkdirSync(profileDir, { recursive: true });
}

async function runChromeTest() {
  console.log('1. Starting Chrome with remote debugging on port 9222...');
  const chromeProcess = spawn(chromePath, [
    '--headless=new',
    '--remote-debugging-port=9222',
    '--user-data-dir=' + profileDir,
    '--remote-allow-origins=*',
    '--no-first-run',
    '--no-default-browser-check',
    '--window-size=1280,900'
  ], {
    stdio: 'ignore'
  });

  let connected = false;
  let targetWsUrl = null;

  for (let i = 0; i < 15; i++) {
    await new Promise(r => setTimeout(r, 600));
    try {
      const res = await fetch('http://127.0.0.1:9222/json/list');
      const tabs = await res.json();
      if (tabs && tabs.length > 0) {
        targetWsUrl = tabs[0].webSocketDebuggerUrl;
        connected = true;
        break;
      }
    } catch (e) {
    }
  }

  if (!connected || !targetWsUrl) {
    chromeProcess.kill();
    throw new Error('Failed to connect to Chrome on port 9222');
  }

  console.log('2. Connected to Chrome CDP target:', targetWsUrl);

  const ws = new WebSocket(targetWsUrl);
  let msgId = 1;
  const callbacks = new Map();

  function sendCommand(method, params = {}) {
    return new Promise((resolve, reject) => {
      const id = msgId++;
      callbacks.set(id, { resolve, reject });
      ws.send(JSON.stringify({ id, method, params }));
    });
  }

  await new Promise((resolve, reject) => {
    ws.onopen = resolve;
    ws.onerror = reject;
  });

  ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    if (data.id && callbacks.has(data.id)) {
      const cb = callbacks.get(data.id);
      callbacks.delete(data.id);
      if (data.error) {
        cb.reject(new Error(data.error.message || JSON.stringify(data.error)));
      } else {
        cb.resolve(data.result);
      }
    }
  };

  await sendCommand('Page.enable');
  await sendCommand('Runtime.enable');

  console.log('3. Navigating Chrome to http://localhost:5173/...');
  await sendCommand('Page.navigate', { url: 'http://localhost:5173/' });

  await new Promise(r => setTimeout(r, 2000));

  console.log('4. Checking initial dashboard DOM elements...');
  let evalRes = await sendCommand('Runtime.evaluate', {
    expression: `(() => {
      return {
        title: document.title,
        heading: document.querySelector('h1')?.innerText,
        uploadSection: !!document.querySelector('h2'),
        recentSection: document.querySelector('h2:nth-of-type(1)')?.innerText || '',
        bodyText: document.body.innerText.slice(0, 300)
      };
    })()`,
    returnByValue: true
  });
  console.log('Initial page state in Chrome:', evalRes.result.value);

  console.log('5. Simulating user selecting sample.eml in FileUpload component...');
  evalRes = await sendCommand('Runtime.evaluate', {
    expression: `(() => {
      const input = document.querySelector('input[type="file"]');
      if (!input) return { success: false, reason: 'File input not found' };

      const content = "From: hr-payroll@corporate-secure.org\\r\\nTo: employee@victim.com\\r\\nSubject: Updated November 2026 Salary Slip\\r\\nDate: Sun, 15 Nov 2026 14:00:00 +0000\\r\\n\\r\\nPlease find attached your updated salary structure.";
      const file = new File([content], 'November-Salary-Slip.eml', { type: 'message/rfc822' });

      const dataTransfer = new DataTransfer();
      dataTransfer.items.add(file);
      input.files = dataTransfer.files;

      input.dispatchEvent(new Event('change', { bubbles: true }));

      return {
        success: true,
        fileName: input.files[0]?.name,
        fileSize: input.files[0]?.size
      };
    })()`,
    returnByValue: true
  });
  console.log('File selection result:', evalRes.result.value);

  await new Promise(r => setTimeout(r, 600));

  console.log('6. Clicking "Upload & Preserve Evidence" button in Chrome...');
  evalRes = await sendCommand('Runtime.evaluate', {
    expression: `(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      const uploadBtn = buttons.find(b => b.innerText.includes('Upload & Preserve Evidence'));
      if (!uploadBtn) return { success: false, reason: 'Upload button not found' };

      uploadBtn.click();
      return { success: true };
    })()`,
    returnByValue: true
  });
  console.log('Click button result:', evalRes.result.value);

  console.log('7. Waiting for backend response and UI state update...');
  let uploadSuccess = false;
  let finalState = null;

  for (let i = 0; i < 20; i++) {
    await new Promise(r => setTimeout(r, 500));
    evalRes = await sendCommand('Runtime.evaluate', {
      expression: `(() => {
        const bodyText = document.body.innerText;
        const hasSuccess = bodyText.includes('Evidence Preserved Successfully');
        const codeElement = document.querySelector('code');
        const recentRecords = Array.from(document.querySelectorAll('div')).filter(d => d.innerText.includes('November-Salary-Slip.eml'));

        return {
          hasSuccess,
          sha256: codeElement ? codeElement.innerText : null,
          recentItemFound: recentRecords.length > 0
        };
      })()`,
      returnByValue: true
    });

    if (evalRes.result.value.hasSuccess) {
      uploadSuccess = true;
      finalState = evalRes.result.value;
      break;
    }
  }

  console.log('8. Upload and UI Verification Result:');
  console.log(finalState);

  console.log('9. Capturing full-page screenshot from Chrome...');
  const screenshotRes = await sendCommand('Page.captureScreenshot', { format: 'png' });
  const screenshotBuffer = Buffer.from(screenshotRes.data, 'base64');
  const screenshotPath = 'dashboard-chrome-verified.png';
  fs.writeFileSync(screenshotPath, screenshotBuffer);
  console.log('Screenshot saved to:', screenshotPath, `(${screenshotBuffer.length} bytes)`);

  ws.close();
  chromeProcess.kill();

  if (!uploadSuccess) {
    throw new Error('Test failed: Evidence Preserved Successfully banner did not appear in Chrome');
  }

  console.log('\n=============================================');
  console.log('CHROME FRONTEND VERIFICATION COMPLETED SUCCESSFULLY!');
  console.log('=============================================');
}

runChromeTest().catch(err => {
  console.error('Test error:', err);
  process.exit(1);
});
