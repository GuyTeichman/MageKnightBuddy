# GitHub-centric development workflow

This project tracks work as GitHub issues and merges into `main` exclusively through pull requests. `main` is branch-protected: direct pushes are blocked, and PRs must pass CI before they're mergeable.

## The loop

1. Work starts from a GitHub issue (bug or feature request) filed at [GuyTeichman/MageKnightBuddy](https://github.com/GuyTeichman/MageKnightBuddy).
2. The author points Claude Code at a specific issue number for context.
3. Claude creates a branch named `issue-<number>-<slug>` (e.g. `issue-42-fix-fame-rounding`), implements the change, and commits.
4. Claude pushes the branch and opens a PR whose description includes `Closes #<number>`, so the issue auto-closes when the PR merges.
5. CI (`.github/workflows/ci.yml`) runs two required checks on the PR: `test` (`./gradlew test`) and `build` (`./gradlew build`, gated behind `test` passing).
6. Once both checks are green, the PR is merged into `main`, closing the linked issue.

## Standing authorization

Pushing branches and opening PRs are normally actions Claude confirms before taking. For *this specific flow* — implementing a change for an issue the author explicitly pointed Claude at — that confirmation is pre-authorized here: Claude pushes the branch and runs `gh pr create` without asking each time. This does not extend to force-pushes, merging the PR itself, or any action outside this loop.

## Required CI checks

Branch protection on `main` requires the `test` and `build` jobs defined in `.github/workflows/ci.yml` to pass before a PR can merge. Both must be added as required status checks in the repo's branch protection settings (GitHub only offers a check for selection after it has run at least once, or it can be typed in manually).
