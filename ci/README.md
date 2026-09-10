# Release & build pipeline

> PlotManager — Copyright (c) 2026 **RedGlitchX**. All Rights Reserved.

This folder holds the GitHub Actions workflows that build PlotManager and
publish releases.

## Why are they in `ci/workflows/` and not `.github/workflows/`?

GitHub refuses writes to `.github/workflows/` from an app that does not hold the
`workflows` permission, so they are staged here. Install them once, from your
own account:

```bash
bash scripts/enable-ci.sh --push
```

After that the two workflows are live and this folder is just the source of
truth for them.

## What the workflows do

| Workflow | Trigger | Result |
|---|---|---|
| `build.yml` | every push / PR | verify-compiles against **every** supported Paper version and builds the single `PlotManagerv1-All.jar` |
| `release.yml` | tag `v*`, or "Run workflow" | publishes a GitHub Release with the one jar + the source code + checksums |

`build.yml` also has a manual **commit_jar** switch: tick it on a "Run
workflow" dispatch and the built jar is committed back to the branch as
`release/PlotManagerv1-All.jar` (handy for grabbing the file without opening
the Actions UI).

### The version matrix is automatic

`.github/scripts/targets.py` reads PaperMC's live `maven-metadata.xml` and
builds the list of targets from the first supported release (**1.21**) up to
the newest Paper build available at that moment — so "first version to last"
stays true without editing anything. If PaperMC is unreachable the script falls
back to a pinned list, so a release is never blocked.

Java is picked per target: `21` for the 1.21.x line, `25` for 26.x and newer.
The matrix only *verifies* compilation — the shipped jar is always the single
file compiled against the oldest (1.21) API, which is forward compatible with
every newer version.

### What ends up on a release

```
PlotManagerv1-All.jar            <- the plugin: one file, every MC version
PlotManager-<version>-source.zip  <- full source code
PlotManager-<version>-sources.jar <- same source, Maven layout
LICENSE.txt
SHA256SUMS.txt
```

## Cutting a release

```bash
git tag v1.0.0
git push origin v1.0.0
```

or Actions → **Release** → **Run workflow** → type `1.0.0` (with optional
*pre-release* / *draft* switches).

Re-running for an existing tag re-uploads the files with `--clobber`, so a
failed build can simply be retried.

## Building by hand

```bash
mvn -B package                                    # universal jar (Paper 1.21.4 API)
mvn -B package -Dpaper.version=1.21.8-R0.1-SNAPSHOT -Djar.classifier=-mc1.21.8
```

The jar lands in `target/`.
