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

def safe(s):
    # A literal </script> inside an inlined file (e.g. ads-config.js has
    # one in a comment) would close the <script> block early and break the
    # rest of the file — escape it. <\/script> is identical to </script>
    # for JS (inside a comment it is just text; in a string, \/ == /).
    # This is the same fix the manual build's inliner has; without it the
    # inlined ADS_CONFIG silently vanished and the game fell back to the
    # default adinplay/test provider.
    return s.replace("</script>", "<\\/script>")

def main():
    html_path = sys.argv[1]
    html = open(html_path, encoding="utf-8").read()
    orig_closes = html.count("</script>")
    for cfg_path in sys.argv[2:]:
        name = cfg_path.rsplit("/", 1)[-1]
        marker = f'<script src="{name}"></script>'
        cfg = open(cfg_path, encoding="utf-8").read()
        assert marker in html, f"config script tag not found in index.html: {marker}"
        html = html.replace(marker, "<script>\n" + safe(cfg) + "\n</script>", 1)
        assert marker not in html, f"config script tag still present after inline: {marker}"
        print(f"inlined {name} into index.html")
    assert html.count("</script>") == orig_closes, (
        "inlining changed the </script> count — a config broke a script block"
    )
    open(html_path, "w", encoding="utf-8").write(html)

if __name__ == "__main__":
    main()
