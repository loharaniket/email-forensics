import { spawnSync } from 'node:child_process';
import path from 'node:path';

const scriptPath = path.resolve('.agents/scripts/block-git-commits.mjs');

const testCases = [
  { name: 'npm test', input: { toolCall: { name: 'run_command', args: { CommandLine: 'npm test' } } }, expected: 'allow' },
  { name: 'npm run build', input: { toolCall: { name: 'run_command', args: { CommandLine: 'npm run build' } } }, expected: 'allow' },
  { name: 'mvn test', input: { toolCall: { name: 'run_command', args: { CommandLine: 'mvn clean test' } } }, expected: 'allow' },
  { name: 'git status', input: { toolCall: { name: 'run_command', args: { CommandLine: 'git status' } } }, expected: 'allow' },
  { name: 'git diff', input: { toolCall: { name: 'run_command', args: { CommandLine: 'git diff' } } }, expected: 'allow' },
  { name: 'git log', input: { toolCall: { name: 'run_command', args: { CommandLine: 'git log -n 5' } } }, expected: 'allow' },
  { name: 'git show', input: { toolCall: { name: 'run_command', args: { CommandLine: 'git show HEAD' } } }, expected: 'allow' },
  { name: 'git branch --show-current', input: { toolCall: { name: 'run_command', args: { CommandLine: 'git branch --show-current' } } }, expected: 'allow' },
  { name: 'git branch --list', input: { toolCall: { name: 'run_command', args: { CommandLine: 'git branch --list' } } }, expected: 'allow' },
  { name: 'git branch -a', input: { toolCall: { name: 'run_command', args: { CommandLine: 'git branch -a' } } }, expected: 'allow' },
  { name: 'gh pr view', input: { toolCall: { name: 'run_command', args: { CommandLine: 'gh pr view 12' } } }, expected: 'allow' },
  { name: 'gh repo view', input: { toolCall: { name: 'run_command', args: { CommandLine: 'gh repo view' } } }, expected: 'allow' },
  { name: 'gh api GET', input: { toolCall: { name: 'run_command', args: { CommandLine: 'gh api repos/owner/repo' } } }, expected: 'allow' },

  { name: 'git commit blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['git', 'commit', '-m', 'test'].join(' ') } } }, expected: 'deny' },
  { name: 'git push blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['git', 'push', 'origin', 'development'].join(' ') } } }, expected: 'deny' },
  { name: 'git push force blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['git', 'push', '--force'].join(' ') } } }, expected: 'deny' },
  { name: 'chained commit blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['npm test && git', 'commit'].join(' ') } } }, expected: 'deny' },
  { name: 'git reset blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['git', 'reset', '--hard'].join(' ') } } }, expected: 'deny' },
  { name: 'git clean blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['git', 'clean', '-fd'].join(' ') } } }, expected: 'deny' },
  { name: 'git checkout blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['git', 'checkout', '.'].join(' ') } } }, expected: 'deny' },
  { name: 'git checkout branch blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['git', 'checkout', '-b', 'feature/test'].join(' ') } } }, expected: 'deny' },
  { name: 'git restore blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['git', 'restore', '.'].join(' ') } } }, expected: 'deny' },
  { name: 'git switch blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['git', 'switch', 'development'].join(' ') } } }, expected: 'deny' },
  { name: 'git branch create blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['git', 'branch', 'new-feature'].join(' ') } } }, expected: 'deny' },
  { name: 'git branch delete blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['git', 'branch', '-D', 'feature'].join(' ') } } }, expected: 'deny' },
  { name: 'git branch rename blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['git', 'branch', '-m', 'old', 'new'].join(' ') } } }, expected: 'deny' },

  { name: 'gh pr edit blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: 'gh pr edit 1 --title "Update"' } } }, expected: 'deny' },
  { name: 'gh pr create blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: 'gh pr create --title "Test"' } } }, expected: 'deny' },
  { name: 'gh pr merge blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: 'gh pr merge 1' } } }, expected: 'deny' },
  { name: 'gh api POST blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: 'gh api --method POST /repos/test/issues' } } }, expected: 'deny' },
  { name: 'gh api -X POST blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: 'gh api -X POST /repos/test/issues' } } }, expected: 'deny' },
  { name: 'gh api field mutation blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: 'gh api repos/test/issues -f title=bug' } } }, expected: 'deny' },

  { name: 'powershell commit wrapper blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['powershell', '-Command', '"git commit -m test"'].join(' ') } } }, expected: 'deny' },
  { name: 'cmd push wrapper blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['cmd', '/c', '"git push"'].join(' ') } } }, expected: 'deny' },
  { name: 'bash reset wrapper blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['bash', '-c', '"git reset --hard"'].join(' ') } } }, expected: 'deny' },
  { name: 'sh checkout wrapper blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: ['sh', '-c', '"git checkout ."'].join(' ') } } }, expected: 'deny' },
  { name: 'encoded shell command blocked', input: { toolCall: { name: 'run_command', args: { CommandLine: 'powershell -EncodedCommand aGVsbG8=' } } }, expected: 'deny' },

  { name: 'github_create_pull_request blocked', input: { toolCall: { name: 'github_create_pull_request', args: {} } }, expected: 'deny' },
  { name: 'namespaced github:create_pull_request blocked', input: { toolCall: { name: 'github:create_pull_request', args: {} } }, expected: 'deny' },
  { name: 'namespaced mcp__github__create_branch blocked', input: { toolCall: { name: 'mcp__github__create_branch', args: {} } }, expected: 'deny' },
  { name: 'namespaced github-mcp:push_code blocked', input: { toolCall: { name: 'github-mcp:push_code', args: {} } }, expected: 'deny' },
  { name: 'namespaced github_delete_repo blocked', input: { toolCall: { name: 'github_delete_repo', args: {} } }, expected: 'deny' },
  { name: 'namespaced github_unknown_mutation blocked', input: { toolCall: { name: 'github_unknown_mutation', args: {} } }, expected: 'deny' },
  { name: 'namespaced github:list_issues allowed', input: { toolCall: { name: 'github:list_issues', args: {} } }, expected: 'allow' },
  { name: 'namespaced github_get_pull_request allowed', input: { toolCall: { name: 'github_get_pull_request', args: {} } }, expected: 'allow' },

  { name: 'empty input blocked', input: '', expected: 'deny' },
  { name: 'malformed json blocked', input: '{bad_json', expected: 'deny' }
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
    console.log(`FAIL: ${tc.name} -> expected ${tc.expected}, got ${decision} (${result.stdout.trim()})`);
    allPassed = false;
  }
}

console.log('ALL_TESTS_PASSED:', allPassed);
process.exit(allPassed ? 0 : 1);
