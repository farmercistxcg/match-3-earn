#!/usr/bin/env python3
"""Inject the live Supabase URL + anon key into the repo's placeholder config.

Reads SUPABASE_URL and SUPABASE_ANON_KEY from the environment (GitHub repo
secrets in CI) and rewrites app/src/main/assets/public/supabase-config.js.
Fails loudly if either placeholder is still present afterwards.

Kept as a file (instead of an inline heredoc in the workflow) because a
heredoc terminator must sit at column 0, which YAML block scalars cannot
express — the inline version broke run 44466887331 with
"here-document delimited by end-of-file (wanted `PYEOF')".
"""
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def main():
    url = os.environ.get("SUPABASE_URL", "")
    key = os.environ.get("SUPABASE_ANON_KEY", "")
    if not url or not key:
        print("SUPABASE_URL / SUPABASE_ANON_KEY not set", file=sys.stderr)
        return 1
    cfg_path = os.path.join(
        ROOT, "app/src/main/assets/public/supabase-config.js"
    )
    cfg = open(cfg_path, encoding="utf-8").read()
    cfg = cfg.replace("https://YOUR-PROJECT.supabase.co", url)
    cfg = cfg.replace("YOUR-ANON-KEY", key)
    assert "YOUR-PROJECT" not in cfg and "YOUR-ANON-KEY" not in cfg, (
        "placeholder left in config"
    )
    open(cfg_path, "w", encoding="utf-8").write(cfg)
    print("Supabase config injected from secrets")


if __name__ == "__main__":
    sys.exit(main())
