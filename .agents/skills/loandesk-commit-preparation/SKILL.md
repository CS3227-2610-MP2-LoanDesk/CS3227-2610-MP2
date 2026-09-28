---
name: loandesk-commit-preparation
description: Prepare LoanDesk commits and commit messages when asked to draft a message, stage intended files, or create a commit. Inspect the scoped diff, preserve unrelated work, and require explicit authorization before committing.
---

# LoanDesk commit preparation

Use this skill for a request to write a commit message, prepare a commit, stage
files, or create a commit in this repository. First inspect the current branch,
`git status --short`, the relevant diff (`git diff` or `git diff --cached`),
and recent commit subjects when that helps match local style.

Summarize the actual change in an imperative subject line of at most 72
characters. Include a concise body only when it clarifies a material behavior,
test, migration, compatibility, or coordination detail. Do not claim tests,
reviews, documentation, or external actions that were not performed.

When staging is requested, stage only the files in the user-approved scope.
Treat pre-existing unstaged, staged, or untracked files as user work unless the
request clearly includes them. Before committing, show or inspect the staged
diff and confirm it does not contain `data/`, secrets, generated build output,
or unrelated changes.

Creating a commit requires an explicit user request to commit. A request to
draft a message or stage files is not commit authorization. If the request
does authorize a commit, run relevant verification when it has not already
been run or state that it was not run and why. Report the exact commit hash,
subject, staged scope, verification performed, and any remaining worktree
changes.

Do not amend, reset, force-push, or alter another branch unless the user
explicitly asks. Do not create or switch branches merely to write a message or
stage a commit; follow an explicit branch request when one is provided.
