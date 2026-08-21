#!/usr/bin/env python3
"""
PlotManager - The Ultimate Plot Management System
Copyright (c) 2026 RedGlitchX. All Rights Reserved.

Builds the GitHub Actions build matrix: one entry per supported Paper/Minecraft
version, from the first supported release up to the newest one PaperMC
publishes. Falls back to a pinned list when the repository is unreachable so a
release can never be blocked by a network hiccup.

Usage:  python3 .github/scripts/targets.py [--max N]
Output: a single line of JSON, ready for `matrix: ${{ fromJSON(...) }}`
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import urllib.request

METADATA = ("https://repo.papermc.io/repository/maven-public/"
            "io/papermc/paper/paper-api/maven-metadata.xml")

# Oldest server release PlotManager supports. Its jar is the "universal" build:
# the Paper API only ever adds methods, so a jar compiled here also runs on
# every newer version.
FLOOR = (1, 21, 4)

# Used when repo.papermc.io cannot be reached during the workflow.
FALLBACK = [
    "1.21.4-R0.1-SNAPSHOT",
    "1.21.5-R0.1-SNAPSHOT",
    "1.21.6-R0.1-SNAPSHOT",
    "1.21.7-R0.1-SNAPSHOT",
    "1.21.8-R0.1-SNAPSHOT",
]


def parse(version: str):
    """'1.21.4-R0.1-SNAPSHOT' -> (1, 21, 4). None when unparsable."""
    base = version.split("-", 1)[0]
    if not re.fullmatch(r"\d+(\.\d+)*", base):
        return None
    parts = [int(p) for p in base.split(".")]
    while len(parts) < 3:
        parts.append(0)
    return tuple(parts[:3])


def fetch() -> list[str]:
    try:
        with urllib.request.urlopen(METADATA, timeout=45) as response:
            body = response.read().decode("utf-8", "replace")
        found = re.findall(r"<version>([^<]+)</version>", body)
        return found or list(FALLBACK)
    except Exception as exc:                                  # noqa: BLE001
        print(f"::warning::could not read PaperMC metadata ({exc}); "
              f"using the pinned fallback list", file=sys.stderr)
        return list(FALLBACK)


def jdk_for(version: tuple[int, ...]) -> str:
    """Paper 1.21.x targets Java 21; anything newer is built on Java 25."""
    return "21" if version[0] == 1 else "25"


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--max", type=int, default=24,
                    help="cap on the number of build targets")
    args = ap.parse_args()

    seen: dict[tuple, str] = {}
    for raw in fetch():
        if not raw.endswith("-R0.1-SNAPSHOT"):
            continue
        version = parse(raw)
        if version is None or version < FLOOR:
            continue
        seen[version] = raw

    if not seen:
        for raw in FALLBACK:
            version = parse(raw)
            if version:
                seen[version] = raw

    ordered = sorted(seen)
    if len(ordered) > args.max:                # keep the oldest + the newest ones
        ordered = [ordered[0]] + ordered[-(args.max - 1):]

    include = []
    for position, version in enumerate(ordered):
        mc = ".".join(str(p) for p in version).removesuffix(".0") if version[2] == 0 \
            else ".".join(str(p) for p in version)
        include.append({
            "mc": mc,
            "paper": seen[version],
            "jdk": jdk_for(version),
            "universal": position == 0,
        })

    print(json.dumps({"include": include}, separators=(",", ":")))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
