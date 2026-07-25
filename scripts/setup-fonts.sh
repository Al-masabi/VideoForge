#!/usr/bin/env bash
set -euo pipefail

DEST="$(cd "$(dirname "$0")/../core/designsystem/src/main/res" && pwd)/font"
mkdir -p "$DEST"

BASE="https://raw.githubusercontent.com/google/fonts/main/ofl"

fetch() {
    curl -fL "$1" -o "$DEST/$2"
    echo "✓ $2"
}

echo "Downloading open-source fonts (OFL)..."

fetch "$BASE/cairo/static/Cairo-ExtraBold.ttf"        "cairo_extrabold.ttf"
fetch "$BASE/cairo/static/Cairo-Bold.ttf"             "cairo_bold.ttf"
fetch "$BASE/cairo/static/Cairo-SemiBold.ttf"         "cairo_semibold.ttf"
fetch "$BASE/ibmplexsansarabic/IBMPlexSansArabic-Regular.ttf" "plex_arabic_regular.ttf"
fetch "$BASE/ibmplexsansarabic/IBMPlexSansArabic-Medium.ttf"  "plex_arabic_medium.ttf"
fetch "$BASE/ibmplexsansarabic/IBMPlexSansArabic-Bold.ttf"    "plex_arabic_bold.ttf"
fetch "$BASE/ibmplexmono/IBMPlexMono-Regular.ttf"     "plex_mono_regular.ttf"
fetch "$BASE/ibmplexmono/IBMPlexMono-Bold.ttf"        "plex_mono_bold.ttf"

echo ""
echo "Fonts ready in: $DEST"