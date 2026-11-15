import { spawnSync } from 'node:child_process';
import path from 'node:path';

const scriptPath = path.resolve('.agents/scripts/block-git-commits.mjs');

const testCases = [
  { name: 'npm test', input: { toolCall: { name: 'run_command', args: { CommandLine: 'npm test' } } }, expected: 'allow' },
  { name: 'mvn test', input: { toolCall: { name: 'run_command', args: { CommandLine: 'mvn clean test' } } }, expected: 'allow' },
  { name: 'git status', input: { toolCall: { name: 'run_command', args: { CommandLine: 'git status' } } }, expected: 'allow' },
  { name: 'git diff', input: { toolCall: { name: 'run_command', args: { CommandLine: 'git diff' } } }, expected: 'allow' },
  { name: 'git branch --show-current', input: { toolCall: { name: 'run_command', args: { CommandLine: 'git branch --show-current' } } }, expected: 'allow' },
  { name: 'git commit blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['git', 'commit'].join(' ') } } }, expected: 'deny' },
  { name: 'git push blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['git', 'push'].join(' ') } } }, expected: 'deny' },
  { name: 'git push force blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['git', 'push', '--force'].join(' ') } } }, expected: 'deny' },
  { name: 'chained commit blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['npm test && git', 'commit'].join(' ') } } }, expected: 'deny' },
  { name: 'git reset blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['git', 'reset', '--hard'].join(' ') } } }, expected: 'deny' },
  { name: 'git clean blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['git', 'clean', '-fd'].join(' ') } } }, expected: 'deny' },
  { name: 'git checkout blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['git', 'checkout', '.'].join(' ') } } }, expected: 'deny' },
  { name: 'git restore blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['git', 'restore', '.'].join(' ') } } }, expected: 'deny' },
  { name: 'git switch blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['git', 'switch', 'development'].join(' ') } } }, expected: 'deny' },
  { name: 'gh pr create blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: 'gh pr create' } } }, expected: 'deny' },
  { name: 'github mcp tool blocked', input: { toolCall: { name: 'github_create_pull_request', args: {} } }, expected: 'deny' },
  { name: 'empty input blocked', input: '', expected: 'deny' },
  { name: 'malformed json blocked', input: '{bad}', expected: 'deny' }
];

let allPassed = true;

for (const tc of testCases) {
  const inputStr = typeof tc.input === 'string' ? tc.input : JSON.stringify(tc.input);
  const result = spawnSync(process.execPath, [scriptPath], {
    input: inputStr,
    encoding: 'utf-8'
  });

  let decision = 'error';
  try {
    const parsed = JSON.parse(result.stdout);
    decision = parsed.decision;
  } catch (e) {
    decision = 'parse_error: ' + result.stdout;
  }

  if (decision === tc.expected) {
    console.log(`PASS: ${tc.name} -> ${decision}`);
  } else {
    console.log(`FAIL: ${tc.name} -> expected ${tc.expected}, got ${decision}`);
    allPassed = false;
  }
}

console.log(`ALL_TESTS_PASSED: ${allPassed}`);
if (!allPassed) process.exit(1);
