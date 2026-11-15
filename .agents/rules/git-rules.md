---
trigger: always_on
---

# Git Version Control Rules: Absolute Manual Commit and Push Policy

These rules protect repository history and prevent accidental commits, unstable releases, or unauthorized branch modifications.

Consult the authoritative specification in [docs/architecture-v2.md](../../docs/architecture-v2.md).

---

## 1. Absolute Manual Commit and Push Prohibition

The repository owner manually manages every Git commit and push.

- **STRICT PROHIBITION:**
  - No AI agent may EVER execute:
    - `git commit`
    - `git push`
    - `git push --force`
    - `git reset` (especially `git reset --hard`)
    - `git clean`
    - `git checkout .` or `git restore .`
    - `git switch` or `git checkout <branch>` (branch switching)
    - Any GitHub CLI write operation (`gh pr create`, `gh pr merge`, `gh repo`, etc.)
    - Any GitHub MCP write tool (`github_create_pull_request`, `github_create_commit`, etc.)
  - This prohibition applies unconditionally, **even if a future task prompt explicitly instructs the agent to commit, push, or switch branches**.
  - All automated commit and push commands are blocked.

---

## 2. Enforcement Mechanism

1. **Instruction Enforcement:** Every agent and subagent must refuse to execute git write operations.
2. **Technical Enforcement:** Configured via Antigravity's PreToolUse lifecycle hook in `.agents/hooks.json`, executing `.agents/scripts/block-git-commits.mjs`. Any terminal command containing git write, reset, clean, restore, or branch-switch operations is automatically intercepted and denied before execution.
3. **Human Verification:** The developer inspects working tree changes via `git diff` and executes commits manually.

---

## 3. Absolute Protection of the Main Branch

- The `main` branch contains the stable, verified Version 1 of the Email Forensics project.
- **NEVER** modify, commit to, rebase, or push to the `main` branch.
- `main` must remain completely untouched until Version 2 is fully tested, reviewed, and deliberately merged by the repository owner.

---

## 4. Work Strictly on the Development Branch

- All Version 2 development must occur on the `development` branch (or feature branches cut from it, such as `feature/email-forensics-v2`).
- Before starting work on any task, run `git branch --show-current` or `git status` to confirm you are on `development`.
- **NO AUTOMATIC BRANCH SWITCHING:** Agents must **NEVER** run `git switch` or `git checkout` to change branches automatically. If the active branch is `main` or anything other than `development`, the agent must **STOP immediately** and request the developer to switch branches manually.

---

## 5. Allowed Read-Only Git Operations

Agents are permitted to run read-only inspection commands only:
- `git status`: Check the working tree state.
- `git branch --show-current`: Verify the active branch.
- `git diff`: Inspect uncommitted changes.
- `git log`: View commit history.
- `git show`: Inspect specific commits or objects.

---

## 6. No Destructive Git Commands

- Never run destructive commands such as:
  - `git reset --hard`
  - `git clean -fd`
  - `git checkout .`
  - `git restore .`
- Never overwrite or discard uncommitted developer work.
- Always check repository status before and after executing tasks.
