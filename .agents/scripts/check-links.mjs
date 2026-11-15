import fs from 'fs';
import path from 'path';

function getMdFiles(dir) {
  let results = [];
  const list = fs.readdirSync(dir);
  for (const file of list) {
    const full = path.join(dir, file);
    const stat = fs.statSync(full);
    if (stat.isDirectory()) {
      results = results.concat(getMdFiles(full));
    } else if (file.endsWith('.md')) {
      results.push(full);
    }
  }
  return results;
}

const files = [...getMdFiles('.agents'), ...getMdFiles('docs'), path.resolve('AGENTS.md')];
let broken = 0;

for (const file of files) {
  const content = fs.readFileSync(file, 'utf8');
  const regex = /\[([^\]]+)\]\(([^)]+)\)/g;
  let match;
  while ((match = regex.exec(content)) !== null) {
    let link = match[2].trim();
    if (link.startsWith('http://') || link.startsWith('https://') || link.startsWith('#') || link.startsWith('mailto:') || link.startsWith('<')) {
      continue;
    }
    link = link.split('#')[0];
    if (!link) {
      continue;
    }
    const dir = path.dirname(file);
    const target = path.resolve(dir, link);
    if (!fs.existsSync(target)) {
      console.log('BROKEN:', file, '->', link);
      broken++;
    }
  }
}

console.log('TOTAL_BROKEN_LINKS:', broken);
