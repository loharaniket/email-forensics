---
trigger: always_on
---

# Git Version Control Rules: Preserving Stable Branches

These rules protect repository history and prevent accidental data loss or unstable releases.

## 1. Absolute Protection of the Main Branch

- The `main` branch contains the stable, verified Version 1 of the Email Forensics project.
- **NEVER** switch to `main` to make edits.
- **NEVER** commit changes to `main`.
- **NEVER** force-push or reset `main`.
- `main` must remain completely untouched until Version 2 is fully tested, reviewed, and deliberately merged by the repository owner.

## 2. Work Strictly on the Development Branch

- All Version 2 development must occur on the `development` branch (or feature branches cut from it, such as `feature/email-forensics-v2`).
- Before starting work on any task, run `git status` or `git branch --show-current` to confirm you are on `development`.
- If on `main`, immediately switch to `development`:
  ```bash
  git checkout development
  ```

## 3. No Automated Commits or Pushes

- Do NOT execute `git commit` or `git push` automatically.
- The project owner manages git commits and pushes manually.
- Leave new and modified files in the working tree for the developer to inspect with `git diff` and commit themselves.

## 4. No Destructive Git Commands

- Never run destructive commands such as:
  - `git reset --hard`
  - `git clean -fd`
  - `git checkout .` (without explicit confirmation)
  - `git push --force`
- Never overwrite or discard uncommitted developer work.
- Always check repository status before and after executing tasks.
