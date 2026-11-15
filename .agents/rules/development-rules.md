---
trigger: always_on
---

# Development Rules: Beginner-Friendly and Clean Code

These rules apply to all code generated or modified in this repository.

Consult the authoritative specification in [docs/architecture-v2.md](../../docs/architecture-v2.md).

---

## 1. Beginner-Friendly Code

The developer is learning Spring Boot and building an MVP.
- Write simple, clear code that any beginner can easily read, understand, and maintain.
- Do not use complex or obscure programming language features when a simpler solution exists.
- Avoid deep inheritance hierarchies, abstract base classes, and custom generic frameworks.
- Prefer explicit code over clever or highly compressed one-liners.

---

## 2. Simple and Clear Naming Conventions

Always follow standard Java and JavaScript naming conventions with self-explanatory names.

- **Classes:** PascalCase (for example: `EmailController`, `EmailService`, `EmailRepository`, `EvidenceService`, `RiskScoreService`).
- **Methods:** camelCase (for example: `uploadEmail`, `checkEmail`, `calculateRiskScore`, `extractHeaders`).
- **Variables and Parameters:** camelCase (for example: `emailFile`, `fileHash`, `emailText`, `providerResult`).
- **Constants:** UPPER_SNAKE_CASE (for example: `MAX_FILE_SIZE_BYTES`, `DEFAULT_STATUS`).
- **Packages:** lowercase (for example: `com.emailforensics.service`).

Avoid cryptic acronyms, single-letter variables (except standard loop index `i`), and confusingly long names.

---

## 3. Strict No Code Comments Rule

Never write comments inside source code files. This is a strict project requirement.

- Do not write single-line comments (`//`).
- Do not write multi-line comments (`/* ... */`).
- Do not write JavaDoc comments (`/** ... */`).
- Do not write TODO comments.
- Do not write HTML comments (`<!-- ... -->`).
- Do not write CSS comments (`/* ... */`).
- Do not write explanatory comments inside methods.

Make the code self-explanatory through meaningful names and small methods. If an explanation is necessary, provide it in the chat response or write it in external documentation in `docs/`.

---

## 4. Small Methods and Single Responsibility

- Keep methods short (under 25 lines where possible) and focused on a single task.
- Break down large methods with multiple responsibilities into smaller helper methods.
- Avoid deeply nested `if`/`else` statements or loops. Use early returns (`guard clauses`) to keep nesting shallow.
- Each class must have one clear, well-defined responsibility.

---

## 5. Simple Flow and Control Structures

- Use standard `for` loops, enhanced `for-each` loops, and basic `if`/`else` conditions.
- Avoid deeply chained Java Streams or reactive patterns when a standard loop is more readable for a beginner.
- Handle null values safely and explicitly without overly complex Optional chains.

---

## 6. Explaining Changes Outside Source Code (Mentoring)

When implementing or modifying a feature, explain in the chat response or documentation:
1. **What files were created or modified:** List of paths.
2. **Why each file is needed:** Its role in simple English.
3. **How data flows through the feature:** Controller -> Service -> Repository -> Database.
4. **How to test the implementation:** Exact commands and expected results.
5. **What the developer should understand before moving to the next phase.**
