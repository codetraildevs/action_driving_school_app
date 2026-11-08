package com.drivingschoolrwandaapp.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;

/**
 * Centralised AdMob helper.
 *
 * <p>Usage:
 * <ol>
 *   <li>Call {@link #initialize(Context)} once from {@code MainApplication.onCreate()}.
 *   <li>Use {@link #showBanner(Activity, FrameLayout, String)} to load a banner into a container.     * <li>Use {@link #loadInterstitial} / {@link #showInterstitialIfReady} for interstitials.
     * <li>Use {@link #loadRewardedInterstitial} / {@link #showRewardedInterstitialIfReady} for rewarded interstitials.
     * <li>Use {@link #loadRewardedAd} / {@link #showRewardedAdIfReady} for rewarded ads.
     * </ol>
 *     * <p>Ad-unit IDs below are the <b>production</b> IDs for publisher
     * {@code 2864387622629553}. Replace with your own IDs before publishing a new app/r
     * or if you create new ad units in the AdMob console.
     */
public final class AdManager {

    private static final String TAG = "AdManager";

    // ── Test ad-unit IDs (safe to ship in debug/release; Google serves test ads) ────────
    // private static final String BANNER_AD_UNIT_ID       = "ca-app-pub-3940256099942544/6300978111";
    // private static final String INTERSTITIAL_AD_UNIT_ID  = "ca-app-pub-3940256099942544/1033173712";
    // private static final String REWARDED_AD_UNIT_ID      = "ca-app-pub-3940256099942544/5224354917";

    // ── Production ad-unit IDs ──────────────────────────────────────────────────────────
    private static final String BANNER_AD_UNIT_ID            = "ca-app-pub-2864387622629553/4416179690";
    private static final String INTERSTITIAL_AD_UNIT_ID     = "ca-app-pub-2864387622629553/2665325775";
    private static final String REWARDED_AD_UNIT_ID         = "ca-app-pub-2864387622629553/5099917426";
    private static final String REWARDED_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-2864387622629553/1024941040";

    private static boolean initialized = false;
    private static InterstitialAd interstitialAd;
    private static RewardedAd rewardedAd;
    private static InterstitialAd rewardedInterstitialAd;

    // Minimum interval between interstitial shows (AdMob policy: no back-to-back ads)
    private static long lastInterstitialShowTime = 0;
    private static final long INTERSTITIAL_COOLDOWN_MS = 60_000; // 60 seconds

    private AdManager() { /* static utility */ }

    // ────────────────────────────────────────────────────────────────────────────────────
    // Initialization
    // ────────────────────────────────────────────────────────────────────────────────────

    /**
     * Initialise the Mobile Ads SDK. Safe to call more than once (idempotent).
     */
    public static void initialize(@NonNull Context context) {
        if (initialized) return;
        MobileAds.initialize(context, status -> {
            Log.d(TAG, "Mobile Ads SDK initialised");
            initialized = true;
        });
    }

    // ────────────────────────────────────────────────────────────────────────────────────
    // Banner ads
    // ────────────────────────────────────────────────────────────────────────────────────

    /**
     * Load a test banner ad into the given {@code container}.
     *
     * @param activity  host activity (required for ad lifecycle)
     * @param container a {@link FrameLayout} (or any ViewGroup) where the banner will be placed
     * @param adUnitId  pass {@code null} to use the built-in test ID
     */
    public static void showBanner(@NonNull Activity activity,
                                  @NonNull FrameLayout container,
                                  @Nullable String adUnitId) {
        // Don't show ads until consent is obtained (GDPR compliance)
        if (!ConsentHelper.canShowAds()) {
            Log.d(TAG, "Banner skipped — consent not yet obtained");
            return;
        }
        AdView adView = new AdView(activity);
        adView.setAdUnitId(adUnitId != null ? adUnitId : BANNER_AD_UNIT_ID);
        adView.setAdSize(AdSize.BANNER);

        container.removeAllViews();
        container.addView(adView);

        AdRequest request = new AdRequest.Builder().build();
        adView.loadAd(request);

        adView.setAdListener(new com.google.android.gms.ads.AdListener() {
            @Override
            public void onAdLoaded() {
                container.setVisibility(View.VISIBLE);
                Log.d(TAG, "Banner ad loaded");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                container.setVisibility(View.GONE);
                Log.w(TAG, "Banner ad failed: " + error.getMessage());
            }
        });
    }

    /**
     * Hide / destroy a banner that was previously loaded into a container.
     */
    public static void hideBanner(@NonNull FrameLayout container) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child instanceof AdView) {
                ((AdView) child).destroy();
            }
        }
        container.removeAllViews();
        container.setVisibility(View.GONE);
    }

    // ────────────────────────────────────────────────────────────────────────────────────
    // Interstitial ads
    // ────────────────────────────────────────────────────────────────────────────────────

    /**
     * Pre-load a standard interstitial. Call early (e.g. onResume or after init) so it's ready
     * when the user performs a qualifying action.
     */
    public static void loadInterstitial(@NonNull Context context) {
        loadInterstitial(context, null);
    }

    /**
     * Pre-load a standard interstitial with a custom ad-unit ID.
     */
    public static void loadInterstitial(@NonNull Context context, @Nullable String adUnitId) {
        String id = adUnitId != null ? adUnitId : INTERSTITIAL_AD_UNIT_ID;
        AdRequest request = new AdRequest.Builder().build();

        InterstitialAd.load(context, id, request, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                interstitialAd = ad;
                Log.d(TAG, "Interstitial ad loaded");

                ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        interstitialAd = null;
                        loadInterstitial(context, adUnitId);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                        interstitialAd = null;
                        Log.w(TAG, "Interstitial show failed: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                interstitialAd = null;
                Log.w(TAG, "Interstitial load failed: " + error.getMessage());
            }
        });
    }

    /**
     * Pre-load a rewarded interstitial. Call early so it's ready when the user taps "Watch Ad".
     */
    public static void loadRewardedInterstitial(@NonNull Context context) {
        loadRewardedInterstitial(context, null);
    }

    /**
     * Pre-load a rewarded interstitial with a custom ad-unit ID.
     */
    public static void loadRewardedInterstitial(@NonNull Context context, @Nullable String adUnitId) {
        String id = adUnitId != null ? adUnitId : REWARDED_INTERSTITIAL_AD_UNIT_ID;
        AdRequest request = new AdRequest.Builder().build();

        InterstitialAd.load(context, id, request, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                rewardedInterstitialAd = ad;
                Log.d(TAG, "Rewarded interstitial ad loaded");

                ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        rewardedInterstitialAd = null;
                        loadRewardedInterstitial(context, adUnitId);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                        rewardedInterstitialAd = null;
                        Log.w(TAG, "Rewarded interstitial show failed: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                rewardedInterstitialAd = null;
                Log.w(TAG, "Rewarded interstitial load failed: " + error.getMessage());
            }
        });
    }

    /**
     * Show the standard interstitial if it's loaded and ready.
     *
     * @return {@code true} if the ad was shown, {@code false} otherwise
     */
    public static boolean showInterstitialIfReady(@NonNull Activity activity) {
        // Don't show ads until consent is obtained (GDPR compliance)
        if (!ConsentHelper.canShowAds()) {
            Log.d(TAG, "Interstitial skipped — consent not yet obtained");
            return false;
        }
        if (interstitialAd == null) {
            Log.d(TAG, "Interstitial not ready yet");
            return false;
        }
        // Enforce cooldown: don't show interstitials back-to-back
        long now = System.currentTimeMillis();
        if (now - lastInterstitialShowTime < INTERSTITIAL_COOLDOWN_MS) {
            Log.d(TAG, "Interstitial cooldown active — skipping");
            return false;
        }
        lastInterstitialShowTime = now;
        interstitialAd.show(activity);
        return true;
    }

    /**
     * Show the rewarded interstitial if it's loaded and ready.
     * Does NOT enforce the interstitial cooldown (rewarded interstitials are a
     * separate ad slot with their own fill).
     *
     * @return {@code true} if the ad was shown, {@code false} otherwise
     */
    public static boolean showRewardedInterstitialIfReady(@NonNull Activity activity) {
        if (!ConsentHelper.canShowAds()) {
            Log.d(TAG, "Rewarded interstitial skipped — consent not yet obtained");
            return false;
        }
        if (rewardedInterstitialAd == null) {
            Log.d(TAG, "Rewarded interstitial not ready yet");
            return false;
        }
        rewardedInterstitialAd.show(activity);
        return true;
    }

    // ────────────────────────────────────────────────────────────────────────────────────
    // Rewarded ads
    // ────────────────────────────────────────────────────────────────────────────────────

    /**
     * Callback for rewarded ad events.
     */
    public interface RewardedAdCallback {
        /** Called when the user has earned the reward (watched the full ad). */
        void onRewardEarned(@NonNull RewardItem reward);
        /** Called when the ad fails to show. */
        void onAdFailedToShow();
    }

    /**
     * Pre-load a rewarded ad. Call early so it's ready when the user taps "Watch Ad".
     */
    public static void loadRewardedAd(@NonNull Context context) {
        loadRewardedAd(context, null);
    }

    /**
     * Pre-load a rewarded ad with a custom ad-unit ID.
     */
    public static void loadRewardedAd(@NonNull Context context, @Nullable String adUnitId) {
        String id = adUnitId != null ? adUnitId : REWARDED_AD_UNIT_ID;
        AdRequest request = new AdRequest.Builder().build();

        RewardedAd.load(context, id, request, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                rewardedAd = ad;
                Log.d(TAG, "Rewarded ad loaded");

                ad.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        rewardedAd = null;
                        loadRewardedAd(context, adUnitId);
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                        rewardedAd = null;
                        Log.w(TAG, "Rewarded ad show failed: " + error.getMessage());
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                rewardedAd = null;
                Log.w(TAG, "Rewarded ad load failed: " + error.getMessage());
            }
        });
    }

    /**
     * Show the rewarded ad if loaded. The {@code callback} fires when the user earns the
     * reward (watched to completion) or when showing fails.
     *
     * @return {@code true} if the ad was shown, {@code false} otherwise
     */
    public static boolean showRewardedAdIfReady(@NonNull Activity activity,
                                                @Nullable RewardedAdCallback callback) {
        // Don't show ads until consent is obtained (GDPR compliance)
        if (!ConsentHelper.canShowAds()) {
            Log.d(TAG, "Rewarded ad skipped — consent not yet obtained");
            if (callback != null) callback.onAdFailedToShow();
            return false;
        }
        if (rewardedAd != null) {
            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    rewardedAd = null;
                    Log.d(TAG, "Rewarded ad dismissed");
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                    rewardedAd = null;
                    Log.w(TAG, "Rewarded ad show failed: " + error.getMessage());
                    if (callback != null) callback.onAdFailedToShow();
                }
            });

            rewardedAd.show(activity, rewardItem -> {
                Log.d(TAG, "Rewarded! amount=" + rewardItem.getAmount() + " type=" + rewardItem.getType());
                if (callback != null) callback.onRewardEarned(rewardItem);
            });
            return true;
        }
        Log.d(TAG, "Rewarded ad not ready yet");
        return false;
    }

    /**
     * Whether a rewarded ad is currently loaded and ready to show.
     */
    public static boolean isRewardedAdReady() {
        return rewardedAd != null;
    }
}
