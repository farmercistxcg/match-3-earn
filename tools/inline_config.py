#!/usr/bin/env python3
"""Inline config files into index.html (same step as the manual build).

The WebView loads the game with loadDataWithBaseURL from a string, so a
separate <script src="..."> would never resolve in the APK. Each config
file is inlined by replacing its <script src="..."> tag with the file
contents wrapped in <script>...</script>.

Usage:
    python3 tools/inline_config.py <index.html> <config1.js> [<config2.js> ...]

Exits non-zero if any marker script tag is not found.
"""
import sys

def main():
    html_path = sys.argv[1]
    html = open(html_path, encoding="utf-8").read()
    for cfg_path in sys.argv[2:]:
        name = cfg_path.rsplit("/", 1)[-1]
        marker = f'<script src="{name}"></script>'
        cfg = open(cfg_path, encoding="utf-8").read()
        assert marker in html, f"config script tag not found in index.html: {marker}"
        html = html.replace(marker, "<script>\n" + cfg + "\n</script>", 1)
        print(f"inlined {name} into index.html")
    open(html_path, "w", encoding="utf-8").write(html)

if __name__ == "__main__":
    main()
