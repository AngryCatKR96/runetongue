#!/bin/sh
# Usage: tools/subset_font.sh path/to/Galmuri11.ttf
# Writes the font the plugin bundles, keeping only the characters in tools/chars_ko.txt.
set -eu
root=$(cd "$(dirname "$0")/.." && pwd)
uvx --from fonttools==4.55.3 pyftsubset "$1" \
	--text-file="$root/tools/chars_ko.txt" \
	--name-IDs='*' \
	--notdef-outline \
	--output-file="$root/src/main/resources/com/runehangul/Galmuri11-subset.ttf"
