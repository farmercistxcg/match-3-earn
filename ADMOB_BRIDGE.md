# AdMob rewarded-ads bridge — JS ↔ native contract

The native side lives in `MainActivity.java` (`AdMobBridge`, exposed to the
game page as `window.AdMobBridge`). The game side is WIRED UP (versionCode 9 /
1.8): when `ads-config.js` sets `provider: "native"`, the game's
`AdProvider.showRewarded(kind)` routes all three reward flows through this
bridge. The AdinPlay/test path is used only when `provider` is `"adinplay"`.

## Provider switch

The game's `ads-config.js` has a `provider` field: `"adinplay"` (the
JavaScript AdinPlay path, already built into the game, test ads only
until a real publisher ID is pasted in), `"native"` (this AdMob bridge),
or `"none"`. The follow-up wiring sets `provider` to `"native"` and
routes the game's three reward flows (extra moves, free booster, extra
life — see below) through `window.AdMobBridge` instead of AdinPlay.
Until then the game uses AdinPlay and this bridge is inert.

## Reward types

`rewardType` is one of:

| Value | What the player gets (game grants it) |
|---|---|
| `"moves"` | +5 moves on the current level |
| `"booster"` | one free booster of the player's choice (Hammer / Shuffle / Magic) |
| `"life"` | +1 life |

## JS → native

```js
// Is a rewarded ad loaded and ready to show?
AdMobBridge.isRewardedAdReady()        // -> boolean (synchronous)

// Ask to show a rewarded ad for a reward. Returns immediately;
// the result comes back through window.__onAdmobReward.
AdMobBridge.showRewardedAd("moves")

// Ask native to (re)load an ad in the background.
AdMobBridge.preloadRewardedAd()
```

`AdMobBridge` only exists inside the app's WebView. In a desktop browser
(or before the bridge is wired) `window.AdMobBridge` is `undefined` — the
game must check for it and hide/disable the ad buttons when it is absent.

## Native → JS

```js
// Called when an ad finishes loading and one is ready to show.
window.__onAdmobAdReady = function () { /* enable the ad buttons */ };

// Called exactly once per showRewardedAd call, with the outcome.
window.__onAdmobReward = function (result) {
  // result = { rewardType: "moves"|"booster"|"life",
  //            granted: true|false,
  //            error: null | "not_ready" | "closed_early"
  //                   | "show_failed:<code>" }
};
```

**Grant the reward only when `granted === true`.** That means the user
watched the ad through to the reward point. `closed_early` = they closed
it before earning; `not_ready` = no ad was loaded (the game should offer
to retry after a preload); `show_failed` = the ad failed to display.

Native pre-loads an ad at startup and again after every ad closes, so an
ad is normally ready when the player asks for one.

## AdMob IDs

- The app ID goes in the manifest (`${admobAppId}` placeholder): Google's
  TEST app ID by default, the real one via `-PadmobAppId` or the
  `ADMOB_APP_ID` repo secret.
- The rewarded ad unit ID is the `admob_rewarded_ad_unit_id` string
  resource: Google's TEST rewarded unit by default, the real one via
  `-PadmobRewardedAdUnitId` or the `ADMOB_REWARDED_AD_UNIT_ID` secret.
- With test IDs the app shows **test ads** (no revenue). Real revenue
  needs the real IDs from the user's own AdMob account (see README.md).

## Anti-farm limits (game side, to be set when wiring the JS)

The native side does not cap how often ads are shown. The game must:
- cap rewarded ads per level and per day (pick the numbers when wiring);
- never grant a reward except on `granted === true`;
- never show an ad button while another ad flow is in progress.
