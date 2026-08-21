#!/usr/bin/env bash
#
# PlotManager — The Ultimate Plot Management System
# Copyright (c) 2026 RedGlitchX. All Rights Reserved.
#
# Installs the GitHub Actions workflows that build a .jar for every supported
# Minecraft version and publish them (plus the source code) as a GitHub Release.
#
# They live in ci/workflows/ instead of .github/workflows/ because GitHub only
# lets an app write to .github/workflows/ when it holds the "workflows"
# permission. Running this script from your own account installs them.
#
#   bash scripts/enable-ci.sh          # copy + commit
#   bash scripts/enable-ci.sh --push   # copy + commit + push
#
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root"

if [ ! -d ci/workflows ]; then
  echo "error: ci/workflows/ not found - run this from the PlotManager repository." >&2
  exit 1
fi

mkdir -p .github/workflows
cp ci/workflows/*.yml .github/workflows/
echo "installed:"
ls -1 .github/workflows/

git add .github/workflows
if git diff --cached --quiet; then
  echo "nothing to commit - workflows already installed."
else
  git commit -m "Enable PlotManager build + release workflows"
  echo "committed."
fi

if [ "${1:-}" = "--push" ]; then
  branch="$(git rev-parse --abbrev-ref HEAD)"
  git push origin "$branch"
  echo "pushed to $branch."
fi

cat <<'NEXT'

Done. To cut a release with a .jar for every Minecraft version:

    git tag v1.0.0
    git push origin v1.0.0

or open the Actions tab -> "Release" -> "Run workflow" and type the version.
NEXT
