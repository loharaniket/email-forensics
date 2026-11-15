import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { createRequire } from 'module';

const require = createRequire(import.meta.url);
const { authenticate } = require('./node_modules/mailauth/dist/cjs/mailauth.js');

function normalizeStatus(statusVal) {
  if (!statusVal) return 'NOT_VERIFIABLE';
  const val = typeof statusVal === 'string' ? statusVal : statusVal.result;
  if (!val) return 'NOT_VERIFIABLE';
  const s = String(val).toUpperCase();
  if (s === 'PASS') return 'PASS';
  if (s === 'FAIL') return 'FAIL';
  if (s === 'NONE') return 'NONE';
  if (s === 'TEMPERROR') return 'TEMPERROR';
  if (s === 'PERMERROR') return 'PERMERROR';
  return 'NOT_VERIFIABLE';
}

function extractRecordedAuth(rawString) {
  const result = { spf: null, dkim: null, dmarc: null, arc: null };
  const arMatch = rawString.match(/Authentication-Results:\s*([^\r\n]+(?:\r?\n\s+[^\r\n]+)*)/i);
  if (arMatch) {
    const full = arMatch[1].replace(/\r?\n\s+/g, ' ');
    const parts = full.split(';');
    for (const part of parts.slice(1)) {
      const p = part.trim();
      const spfM = p.match(/^spf=([a-z]+)/i);
      if (spfM) result.spf = normalizeStatus(spfM[1]);
      const dkimM = p.match(/^dkim=([a-z]+)/i);
      if (dkimM) result.dkim = normalizeStatus(dkimM[1]);
      const dmarcM = p.match(/^dmarc=([a-z]+)/i);
      if (dmarcM) result.dmarc = normalizeStatus(dmarcM[1]);
      const arcM = p.match(/^arc=([a-z]+)/i);
      if (arcM) result.arc = normalizeStatus(arcM[1]);
    }
  }

  const spfMatch = rawString.match(/Received-SPF:\s*([a-z]+)/i);
  if (spfMatch && !result.spf) {
    result.spf = normalizeStatus(spfMatch[1]);
  }

  return result;
}

async function readInput() {
  const filePath = process.argv[2];
  if (filePath) {
    return fs.readFileSync(filePath);
  }
  return new Promise((resolve, reject) => {
    const chunks = [];
    process.stdin.on('data', chunk => chunks.push(chunk));
    process.stdin.on('end', () => resolve(Buffer.concat(chunks)));
    process.stdin.on('error', reject);
  });
}

async function main() {
  try {
    const rawBytes = await readInput();
    if (!rawBytes || rawBytes.length === 0) {
      console.log(JSON.stringify({
        spf: { status: 'NOT_VERIFIABLE', info: 'Empty email content' },
        dkim: { status: 'NOT_VERIFIABLE', info: 'Empty email content' },
        dmarc: { status: 'NOT_VERIFIABLE', info: 'Empty email content' },
        arc: { status: 'NOT_VERIFIABLE', info: 'Empty email content' },
        receivedChain: []
      }));
      return;
    }

    const authRes = await authenticate(rawBytes, { trustReceived: true });
    const rawString = rawBytes.toString('utf-8');
    const recordedAuth = extractRecordedAuth(rawString);

    let dkimStatus = 'NONE';
    let dkimDomain = null;
    let dkimSelector = null;
    let dkimAligned = false;
    let dkimInfo = '';

    if (authRes.dkim && authRes.dkim.results && authRes.dkim.results.length > 0) {
      const primaryDkim = authRes.dkim.results[0];
      dkimStatus = normalizeStatus(primaryDkim.status);
      dkimDomain = primaryDkim.signingDomain || (primaryDkim.status && primaryDkim.status.header ? primaryDkim.status.header.d : null);
      dkimSelector = primaryDkim.selector || (primaryDkim.status && primaryDkim.status.header ? primaryDkim.status.header.s : null);
      dkimAligned = !!primaryDkim.aligned;
      dkimInfo = primaryDkim.info || '';
    }

    if ((dkimStatus === 'NOT_VERIFIABLE' || dkimStatus === 'NONE') && recordedAuth.dkim) {
      dkimStatus = recordedAuth.dkim;
      dkimInfo = (dkimInfo ? dkimInfo + ' | ' : '') + 'Recorded MTA header: dkim=' + recordedAuth.dkim;
    }

    let spfStatus = 'NOT_VERIFIABLE';
    let spfDomain = null;
    let spfClientIp = null;
    let spfInfo = '';

    if (authRes.spf) {
      spfStatus = normalizeStatus(authRes.spf.status);
      spfDomain = authRes.spf.domain !== 'undefined' ? authRes.spf.domain : null;
      spfClientIp = authRes.spf['client-ip'] || null;
      spfInfo = authRes.spf.info || '';
      if (!spfClientIp && spfStatus === 'TEMPERROR') {
        spfStatus = 'NOT_VERIFIABLE';
      }
    }

    if ((spfStatus === 'NOT_VERIFIABLE' || spfStatus === 'TEMPERROR' || spfStatus === 'NONE') && recordedAuth.spf) {
      spfStatus = recordedAuth.spf;
      spfInfo = (spfInfo ? spfInfo + ' | ' : '') + 'Recorded MTA header: spf=' + recordedAuth.spf;
    }

    let dmarcStatus = 'NONE';
    let dmarcPolicy = 'none';
    let dmarcAligned = false;
    let dmarcDomain = null;
    let dmarcInfo = '';

    if (authRes.dmarc) {
      dmarcStatus = normalizeStatus(authRes.dmarc.status);
      dmarcPolicy = authRes.dmarc.policy || (authRes.dmarc.status && authRes.dmarc.status.policy ? authRes.dmarc.status.policy.dmarc : 'none');
      dmarcDomain = authRes.dmarc.domain || null;
      dmarcAligned = !!(authRes.dmarc.alignment && (authRes.dmarc.alignment.spf?.result || authRes.dmarc.alignment.dkim?.result));
      dmarcInfo = authRes.dmarc.info || '';
    }

    if ((dmarcStatus === 'NOT_VERIFIABLE' || dmarcStatus === 'NONE') && recordedAuth.dmarc) {
      dmarcStatus = recordedAuth.dmarc;
      dmarcInfo = (dmarcInfo ? dmarcInfo + ' | ' : '') + 'Recorded MTA header: dmarc=' + recordedAuth.dmarc;
    }

    let arcStatus = 'NONE';
    let arcInfo = '';

    if (authRes.arc) {
      arcStatus = normalizeStatus(authRes.arc.status);
      arcInfo = authRes.arc.authResults || '';
    }

    if ((arcStatus === 'NOT_VERIFIABLE' || arcStatus === 'NONE') && recordedAuth.arc) {
      arcStatus = recordedAuth.arc;
      arcInfo = (arcInfo ? arcInfo + ' | ' : '') + 'Recorded MTA header: arc=' + recordedAuth.arc;
    }

    const simplifiedChain = [];
    if (authRes.receivedChain && Array.isArray(authRes.receivedChain)) {
      for (const hop of authRes.receivedChain) {
        simplifiedChain.push({
          from: hop.from || null,
          by: hop.by || null,
          with: hop.with || null,
          date: hop.date ? hop.date.toISOString() : null,
          delaySec: typeof hop.delay === 'number' ? hop.delay : null
        });
      }
    }

    const output = {
      spf: {
        status: spfStatus,
        domain: spfDomain,
        clientIp: spfClientIp,
        info: spfInfo
      },
      dkim: {
        status: dkimStatus,
        domain: dkimDomain,
        selector: dkimSelector,
        aligned: dkimAligned,
        info: dkimInfo
      },
      dmarc: {
        status: dmarcStatus,
        domain: dmarcDomain,
        policy: dmarcPolicy,
        aligned: dmarcAligned,
        info: dmarcInfo
      },
      arc: {
        status: arcStatus,
        info: arcInfo
      },
      receivedChain: simplifiedChain
    };

    console.log(JSON.stringify(output));
  } catch (err) {
    console.log(JSON.stringify({
      spf: { status: 'NOT_VERIFIABLE', info: err.message },
      dkim: { status: 'NOT_VERIFIABLE', info: err.message },
      dmarc: { status: 'NOT_VERIFIABLE', info: err.message },
      arc: { status: 'NOT_VERIFIABLE', info: err.message },
      receivedChain: [],
      error: err.message
    }));
  }
}

main();
