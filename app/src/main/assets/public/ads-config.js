/* Match-3 Earn — rewarded ads config (AdinPlay).
   Provider: AdinPlay (adinplay.com) — a JavaScript rewarded-video network
   built for HTML5 games. It works from plain JavaScript inside the
   WebView, which is what this app is (no Gradle, so no AdMob SDK).
   HOW TO GO LIVE:
     1. Create a publisher account at https://adinplay.com and add this
        game as a property/website.
     2. Copy the property ID (also called the "AIP ID") from the AdinPlay
        dashboard and paste it as publisherId below.
     3. Rebuild the APK (the v9 build inlines this file, same as
        supabase-config.js) and ship it.
   While publisherId is empty, the game uses a clearly-labelled TEST AD
   (a 5-second countdown, no real ad, no revenue) so the whole reward
   flow — offer, watch, reward, limits — can be tested end to end.
   As soon as a real publisherId is filled in, the real AdinPlay path
   is used and the test ad disappears by itself.
   NOTE FOR THE BUILD: MainActivity reads only public/index.html, so
   this file MUST be inlined into it at build time exactly like
   supabase-config.js (replace the <script src="ads-config.js"> tag
   with the file contents wrapped in <script>...</script>). */
window.ADS_CONFIG = {
  provider: "native", // CI APK: AdMob via window.AdMobBridge (see ADMOB_BRIDGE.md)
  publisherId: ""        // AdinPlay ID — unused on the native path
};
