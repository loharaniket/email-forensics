import fs from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';

let allValid = true;

const hooksContent = fs.readFileSync('.agents/hooks.json', 'utf8');
try {
  JSON.parse(hooksContent);
  console.log('PASS: .agents/hooks.json is valid JSON');
} catch (e) {
  console.log('FAIL: .agents/hooks.json JSON error:', e.message);
  allValid = false;
}

const jsFiles = [
  '.agents/scripts/block-git-commits.mjs',
  '.agents/scripts/test-git-hook.mjs',
  '.agents/scripts/check-links.mjs'
];

for (const js of jsFiles) {
  const res = spawnSync(process.execPath, ['--check', js], { encoding: 'utf8' });
  if (res.status === 0) {
    console.log(`PASS: ${js} syntax check`);
  } else {
    console.log(`FAIL: ${js} syntax error:`, res.stderr);
    allValid = false;
  }
}

function parseSimpleYaml(raw) {
  const lines = raw.split('\n');
  const result = {};
  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    const colonIdx = trimmed.indexOf(':');
    if (colonIdx > 0) {
      const key = trimmed.slice(0, colonIdx).trim();
      const val = trimmed.slice(colonIdx + 1).trim();
      result[key] = val;
    }
  }
  return result;
}

function checkFrontmatters(dir) {
  const entries = fs.readdirSync(dir);
  for (const entry of entries) {
    const full = path.join(dir, entry);
    if (fs.statSync(full).isDirectory()) {
      checkFrontmatters(full);
    } else if (entry.endsWith('.md')) {
      const content = fs.readFileSync(full, 'utf8');
      if (content.startsWith('---')) {
        const parts = content.split('---');
        if (parts.length >= 3) {
          const rawYaml = parts[1];
          const parsed = parseSimpleYaml(rawYaml);
          if (Object.keys(parsed).length > 0) {
            console.log(`PASS: YAML frontmatter in ${full}`);
          } else {
            console.log(`FAIL: Empty YAML frontmatter in ${full}`);
            allValid = false;
          }
        }
      }
    }
  }
}

checkFrontmatters('.agents/agents');
checkFrontmatters('.agents/rules');
checkFrontmatters('.agents/skills');

console.log('ALL_FILES_VALID:', allValid);
process.exit(allValid ? 0 : 1);
