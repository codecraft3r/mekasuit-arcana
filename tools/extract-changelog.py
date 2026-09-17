#!/usr/bin/env python3
"""Extract release notes for a specific version from CHANGELOG.md (Keep a Changelog 1.1.0)."""
import re
import sys
from pathlib import Path


def extract_changelog(changelog_path: Path, tag_or_version: str) -> str:
    content = changelog_path.read_text(encoding="utf-8")

    # Normalize tag/version (strip leading 'v' if present)
    version = tag_or_version.strip().lstrip("v")

    # Match ## [version] or ## [v?version...] with optional release date (e.g. - YYYY-MM-DD)
    pattern = rf"^##\s+\[v?{re.escape(version)}\](?:\s+-\s+[^\n]+)?\s*$"

    lines = content.splitlines()
    start_idx = None
    end_idx = None

    for i, line in enumerate(lines):
        if start_idx is None:
            if re.match(pattern, line, re.IGNORECASE):
                start_idx = i + 1
        else:
            # Stop at the next version section or link reference definitions
            if line.startswith("## ") or re.match(r"^\[[^\]]+\]:\s+", line):
                end_idx = i
                break

    if start_idx is None:
        raise ValueError(
            f"Version '{tag_or_version}' (normalized: '{version}') not found in {changelog_path}"
        )

    section_lines = lines[start_idx:] if end_idx is None else lines[start_idx:end_idx]

    notes = "\n".join(section_lines).strip()
    return notes


def main():
    if len(sys.argv) < 2:
        print("Usage: extract-changelog.py <tag_or_version> [changelog_path]", file=sys.stderr)
        sys.exit(1)

    tag = sys.argv[1]
    root = Path(__file__).resolve().parents[1]
    changelog = Path(sys.argv[2]) if len(sys.argv) > 2 else root / "CHANGELOG.md"

    try:
        notes = extract_changelog(changelog, tag)
        print(notes)
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()