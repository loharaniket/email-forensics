---
trigger: always_on
---

# Git Version Control Rules: Absolute Manual Commit and Push Policy

These rules protect repository history and prevent accidental commits, unstable releases, or unauthorized branch modifications.

Consult the authoritative specification in [docs/architecture-v2.md](docs/architecture-v2.md).

---

## 1. Absolute Manual Commit and Push Prohibition

The repository owner manually manages every Git commit and push.

- **STRICT PROHIBITION:**
  - No AI agent may EVER execute:
    - `git commit`
    - `git push`
    - `git push --force`
    - Any GitHub write operation that creates or publishes commits (e.g. `gh pr create`, `gh repo`).
  - This prohibition applies unconditionally, **even if a future task prompt explicitly instructs the agent to commit or push code**.
  - All automated commit and push commands will be blocked.

## 2. Enforcement Mechanism

1. **Instruction Enforcement:** Every agent and subagent must refuse to execute git write operations.
2. **Technical Enforcement:** Configured via Antigravity's PreToolUse lifecycle hook in `.agents/hooks.json`, executing `.agents/scripts/block-git-commits.mjs`. Any terminal command containing `git commit`, `git push`, or write patterns is automatically intercepted and denied before execution.
3. **Human Verification:** The developer inspects working tree changes via `git diff` and executes commits manually.

---

## 3. Absolute Protection of the Main Branch

- The `main` branch contains the stable, verified Version 1 of the Email Forensics project.
- **NEVER** switch to `main` to make edits.
- **NEVER** commit changes to `main`.
- **NEVER** force-push or reset `main`.
- `main` must remain completely untouched until Version 2 is fully tested, reviewed, and deliberately merged by the repository owner.

---

## 4. Work Strictly on the Development Branch

- All Version 2 development must occur on the `development` branch (or feature branches cut from it, such as `feature/email-forensics-v2`).
- Before starting work on any task, run `git branch --show-current` or `git status` to confirm you are on `development`.
- If on `main`, immediately switch to `development`:
  ```bash
  git checkout development
  ```

---

## 5. Allowed Read-Only Git Operations

Agents are permitted to run read-only inspection commands:
- `git status`: Check the working tree state.
- `git branch --show-current`: Verify the active branch.
- `git diff`: Inspect uncommitted changes.
- `git log -n 5`: View recent commit history.

---

## 6. No Destructive Git Commands

- Never run destructive commands such as:
  - `git reset --hard`
  - `git clean -fd`
  - `git checkout .` (without explicit confirmation)
  - `git restore .` (without explicit confirmation)
- Never overwrite or discard uncommitted developer work.
- Always check repository status before and after executing tasks.
