"""Check the release excludes dependencies/harness and matches tested classes."""
import sys
from pathlib import Path
from zipfile import ZipFile

root = Path(__file__).resolve().parents[1]
build_libs = root / "build/libs"

if len(sys.argv) > 1:
    version = sys.argv[1].lstrip("v")
    release_path = build_libs / f"mekasuit-arcana-{version}.jar"
    harness_path = build_libs / f"mekasuit-arcana-{version}-runtime-test.jar"
else:
    release_jars = [
        p for p in build_libs.glob("mekasuit-arcana-*.jar")
        if not p.name.endswith("-runtime-test.jar") and not p.name.endswith("-sources.jar")
    ]
    harness_jars = list(build_libs.glob("mekasuit-arcana-*-runtime-test.jar"))
    if not release_jars:
        raise SystemExit("Expected a release jar in build/libs")
    if not harness_jars:
        raise SystemExit("Expected a runtime test jar in build/libs")
    release_path = release_jars[0]
    harness_path = harness_jars[0]

with ZipFile(release_path) as release, ZipFile(harness_path) as harness:
    classes = [name for name in release.namelist() if name.endswith(".class")]
    if not classes or any(not n.startswith("dev/vvh/mekasuitarcana/") for n in classes):
        raise SystemExit("Unexpected bundled dependency classes or empty release")
    if any("RuntimeVerification" in n or "RuntimeTimingVerification" in n for n in classes):
        raise SystemExit("Runtime harness leaked into release")
    if any(release.read(n) != harness.read(n) for n in classes):
        raise SystemExit("Production bytecode differs between release and harness")
    recipes = [n for n in release.namelist() if n.startswith("data/mekasuitarcana/recipe/") and n.endswith(".json")]
    if len(recipes) != 5:
        raise SystemExit("Expected five recipes")
    print(f"PASS: {len(classes)} production classes, five recipes, no bundled dependencies or harness ({release_path.name})")
