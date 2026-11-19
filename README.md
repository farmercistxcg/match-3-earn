# Match-3 Earn — GitHub Actions build (Gradle + Google AdMob)

This repo builds the **Match-3 Earn** Android app with Gradle on GitHub
Actions, because Gradle cannot run in the dev sandbox (loopback TCP is
blocked there) and the manual aapt2/d8 build cannot take the AdMob SDK.

The manual build (`~/workspace/game-app/manual-build/`, aapt2 + d8 +
apksigner) **stays the fallback** until this GitHub build is proven on the
user's phone. Both builds sign with the same debug keystore, so either APK
installs as an update over the other.

## The AppCompat rule (absolute)

The user's phone (Redmi 13C) **refuses to launch any app containing the
AppCompat libraries** — verified by bisect (plain Activity + WebView opens;
anything with AppCompat does not). So this project:

- uses a plain `android.app.Activity` + WebView, framework
  `Theme.Material.NoActionBar`, no `androidx.appcompat` dependency;
- enables AndroidX (`android.useAndroidX=true`) only because the AdMob
  SDK needs `androidx.core` / `androidx.annotation` etc. — those are NOT
  AppCompat and do not break the phone;
- **fails the CI build if `androidx/appcompat` appears anywhere in the
  dex** (see the "Verify APK" step in `.github/workflows/build-apk.yml`).

## AdMob SDK version pin — read before upgrading

`play-services-ads` is pinned to **24.5.0**. Version **25.x pulls in
`androidx.appcompat:appcompat-resources`** (via
`play-services-ads-api` → `com.google.android.play:hsdp:2.0.1`, compile
scope, AAR) — verified 2026-11-19 by walking the full transitive tree.
24.5.0's tree (57 artifacts, depth 3) contains **no** `androidx.appcompat`
artifact. Before any version bump, re-run the tree check and the CI dex
gate; if a newer version pulls AppCompat in, do not upgrade.

## Repo secrets (Settings → Secrets and variables → Actions)

| Secret | What it is | Required? |
|---|---|---|
| `KEYSTORE_BASE64` | base64 of the debug keystore (`manual-build/debug.keystore`) — same key as the manual builds, so the APK installs as an update | Yes |
| `SUPABASE_URL` | Supabase Project URL, e.g. `https://kwnjpmaecdaewdpqwoww.supabase.co` | Yes for online play |
| `SUPABASE_ANON_KEY` | Supabase anon (public) key | Yes for online play |
| `ADMOB_APP_ID` | Real AdMob app ID (`ca-app-pub-…~…`) | No — falls back to Google's TEST app ID (test ads) |
| `ADMOB_REWARDED_AD_UNIT_ID` | Real rewarded ad unit ID (`ca-app-pub-…/…`) | No — falls back to Google's TEST rewarded unit (test ads) |

Set them with the GitHub CLI (run from the repo):

```bash
base64 -w0 ~/workspace/game-app/manual-build/debug.keystore | gh secret set KEYSTORE_BASE64
gh secret set SUPABASE_URL
gh secret set SUPABASE_ANON_KEY
gh secret set ADMOB_APP_ID
gh secret set ADMOB_REWARDED_AD_UNIT_ID
```

**Never** add the Supabase `service_role` key here — it is only ever
pasted by hand into `cashout-server/admin.html`.

## AdMob account setup (the user does this himself)

1. Create an AdMob account at https://admob.google.com (needs a Google
   account + payment details for payouts).
2. Register the app (Android, package `com.youcandoit2221.game`) — AdMob
   gives you the **app ID** (`ca-app-pub-…~…`).
3. Create a **Rewarded** ad unit — AdMob gives you the **ad unit ID**
   (`ca-app-pub-…/…`).
4. Paste both IDs as the `ADMOB_APP_ID` / `ADMOB_REWARDED_AD_UNIT_ID`
   repo secrets above, then re-run the workflow.
5. Until real IDs are set, the build uses Google's official test IDs, so
   the app shows **test ads** (no revenue). Real ads = real IDs.

## How a build works

Push to `main` (or Run workflow manually) → GitHub runner:

1. Checks out, installs JDK 17 + Android SDK 36.
2. Decodes the keystore from `KEYSTORE_BASE64`.
3. Injects the Supabase URL + anon key from secrets into
   `app/src/main/assets/public/supabase-config.js` (placeholders stay if
   the secrets are unset — the APK then plays offline only), and inlines
   both config files (`supabase-config.js`, `ads-config.js`) into
   `index.html` with `tools/inline_config.py` (the WebView loads the
   game as a string, so a separate `<script src>` would never resolve).
4. Runs `./gradlew assembleRelease` (AdMob IDs from secrets or test
   defaults).
5. Verifies: `apksigner verify` (v2+v3), `aapt2 dump badging`, and the
   **AppCompat dex gate**. Uploads the APK as the `match-3-earn-apk`
   artifact.

Download it from the workflow run → Artifacts.

## Game sources

`app/src/main/assets/public/index.html` is a **snapshot** of
`~/workspace/game-app/www/index.html` (the canonical game source), taken
2026-11-19 after the difficulty + AdinPlay rewarded-ads work landed
(verified 79/79 Playwright checks). The game also has an AdinPlay
rewarded-ads path (`ads-config.js`, provider `"adinplay"`, test ads only
until a real publisher ID is pasted in) — that path is the v9 manual
build's ads route. This repo's AdMob bridge is the *other* route:
`ads-config.js` already has a `provider: "native"` option reserved for
it, and the follow-up that wires the bridge sets the provider to
`"native"` so the game's three reward flows (extra moves, free booster,
extra life) call `window.AdMobBridge` instead of AdinPlay. Do not edit
the game in both places — `www/` is canonical until that follow-up lands.

## Versioning

Bump `versionCode` / `versionName` together in `app/build.gradle` and
`app/src/main/AndroidManifest.xml`. Current: 7 / 1.6 (matches manual v8).
The next release (difficulty + ads + bridge) bumps to 8 / 1.7.
