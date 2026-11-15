import fs from 'node:fs';

try {
  const input = fs.readFileSync(0, 'utf-8');
  if (!input || input.trim().length === 0) {
    console.log(JSON.stringify({ decision: 'allow' }));
    process.exit(0);
  }

  const payload = JSON.parse(input);
  const toolCall = payload.toolCall;

  if (toolCall && toolCall.name === 'run_command' && toolCall.args && toolCall.args.CommandLine) {
    const cmd = toolCall.args.CommandLine.trim();

    const blockedPatterns = [
      /\bgit\s+commit\b/i,
      /\bgit\s+push\b/i,
      /\bgit\s+tag\b/i,
      /\bgit\s+merge\b/i,
      /\bgit\s+rebase\b/i,
      /\bgit\s+cherry-pick\b/i,
      /\bgh\s+pr\s+create\b/i,
      /\bgh\s+pr\s+merge\b/i,
      /\bgh\s+repo\b/i
    ];

    const isBlocked = blockedPatterns.some((pattern) => pattern.test(cmd));

    if (isBlocked) {
      console.log(JSON.stringify({
        decision: 'deny',
        reason: 'BLOCKED: Git commit and push operations are strictly prohibited for AI agents in this project. The repository owner manually manages all Git commits and pushes.'
      }));
      process.exit(0);
    }
  }

  console.log(JSON.stringify({ decision: 'allow' }));
} catch (error) {
  console.log(JSON.stringify({ decision: 'allow' }));
}
