import fs from 'node:fs';

const DENY = (reason) => JSON.stringify({
  decision: 'deny',
  reason: `BLOCKED: ${reason}`
});

const ALLOW = JSON.stringify({ decision: 'allow' });

function exitWith(response) {
  process.stdout.write(response);
  process.exit(0);
}

function splitCommands(line) {
  const result = [];
  let current = '';
  let inSingleQuote = false;
  let inDoubleQuote = false;

  for (let i = 0; i < line.length; i++) {
    const ch = line[i];
    const prev = i > 0 ? line[i - 1] : '';

    if (ch === "'" && !inDoubleQuote && prev !== '\\') {
      inSingleQuote = !inSingleQuote;
      current += ch;
    } else if (ch === '"' && !inSingleQuote && prev !== '\\') {
      inDoubleQuote = !inDoubleQuote;
      current += ch;
    } else if (!inSingleQuote && !inDoubleQuote) {
      if (ch === ';' || ch === '\n' || ch === '\r') {
        if (current.trim()) result.push(current.trim());
        current = '';
      } else if (ch === '&' && line[i + 1] === '&') {
        if (current.trim()) result.push(current.trim());
        current = '';
        i++;
      } else if (ch === '|' && line[i + 1] === '|') {
        if (current.trim()) result.push(current.trim());
        current = '';
        i++;
      } else if (ch === '|' || ch === '&') {
        if (current.trim()) result.push(current.trim());
        current = '';
      } else {
        current += ch;
      }
    } else {
      current += ch;
    }
  }

  if (current.trim()) {
    result.push(current.trim());
  }

  return result;
}

function tokenize(cmdStr) {
  const tokens = [];
  const regex = /(?:[^\s"']+|"[^"]*"|'[^']*')+/g;
  let match;
  while ((match = regex.exec(cmdStr)) !== null) {
    tokens.push(match[0]);
  }
  return tokens.map(t => t.replace(/^["']|["']$/g, '').trim());
}

function checkGitCommand(tokens, rawCmd) {
  let verbIndex = 1;
  while (verbIndex < tokens.length) {
    const t = tokens[verbIndex];
    if (t === '-C' || t === '--git-dir' || t === '--work-tree' || t === '-c' || t === '--namespace') {
      verbIndex += 2;
    } else if (t.startsWith('-')) {
      verbIndex += 1;
    } else {
      break;
    }
  }

  if (verbIndex >= tokens.length) {
    return { ok: true };
  }

  const gitVerb = tokens[verbIndex].toLowerCase();
  const allowedReadOnlyVerbs = ['status', 'diff', 'log', 'show', 'rev-parse', 'describe', 'version'];

  if (gitVerb === 'branch') {
    const branchArgs = tokens.slice(verbIndex + 1);
    const safeBranchFlags = new Set(['--show-current', '--list', '-a', '-r', '--all', '--remotes', '-v', '-vv']);
    const isSafe = branchArgs.every(a => safeBranchFlags.has(a) || a.startsWith('--list'));
    if (!isSafe || branchArgs.length === 0 && tokens.length > verbIndex + 1) {
      const hasMutationFlag = branchArgs.some(a => ['-d', '-D', '-m', '-M', '-c', '-C', '--delete', '--move', '--copy'].includes(a));
      if (hasMutationFlag) {
        return { ok: false, reason: `Git branch deletion or modification ('${rawCmd}') is strictly prohibited.` };
      }
      return { ok: false, reason: `Git branch creation or modification ('${rawCmd}') is strictly prohibited. Agents must never modify branches.` };
    }
    return { ok: true };
  }

  if (allowedReadOnlyVerbs.includes(gitVerb)) {
    return { ok: true };
  }

  const prohibitedVerbs = [
    'commit', 'push', 'checkout', 'switch', 'reset', 'clean',
    'restore', 'merge', 'rebase', 'cherry-pick', 'revert',
    'tag', 'rm', 'mv', 'stash', 'pull', 'clone', 'init', 'add', 'stage'
  ];

  if (prohibitedVerbs.includes(gitVerb)) {
    return {
      ok: false,
      reason: `Git command '${gitVerb}' ('${rawCmd}') is strictly prohibited for AI agents. The repository owner manually manages Git commits, pushes, and branch changes.`
    };
  }

  return {
    ok: false,
    reason: `Unrecognized Git operation '${gitVerb}' ('${rawCmd}') blocked for safety. Only read-only inspection commands are permitted.`
  };
}

function checkGhCommand(tokens, rawCmd) {
  let actionTokens = tokens.slice(1);
  if (actionTokens.length === 0) {
    return { ok: false, reason: 'GitHub CLI command missing action. Blocked under deny-by-default policy.' };
  }

  while (actionTokens.length > 0 && actionTokens[0].startsWith('-')) {
    const flag = actionTokens[0];
    if (flag === '--version' || flag === '-v') {
      return { ok: true };
    }
    if (flag === '--help' || flag === '-h') {
      return { ok: true };
    }
    if (flag === '--repo' || flag === '-R') {
      actionTokens = actionTokens.slice(2);
    } else {
      actionTokens = actionTokens.slice(1);
    }
  }

  if (actionTokens.length === 0) {
    return { ok: true };
  }

  const primary = actionTokens[0].toLowerCase();

  if (primary === 'version' || primary === 'help' || primary === 'status') {
    return { ok: true };
  }

  if (primary === 'pr') {
    const subAction = actionTokens[1] ? actionTokens[1].toLowerCase() : '';
    const allowedPr = ['view', 'list', 'status', 'diff', 'checks'];
    if (allowedPr.includes(subAction)) {
      return { ok: true };
    }
    return { ok: false, reason: `GitHub CLI PR action '${subAction || primary}' ('${rawCmd}') is prohibited. Only read-only actions (${allowedPr.join(', ')}) are permitted.` };
  }

  if (primary === 'issue') {
    const subAction = actionTokens[1] ? actionTokens[1].toLowerCase() : '';
    const allowedIssue = ['view', 'list', 'status'];
    if (allowedIssue.includes(subAction)) {
      return { ok: true };
    }
    return { ok: false, reason: `GitHub CLI issue action '${subAction || primary}' ('${rawCmd}') is prohibited. Only read-only actions are permitted.` };
  }

  if (primary === 'repo') {
    const subAction = actionTokens[1] ? actionTokens[1].toLowerCase() : '';
    const allowedRepo = ['view', 'list'];
    if (allowedRepo.includes(subAction)) {
      return { ok: true };
    }
    return { ok: false, reason: `GitHub CLI repo action '${subAction || primary}' ('${rawCmd}') is prohibited. Only read-only actions are permitted.` };
  }

  if (primary === 'release') {
    const subAction = actionTokens[1] ? actionTokens[1].toLowerCase() : '';
    const allowedRelease = ['view', 'list', 'download'];
    if (allowedRelease.includes(subAction)) {
      return { ok: true };
    }
    return { ok: false, reason: `GitHub CLI release action '${subAction || primary}' ('${rawCmd}') is prohibited. Only read-only actions are permitted.` };
  }

  if (primary === 'run') {
    const subAction = actionTokens[1] ? actionTokens[1].toLowerCase() : '';
    const allowedRun = ['view', 'list', 'watch'];
    if (allowedRun.includes(subAction)) {
      return { ok: true };
    }
    return { ok: false, reason: `GitHub CLI run action '${subAction || primary}' ('${rawCmd}') is prohibited. Only read-only actions are permitted.` };
  }

  if (primary === 'search') {
    return { ok: true };
  }

  if (primary === 'api') {
    const joined = actionTokens.join(' ').toLowerCase();
    const mutationKeywords = [
      '--method post', '--method put', '--method patch', '--method delete',
      '-x post', '-x put', '-x patch', '-x delete',
      '--method=post', '--method=put', '--method=patch', '--method=delete',
      '-f ', '--field ', '-f=', '--field=', '--raw-field', '--input'
    ];

    const hasMutation = mutationKeywords.some(kw => joined.includes(kw));
    if (hasMutation) {
      return { ok: false, reason: `GitHub API write request ('${rawCmd}') is strictly prohibited for AI agents.` };
    }
    return { ok: true };
  }

  return { ok: false, reason: `GitHub CLI command '${primary}' ('${rawCmd}') is blocked under deny-by-default policy. Only safe inspection commands are permitted.` };
}

function validateSingleCommand(cmdStr) {
  const tokens = tokenize(cmdStr);
  if (tokens.length === 0) {
    return { ok: true };
  }

  const baseToken = tokens[0].toLowerCase().replace(/^.*[\\/]/, '');

  const shellNames = ['bash', 'sh', 'zsh', 'ksh', 'csh', 'cmd', 'cmd.exe', 'powershell', 'powershell.exe', 'pwsh', 'pwsh.exe'];
  if (shellNames.includes(baseToken)) {
    const joined = tokens.slice(1).join(' ');
    const lowerJoined = joined.toLowerCase();

    if (lowerJoined.includes('-encodedcommand') || lowerJoined.includes('-e ') || lowerJoined.includes('frombase64')) {
      return { ok: false, reason: `Encoded or obfuscated shell command ('${cmdStr}') is prohibited.` };
    }

    let commandPayload = '';
    for (let i = 1; i < tokens.length; i++) {
      const t = tokens[i];
      const lowerT = t.toLowerCase();
      if ((lowerT === '-c' || lowerT === '/c' || lowerT === '-command') && i + 1 < tokens.length) {
        commandPayload = tokens.slice(i + 1).join(' ');
        break;
      }
    }

    if (commandPayload) {
      const nestedSubCmds = splitCommands(commandPayload);
      for (const nested of nestedSubCmds) {
        const nestedCheck = validateSingleCommand(nested);
        if (!nestedCheck.ok) {
          return nestedCheck;
        }
      }
      return { ok: true };
    }

    const containsGitOrGh = tokens.some(t => {
      const lt = t.toLowerCase();
      return lt === 'git' || lt.startsWith('git ') || lt === 'gh' || lt.startsWith('gh ');
    });
    if (containsGitOrGh) {
      return { ok: false, reason: `Unverified Git invocation inside shell wrapper ('${cmdStr}') is blocked for safety.` };
    }

    return { ok: true };
  }

  if (baseToken === 'git' || baseToken === 'git.exe') {
    return checkGitCommand(tokens, cmdStr);
  }

  if (baseToken === 'gh' || baseToken === 'gh.exe') {
    return checkGhCommand(tokens, cmdStr);
  }

  const hasSubshellGit = cmdStr.match(/\$\([^)]*(?:git|gh)[^)]*\)|`[^`]*(?:git|gh)[^`]*`/i);
  if (hasSubshellGit) {
    return { ok: false, reason: `Subshell expansion with Git or GitHub CLI ('${cmdStr}') is prohibited.` };
  }

  return { ok: true };
}

function checkGitHubTool(toolName) {
  const lowerName = toolName.toLowerCase();
  const writeVerbs = [
    'create', 'update', 'push', 'delete', 'merge', 'commit',
    'fork', 'write', 'edit', 'patch', 'publish', 'release',
    'tag', 'branch', 'rebase', 'cherry_pick', 'revert', 'close',
    'reopen', 'assign', 'add', 'remove', 'sync', 'stage', 'post', 'put'
  ];

  const hasWriteVerb = writeVerbs.some(verb => lowerName.includes(verb));
  if (hasWriteVerb) {
    return { ok: false, reason: `GitHub write tool '${toolName}' is strictly prohibited for AI agents.` };
  }

  const readVerbs = ['get', 'read', 'list', 'search', 'view', 'check', 'status'];
  const hasReadVerb = readVerbs.some(verb => lowerName.includes(verb));
  if (hasReadVerb) {
    return { ok: true };
  }

  return { ok: false, reason: `Unrecognized GitHub tool '${toolName}' denied under deny-by-default write protection policy.` };
}

try {
  const input = fs.readFileSync(0, 'utf-8');
  if (!input || input.trim().length === 0) {
    exitWith(DENY('No input provided to hook. Denying tool execution for safety.'));
  }

  let payload;
  try {
    payload = JSON.parse(input);
  } catch (err) {
    exitWith(DENY('Malformed JSON input in hook. Denying tool execution for safety.'));
  }

  const toolCall = payload.toolCall;
  if (!toolCall || typeof toolCall !== 'object') {
    exitWith(DENY('Missing toolCall object in hook payload. Denying tool execution for safety.'));
  }

  const toolName = String(toolCall.name || '');

  if (toolName.toLowerCase().includes('github')) {
    const ghToolCheck = checkGitHubTool(toolName);
    if (!ghToolCheck.ok) {
      exitWith(DENY(ghToolCheck.reason));
    }
  }

  if (toolName === 'run_command') {
    const args = toolCall.args;
    if (!args || typeof args !== 'object' || typeof args.CommandLine !== 'string') {
      exitWith(DENY('Invalid or missing CommandLine in run_command tool call.'));
    }

    const commandLine = args.CommandLine.trim();
    if (commandLine.length === 0) {
      exitWith(ALLOW);
    }

    const subCommands = splitCommands(commandLine);
    if (subCommands.length === 0) {
      exitWith(ALLOW);
    }

    for (const subCmd of subCommands) {
      const check = validateSingleCommand(subCmd);
      if (!check.ok) {
        exitWith(DENY(check.reason));
      }
    }
  }

  exitWith(ALLOW);
} catch (unexpectedError) {
  exitWith(DENY(`Unexpected hook error: ${unexpectedError.message}. Denying for safety.`));
}
