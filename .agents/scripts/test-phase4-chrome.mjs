import { spawn } from 'child_process';
import fs from 'fs';

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
    '--window-size=1280,1400'
  ], {
    stdio: 'ignore'
  });

  let connected = false;
  let targetWsUrl = null;

  for (let i = 0; i < 20; i++) {
    await new Promise(r => setTimeout(r, 500));
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

  console.log('3. Navigating to http://localhost:5173/evidence/1...');
  await sendCommand('Page.navigate', { url: 'http://localhost:5173/evidence/1' });

  await new Promise(r => setTimeout(r, 2500));

  console.log('4. Inspecting DOM elements on Investigation page...');
  let evalRes = await sendCommand('Runtime.evaluate', {
    expression: `(() => {
      const bodyText = document.body.innerText;
      const h1 = document.querySelector('h1')?.innerText || '';
      const h2s = Array.from(document.querySelectorAll('h2')).map(h => h.innerText);
      const rows = Array.from(document.querySelectorAll('table tbody tr')).map(tr => {
        const cells = Array.from(tr.querySelectorAll('td')).map(td => td.innerText.trim());
        return {
          type: cells[0] || '',
          origin: cells[1] || '',
          value: cells[2] || '',
          verdict: cells[3] || ''
        };
      });

      return {
        h1,
        h2s,
        indicatorRowsCount: rows.length,
        hasThreatIndicatorsCard: h2s.some(h => h.includes('Extracted Threat Indicators')),
        sampleRows: rows.slice(0, 5),
        bodySnippet: bodyText.slice(0, 400)
      };
    })()`,
    returnByValue: true
  });

  console.log('DOM Evaluation Result:', JSON.stringify(evalRes.result.value, null, 2));

  console.log('5. Testing filter buttons...');
  evalRes = await sendCommand('Runtime.evaluate', {
    expression: `(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      const ipBtn = buttons.find(b => b.innerText.includes('Public IPs'));
      if (ipBtn) {
        ipBtn.click();
      }
      return { clicked: !!ipBtn };
    })()`,
    returnByValue: true
  });

  await new Promise(r => setTimeout(r, 500));

  evalRes = await sendCommand('Runtime.evaluate', {
    expression: `(() => {
      const rows = Array.from(document.querySelectorAll('table tbody tr')).map(tr => {
        const cells = Array.from(tr.querySelectorAll('td')).map(td => td.innerText.trim());
        return {
          type: cells[0] || '',
          origin: cells[1] || '',
          value: cells[2] || ''
        };
      });
      return { filteredRowCount: rows.length, rows };
    })()`,
    returnByValue: true
  });

  console.log('Filtered IPs Result:', JSON.stringify(evalRes.result.value, null, 2));

  console.log('6. Resetting filter to ALL...');
  await sendCommand('Runtime.evaluate', {
    expression: `(() => {
      const buttons = Array.from(document.querySelectorAll('button'));
      const allBtn = buttons.find(b => b.innerText.startsWith('All'));
      if (allBtn) allBtn.click();
    })()`,
    returnByValue: true
  });

  await new Promise(r => setTimeout(r, 500));

  console.log('7. Capturing full-page screenshot from Chrome...');
  const screenshotRes = await sendCommand('Page.captureScreenshot', { format: 'png' });
  const screenshotBuffer = Buffer.from(screenshotRes.data, 'base64');
  const screenshotPath = 'phase4-investigation-chrome.png';
  fs.writeFileSync(screenshotPath, screenshotBuffer);
  console.log('Screenshot saved to:', screenshotPath, `(${screenshotBuffer.length} bytes)`);

  ws.close();
  chromeProcess.kill();

  console.log('\n=============================================');
  console.log('PHASE 4 CHROME UI VERIFICATION COMPLETED SUCCESSFULLY!');
  console.log('=============================================');
}

runChromeTest().catch(err => {
  console.error('Test error:', err);
  process.exit(1);
});
