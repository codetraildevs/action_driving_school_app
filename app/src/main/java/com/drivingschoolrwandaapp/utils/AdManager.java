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
 *   <li>Use {@link #showBanner(Activity, FrameLayout, String)} to load a banner into a container.
 *   <li>Use {@link #loadInterstitial} / {@link #showInterstitialIfReady} for interstitials.
 *   <li>Use {@link #loadRewardedAd} / {@link #showRewardedAdIfReady} for rewarded ads.
 * </ol>
 *
 * <p>Ad-unit IDs below are the <b>test</b> IDs shipped by Google. Replace with your
 * production IDs before publishing.
 */
public final class AdManager {

    private static final String TAG = "AdManager";

    // ── Test ad-unit IDs (safe to ship in debug/release; Google serves test ads) ────────
    private static final String BANNER_AD_UNIT_ID       = "ca-app-pub-3940256099942544/6300978111";
    private static final String INTERSTITIAL_AD_UNIT_ID  = "ca-app-pub-3940256099942544/1033173712";
    private static final String REWARDED_AD_UNIT_ID      = "ca-app-pub-3940256099942544/5224354917";

    // ── Production ad-unit IDs (uncomment & set once you create ad units in AdMob console)
    // private static final String BANNER_AD_UNIT_ID      = "ca-app-pub-7698315805599052/XXXXXXXXXX";
    // private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-7698315805599052/XXXXXXXXXX";
    // private static final String REWARDED_AD_UNIT_ID     = "ca-app-pub-7698315805599052/XXXXXXXXXX";

    private static boolean initialized = false;
    private static InterstitialAd interstitialAd;
    private static RewardedAd rewardedAd;

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
     * Pre-load an interstitial. Call early (e.g. onResume or after init) so it's ready
     * when the user performs a qualifying action.
     */
    public static void loadInterstitial(@NonNull Context context) {
        loadInterstitial(context, null);
    }

    /**
     * Pre-load an interstitial with a custom ad-unit ID.
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
     * Show the interstitial if it's loaded and ready.
     *
     * @return {@code true} if the ad was shown, {@code false} otherwise
     */
    public static boolean showInterstitialIfReady(@NonNull Activity activity) {
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
