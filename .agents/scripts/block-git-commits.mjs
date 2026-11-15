import fs from 'node:fs';

const DENY_RESPONSE = (reason) => JSON.stringify({
  decision: 'deny',
  reason: `BLOCKED: ${reason}`
});

const ALLOW_RESPONSE = JSON.stringify({ decision: 'allow' });

function exitWith(responseString) {
  process.stdout.write(responseString);
  process.exit(0);
}

try {
  const input = fs.readFileSync(0, 'utf-8');
  if (!input || input.trim().length === 0) {
    exitWith(DENY_RESPONSE('No input provided to hook. Denying tool execution for safety.'));
  }

  let payload;
  try {
    payload = JSON.parse(input);
  } catch (err) {
    exitWith(DENY_RESPONSE('Malformed JSON input in hook. Denying tool execution for safety.'));
  }

  const toolCall = payload.toolCall;
  if (!toolCall || typeof toolCall !== 'object') {
    exitWith(DENY_RESPONSE('Missing toolCall object in hook payload. Denying tool execution for safety.'));
  }

  const toolName = toolCall.name || '';

  if (toolName.startsWith('github_')) {
    const writeKeywords = ['create', 'update', 'push', 'delete', 'merge', 'commit', 'fork', 'write'];
    const isWriteTool = writeKeywords.some((kw) => toolName.toLowerCase().includes(kw));
    if (isWriteTool) {
      exitWith(DENY_RESPONSE(`GitHub write tool '${toolName}' is strictly prohibited for AI agents.`));
    }
  }

  if (toolName === 'run_command') {
    const args = toolCall.args;
    if (!args || typeof args !== 'object' || typeof args.CommandLine !== 'string') {
      exitWith(DENY_RESPONSE('Invalid or missing CommandLine in run_command tool call.'));
    }

    const commandLine = args.CommandLine;

    const subCommands = commandLine.split(/[;&|\n\r]+/).map((s) => s.trim()).filter(Boolean);

    for (const subCmd of subCommands) {
      const tokens = subCmd.match(/(?:[^\s"']+|"[^"]*"|'[^']*')+/g) || [];
      if (tokens.length === 0) {
        continue;
      }

      const cleanTokens = tokens.map((t) => t.replace(/^["']|["']$/g, '').trim());
      const firstToken = cleanTokens[0].toLowerCase();
      const baseCmd = firstToken.replace(/^.*[\\/]/, '');

      if (baseCmd === 'gh' || baseCmd === 'gh.exe') {
        const ghAction = cleanTokens.slice(1).join(' ').toLowerCase();
        const prohibitedGhActions = [
          'pr create', 'pr merge', 'pr close', 'repo create', 'repo fork',
          'repo delete', 'release create', 'release delete', 'issue create',
          'issue close', 'gist create'
        ];
        const isBlockedGh = prohibitedGhActions.some((p) => ghAction.includes(p));
        if (isBlockedGh) {
          exitWith(DENY_RESPONSE(`GitHub CLI write command ('${subCmd}') is strictly prohibited for AI agents.`));
        }
      }

      if (baseCmd === 'git' || baseCmd === 'git.exe') {
        let verbIndex = 1;
        while (verbIndex < cleanTokens.length) {
          const t = cleanTokens[verbIndex];
          if (t === '-C' || t === '--git-dir' || t === '--work-tree' || t === '-c') {
            verbIndex += 2;
          } else if (t.startsWith('-')) {
            verbIndex += 1;
          } else {
            break;
          }
        }

        if (verbIndex >= cleanTokens.length) {
          continue;
        }

        const gitVerb = cleanTokens[verbIndex].toLowerCase();
        const allowedVerbs = ['status', 'diff', 'log', 'show', 'rev-parse'];

        if (gitVerb === 'branch') {
          const branchArgs = cleanTokens.slice(verbIndex + 1);
          const isListing = branchArgs.length === 0 || branchArgs.every((a) => (
            a === '--show-current' || a === '--list' || a === '-a' ||
            a === '-r' || a === '--all' || a === '--remotes' || a === '-v'
          ));
          if (!isListing) {
            exitWith(DENY_RESPONSE(`Branch creation, modification, or deletion ('${subCmd}') is prohibited. Agents must never modify branches.`));
          }
          continue;
        }

        if (allowedVerbs.includes(gitVerb)) {
          continue;
        }

        const prohibitedVerbs = [
          'commit', 'push', 'checkout', 'switch', 'reset', 'clean',
          'restore', 'merge', 'rebase', 'cherry-pick', 'revert',
          'tag', 'rm', 'mv', 'stash', 'pull', 'clone', 'init'
        ];

        if (prohibitedVerbs.includes(gitVerb)) {
          exitWith(DENY_RESPONSE(
            `Git command '${gitVerb}' ('${subCmd}') is strictly prohibited for AI agents. ` +
            'The repository owner manually manages Git commits, pushes, and branch changes.'
          ));
        }

        exitWith(DENY_RESPONSE(
          `Unrecognized Git operation '${gitVerb}' ('${subCmd}') blocked for safety. ` +
          'Only read-only inspection commands (status, diff, log, show, branch --show-current) are permitted.'
        ));
      }
    }
  }

  exitWith(ALLOW_RESPONSE);
} catch (unexpectedError) {
  exitWith(DENY_RESPONSE(`Unexpected hook error: ${unexpectedError.message}. Denying for safety.`));
}
