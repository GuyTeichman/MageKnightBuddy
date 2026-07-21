# assets

Orphan branch (no shared history with `main`) used only to host images referenced from PR/issue comments via `raw.githubusercontent.com` URLs, since `gh` CLI has no way to upload attachments directly into a PR body or comment.

Not part of any app code. Never merge this branch into `main`.

## Layout

`.github/pr-assets/issue-<N>/*.png` — screenshots for issue/PR `<N>`, one subfolder per issue.

## Usage

1. Add new images under `.github/pr-assets/issue-<N>/`, commit, push to `origin/assets`.
2. Get the commit SHA (`git rev-parse HEAD`).
3. Reference images in a PR/issue comment as:
   `https://raw.githubusercontent.com/GuyTeichman/MageKnightBuddy/<commit-sha>/.github/pr-assets/issue-<N>/<file>.png`
   Pin to the commit SHA, not the branch name — a URL pinned to a SHA keeps working even after more images are added later.
4. Note in the comment that the images are hosted from this branch and aren't part of the PR's own diff.

See `docs/design/workflow.md` for the full convention.
