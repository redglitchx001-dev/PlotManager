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
| `build.yml` | every push / PR | compiles the plugin against **every** supported Paper version and uploads each jar as a build artifact |
| `release.yml` | tag `v*`, or "Run workflow" | builds all jars, then publishes a GitHub Release with them + the source code + checksums |

### The version matrix is automatic

`.github/scripts/targets.py` reads PaperMC's live `maven-metadata.xml` and
builds the list of targets from the first supported release (**1.21.4**) up to
the newest Paper build available at that moment — so "first version to last"
stays true without editing anything. If PaperMC is unreachable the script falls
back to a pinned list, so a release is never blocked.

Java is picked per target: `21` for the 1.21.x line, `25` for 26.x and newer.

### What ends up on a release

```
PlotManager-<version>-universal.jar     <- recommended download, runs on all supported versions
PlotManager-<version>-mc1.21.4.jar      <- one jar per Minecraft version ...
PlotManager-<version>-mc1.21.5.jar
PlotManager-<version>-mc<latest>.jar
PlotManager-<version>-source.zip        <- full source code
PlotManager-<version>-sources.jar       <- same source, Maven layout
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
