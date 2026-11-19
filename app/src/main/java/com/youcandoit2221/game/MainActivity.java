package com.youcandoit2221.game;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Match-3 Earn — plain android.app.Activity + WebView, NO AppCompat, NO
 * AndroidX UI libraries (same proven approach as the manual build, which is
 * the only thing that opens on the user's phone).
 *
 * The game HTML is loaded from assets/public/index.html with base URL
 * https://localhost/ so localStorage persists, exactly like the manual
 * build. supabase-config.js is inlined into that HTML at build time
 * (see tools/inline_config.py) because a separate <script src> would
 * never resolve for a loadDataWithBaseURL page.
 *
 * ADMOB BRIDGE (see ADMOB_BRIDGE.md for the full contract):
 * JavaScript in the game calls window.AdMobBridge.showRewardedAd(type)
 * with type "moves" | "booster" | "life". Native code shows a Google
 * AdMob rewarded ad; when the user earns the reward (watches it through)
 * the page is called back at window.__onAdmobReward({rewardType, granted}).
 * The reward is granted by the GAME only when granted === true.
 * Until the game wires up the JS side, this bridge is inert.
 */
public class MainActivity extends Activity {
    private static final String TAG = "Match3Earn";
    private WebView wv;
    private RewardedAd rewardedAd;
    private String pendingRewardType;
    private boolean rewardGranted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MobileAds.initialize(this, status -> loadRewardedAd());
        wv = new WebView(this);
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        wv.addJavascriptInterface(new AdMobBridge(), "AdMobBridge");
        String html = readAsset("public/index.html");
        // Base URL gives the page a real origin (https://localhost/) so
        // localStorage saves (profiles, coins, lives) persist across launches.
        wv.loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null);
        setContentView(wv);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pause the WebView when the app is minimized: this pauses JS timers
        // and audio, so the background music stops instead of playing on.
        if (wv != null) wv.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (wv != null) wv.onResume();
    }

    private void loadRewardedAd() {
        String unitId = getString(R.string.admob_rewarded_ad_unit_id);
        AdRequest request = new AdRequest.Builder().build();
        RewardedAd.load(this, unitId, request, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(RewardedAd ad) {
                rewardedAd = ad;
                rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        // If the ad was closed without the reward callback
                        // firing, the user closed early: no reward.
                        if (pendingRewardType != null && !rewardGranted) {
                            jsRewardResult(pendingRewardType, false, "closed_early");
                        }
                        pendingRewardType = null;
                        rewardGranted = false;
                        rewardedAd = null;
                        loadRewardedAd(); // preload the next one
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError err) {
                        Log.w(TAG, "Rewarded ad failed to show: " + err.getMessage());
                        if (pendingRewardType != null) {
                            jsRewardResult(pendingRewardType, false,
                                    "show_failed:" + err.getCode());
                        }
                        pendingRewardType = null;
                        rewardGranted = false;
                        rewardedAd = null;
                        loadRewardedAd();
                    }
                });
                jsAdReady();
            }

            @Override
            public void onAdFailedToLoad(LoadAdError err) {
                Log.w(TAG, "Rewarded ad failed to load: " + err.getMessage());
                rewardedAd = null;
                // No retry timer here: the next show/preload request retries.
            }
        });
    }

    private void showRewardedAdOnUi(String rewardType) {
        if (rewardedAd == null) {
            jsRewardResult(rewardType, false, "not_ready");
            loadRewardedAd();
            return;
        }
        pendingRewardType = rewardType;
        rewardGranted = false;
        rewardedAd.show(this, (RewardItem item) -> {
            rewardGranted = true;
            jsRewardResult(rewardType, true, null);
            pendingRewardType = null;
        });
    }

    private void jsRewardResult(String rewardType, boolean granted, String error) {
        String type = rewardType == null ? "" : rewardType.replace("'", "");
        String err = error == null ? "null" : ("'" + error.replace("'", "") + "'");
        String js = "window.__onAdmobReward && window.__onAdmobReward({rewardType:'"
                + type + "',granted:" + granted + ",error:" + err + "});";
        runOnUiThread(() -> {
            if (wv != null) wv.evaluateJavascript(js, null);
        });
    }

    private void jsAdReady() {
        runOnUiThread(() -> {
            if (wv != null) {
                wv.evaluateJavascript(
                        "window.__onAdmobAdReady && window.__onAdmobAdReady();", null);
            }
        });
    }

    /** JavaScript interface exposed to the game page as window.AdMobBridge. */
    public class AdMobBridge {
        @JavascriptInterface
        public boolean isRewardedAdReady() {
            return rewardedAd != null;
        }

        @JavascriptInterface
        public void showRewardedAd(String rewardType) {
            runOnUiThread(() -> showRewardedAdOnUi(rewardType));
        }

        @JavascriptInterface
        public void preloadRewardedAd() {
            runOnUiThread(() -> loadRewardedAd());
        }
    }

    private String readAsset(String path) {
        try {
            InputStream in = getAssets().open(path);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            in.close();
            return out.toString("UTF-8");
        } catch (Exception e) {
            return "<html><body>Failed to load game: " + e.getMessage() + "</body></html>";
        }
    }
}
