package com.drivingschoolrwandaapp.utils;

import android.app.Activity;
import android.content.Context;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewTreeLifecycleOwner;

import com.drivingschoolrwandaapp.BuildConfig;
import com.drivingschoolrwandaapp.R;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.InterstitialAdLoadCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.RewardItem;
import com.google.android.gms.ads.RewardedAd;
import com.google.android.gms.ads.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;

/**
 * Consent-gated AdMob ads.
 *
 * Supported ad formats:
 * <ul>
 *   <li>adaptive anchored banner</li>
 *   <li>transition interstitial</li>
 *   <li>rewarded video</li>
 *   <li>retake rewarded interstitial</li>
 * </ul>
 *
 * <p>Ad requests and impressions are gated by the User Messaging Platform
 * consent decision in {@link ConsentHelper}. Ads are not requested or shown
 * before consent is obtained where required (for example EEA/UK users).
 *
 * <p>The AdMob App ID is declared in the manifest as
 * {@code @string/admob_app_id} and is shared by debug and release builds.
 * Ad unit IDs are selected at runtime based on {@link BuildConfig#DEBUG}.
 */
public final class AdManager {

    private static final String TAG = "AdManager";

    // ------------------------------------------------------------------
    // Ad unit IDs
    // ------------------------------------------------------------------
    // Debug branch: AdMob test ad unit IDs. These must only be used in
    // debug builds.
    //
    // Production branch: the ad unit IDs from your AdMob console, mapped
    // to the same ad formats used in the app:
    // - Banner
    // - Rewarded video
    // - Rewarded interstitial (retake)
    // - Interstitial
    //
    // If you need a separate AdMob App ID per build variant, override
    // @string/admob_app_id in a build-variant resource instead of changing
    // this class.

    private static final String BANNER_AD_UNIT_ID = BuildConfig.DEBUG
            ? "ca-app-pub-3940256099942544/9214589741"
            : "ca-app-pub-2864387622629553/4416179690";

    private static final String INTERSTITIAL_AD_UNIT_ID = BuildConfig.DEBUG
            ? "ca-app-pub-3940256099942544/9214589741"
            : "ca-app-pub-2864387622629553/2665325775";

    private static final String REWARDED_AD_UNIT_ID = BuildConfig.DEBUG
            ? "ca-app-pub-3940256099942544/9214589741"
            : "ca-app-pub-2864387622629553/5099917426";

    private static final String RETAKE_AD_UNIT_ID = BuildConfig.DEBUG
            ? "ca-app-pub-3940256099942544/9214589741"
            : "ca-app-pub-2864387622629553/1024941040";

    // ------------------------------------------------------------------
    // Lifecycle state
    // ------------------------------------------------------------------
    private static boolean initialized;
    private static boolean showingFullScreen;

    private static InterstitialAd interstitialAd;
    private static boolean loadingInterstitial;
    private static long interstitialLoadedAt;

    private static RewardedAd rewardedAd;
    private static boolean loadingReward;
    private static long rewardedLoadedAt;

    private static RewardedInterstitialAd retakeAd;
    private static boolean loadingRetake;
    private static long retakeLoadedAt;

    private static long lastFullScreenAt;

    private static final long AD_MAX_AGE_MS = 55 * 60 * 1000L;
    private static final long TRANSITION_INTERVAL_MS = 120_000;

    private AdManager() {
        /* static utility class */
    }

    // ------------------------------------------------------------------
    // Consent gating
    // ------------------------------------------------------------------
    private static boolean canRequestAds(Context context) {
        if (!ConsentHelper.canShowAds()) {
            return false;
        }
        if (!initialized) {
            initialized = true;
            MobileAds.initialize(context.getApplicationContext(),
                    status -> Log.d(TAG, "Mobile Ads initialized"));
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Banner
    // ------------------------------------------------------------------
    /**
     * Show an adaptive anchored banner inside {@code container}.
     *
     * <p>If consent is not yet available, the request is deferred and retried
     * when consent becomes available.
     */
    public static void showBanner(@NonNull Activity activity,
                                  @NonNull FrameLayout container,
                                  @Nullable String adUnitId) {
        if (!canRequestAds(activity)) {
            deferBannerUntilConsent(activity, container, adUnitId);
            return;
        }

        AdView adView = new AdView(activity);
        adView.setAdUnitId(adUnitId != null ? adUnitId : BANNER_AD_UNIT_ID);

        hideBanner(container);
        container.addView(adView,
                new FrameLayout.LayoutParams(-2, -2, android.view.Gravity.CENTER));
        container.setVisibility(View.INVISIBLE);

        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                if (!ConsentHelper.canShowAds()) {
                    hideBanner(container);
                    return;
                }
                container.setVisibility(View.VISIBLE);
                Log.d(TAG, "Banner ad loaded");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                container.setVisibility(View.GONE);
                Log.w(TAG, "Banner ad failed: " + error.getMessage());
            }
        });

        attachDestroyOnLifecycleDestroy(activity, container, adView);
        measureAndLoadAdaptiveBanner(container, adView);
    }

    private static void deferBannerUntilConsent(Activity activity,
                                                FrameLayout container,
                                                String adUnitId) {
        LifecycleOwner owner = ViewTreeLifecycleOwner.get(container);
        if (owner == null && activity instanceof LifecycleOwner) {
            owner = (LifecycleOwner) activity;
        }
        if (owner != null) {
            ConsentHelper.eligibility.observe(owner,
                    new androidx.lifecycle.Observer<Boolean>() {
                        @Override
                        public void onChanged(Boolean eligible) {
                            if (!Boolean.TRUE.equals(eligible)
                                    || !ConsentHelper.canShowAds()) {
                                return;
                            }
                            ConsentHelper.eligibility.removeObserver(this);
                            container.post(() -> {
                                if (container.isAttachedToWindow()
                                        && container.getChildCount() == 0) {
                                    showBanner(activity, container, adUnitId);
                                }
                            });
                        }
                    });
        }
    }

    private static void attachDestroyOnLifecycleDestroy(Activity activity,
                                                       FrameLayout container,
                                                       AdView adView) {
        LifecycleOwner owner = ViewTreeLifecycleOwner.get(container);
        if (owner == null && activity instanceof LifecycleOwner) {
            owner = (LifecycleOwner) activity;
        }
        if (owner != null) {
            owner.getLifecycle().addObserver(new DefaultLifecycleObserver() {
                @Override
                public void onDestroy(@NonNull LifecycleOwner lifecycleOwner) {
                    adView.destroy();
                    lifecycleOwner.getLifecycle().removeObserver(this);
                }
            });
        }
    }

    private static void measureAndLoadAdaptiveBanner(FrameLayout container,
                                                     AdView adView) {
        container.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int width = container.getWidth()
                                - container.getPaddingLeft()
                                - container.getPaddingRight();
                        if (width <= 0) {
                            return;
                        }
                        container.getViewTreeObserver()
                                .removeOnGlobalLayoutListener(this);
                        if (adView.getParent() != container
                                || !ConsentHelper.canShowAds()) {
                            return;
                        }
                        int widthDp = (int) (width
                                / container.getResources().getDisplayMetrics().density);
                        adView.setAdSize(
                                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                                        container.getContext(), widthDp));
                        adView.loadAd(new AdRequest.Builder().build());
                    }
                });
    }

    /**
     * Remove and destroy any banner currently inside the container.
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

    // ------------------------------------------------------------------
    // Rewarded ad
    // ------------------------------------------------------------------
    public interface RewardedAdCallback {
        void onRewardEarned(@NonNull RewardItem reward);
        void onAdFailedToShow();
    }

    public static void loadRewardedAd(@NonNull Context context) {
        if (SystemClock.elapsedRealtime() - rewardedLoadedAt >= AD_MAX_AGE_MS) {
            rewardedAd = null;
        }
        if (!canRequestAds(context) || loadingReward || rewardedAd != null) {
            return;
        }
        loadingReward = true;
        RewardedAd.load(context.getApplicationContext(), REWARDED_AD_UNIT_ID,
                new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        loadingReward = false;
                        rewardedLoadedAt = SystemClock.elapsedRealtime();
                        Log.d(TAG, "Rewarded video loaded");
                        rewardedAd = ConsentHelper.canShowAds() ? ad : null;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        loadingReward = false;
                        Log.w(TAG, "Rewarded ad load failed: " + error.getCode());
                    }
                });
    }

    public static void showRewardedAdIfReady(@NonNull Activity activity,
                                             @NonNull RewardedAdCallback callback) {
        if (showingFullScreen) {
            return;
        }
        if (SystemClock.elapsedRealtime() - rewardedLoadedAt >= AD_MAX_AGE_MS) {
            rewardedAd = null;
        }
        if (!ConsentHelper.canShowAds()) {
            rewardedAd = null;
            callback.onAdFailedToShow();
            return;
        }
        RewardedAd ad = rewardedAd;
        if (ad == null) {
            loadRewardedAd(activity);
            callback.onAdFailedToShow();
            return;
        }
        rewardedAd = null;
        showingFullScreen = true;
        RewardEvents events = new RewardEvents(callback);
        ad.setFullScreenContentCallback(events);
        ad.show(activity, events);
    }

    // ------------------------------------------------------------------
    // Retake rewarded interstitial
    // ------------------------------------------------------------------
    public static void loadRetakeAd(Activity activity) {
        if (SystemClock.elapsedRealtime() - retakeLoadedAt >= AD_MAX_AGE_MS) {
            retakeAd = null;
        }
        if (!canRequestAds(activity) || loadingRetake || retakeAd != null) {
            return;
        }
        loadingRetake = true;
        RewardedInterstitialAd.load(activity.getApplicationContext(),
                RETAKE_AD_UNIT_ID,
                new AdRequest.Builder().build(),
                new RewardedInterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedInterstitialAd ad) {
                        loadingRetake = false;
                        retakeLoadedAt = SystemClock.elapsedRealtime();
                        Log.d(TAG, "Rewarded interstitial loaded");
                        if (ConsentHelper.canShowAds()) {
                            retakeAd = ad;
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        loadingRetake = false;
                        Log.w(TAG, "Retake ad load failed: " + error.getCode());
                    }
                });
    }

    public static void showRetakeAd(Activity activity, RewardedAdCallback callback) {
        if (showingFullScreen) {
            return;
        }
        if (SystemClock.elapsedRealtime() - retakeLoadedAt >= AD_MAX_AGE_MS) {
            retakeAd = null;
        }
        RewardedInterstitialAd ad = retakeAd;
        if (ad == null || !canRequestAds(activity)) {
            loadRetakeAd(activity);
            callback.onAdFailedToShow();
            return;
        }

        showingFullScreen = true;
        AlertDialog intro = new AlertDialog.Builder(activity)
                .setMessage(R.string.rewarded_retake_intro)
                .setNegativeButton(R.string.skip_ad, (dialog, which) -> {})
                .create();

        CountDownTimer timer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long remaining) {
                /* no-op */
            }

            @Override
            public void onFinish() {
                if (!intro.isShowing()) {
                    return;
                }
                intro.setOnDismissListener(null);
                intro.dismiss();
                showingFullScreen = false;
                if (activity.isFinishing() || activity.isDestroyed()
                        || !ConsentHelper.canShowAds()
                        || !activityResumed(activity)) {
                    return;
                }
                retakeAd = null;
                showingFullScreen = true;
                RewardEvents events = new RewardEvents(callback);
                ad.setFullScreenContentCallback(events);
                ad.show(activity, events);
            }
        };
        intro.setOnDismissListener(dialog -> {
            timer.cancel();
            showingFullScreen = false;
        });
        intro.show();
        timer.start();
    }

    private static boolean activityResumed(Activity activity) {
        if (!(activity instanceof LifecycleOwner)) {
            return true;
        }
        return ((LifecycleOwner) activity)
                .getLifecycle()
                .getCurrentState()
                .isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED);
    }

    // ------------------------------------------------------------------
    // Transition interstitial
    // ------------------------------------------------------------------
    public static void loadInterstitial(Context context) {
        if (SystemClock.elapsedRealtime() - interstitialLoadedAt >= AD_MAX_AGE_MS) {
            interstitialAd = null;
        }
        if (!canRequestAds(context) || loadingInterstitial || interstitialAd != null) {
            return;
        }
        loadingInterstitial = true;
        InterstitialAd.load(context.getApplicationContext(),
                INTERSTITIAL_AD_UNIT_ID,
                new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        loadingInterstitial = false;
                        interstitialLoadedAt = SystemClock.elapsedRealtime();
                        interstitialAd = ConsentHelper.canShowAds() ? ad : null;
                        Log.d(TAG, "Transition interstitial loaded");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        loadingInterstitial = false;
                        Log.w(TAG, "Transition ad load failed: " + error.getCode());
                    }
                });
    }

    public static void showTransitionAd(Activity activity, Runnable continueAction) {
        if (showingFullScreen) {
            return;
        }
        loadInterstitial(activity);
        if (!ConsentHelper.canShowAds() || interstitialAd == null
                || !isTransitionAllowed()) {
            continueAction.run();
            return;
        }
        InterstitialAd ad = interstitialAd;
        interstitialAd = null;
        showingFullScreen = true;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            private boolean completed;

            @Override
            public void onAdShowedFullScreenContent() {
                lastFullScreenAt = SystemClock.elapsedRealtime();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                finishTransition();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                finishTransition();
            }

            private void finishTransition() {
                if (completed) {
                    return;
                }
                completed = true;
                showingFullScreen = false;
                loadInterstitial(activity);
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    continueAction.run();
                }
            }
        });
        ad.show(activity);
    }

    private static boolean isTransitionAllowed() {
        if (lastFullScreenAt <= 0) {
            return true;
        }
        return SystemClock.elapsedRealtime() - lastFullScreenAt >= TRANSITION_INTERVAL_MS;
    }

    // ------------------------------------------------------------------
    // Reward delivery helper
    // ------------------------------------------------------------------
    private static final class RewardEvents extends FullScreenContentCallback
            implements OnUserEarnedRewardListener {

        private final RewardedAdCallback callback;
        private RewardItem earned;
        private boolean dismissed;
        private boolean delivered;

        RewardEvents(RewardedAdCallback callback) {
            this.callback = callback;
        }

        @Override
        public void onAdShowedFullScreenContent() {
            lastFullScreenAt = SystemClock.elapsedRealtime();
        }

        @Override
        public void onUserEarnedReward(@NonNull RewardItem reward) {
            Log.d(TAG, "Reward earned by SDK callback");
            earned = reward;
            deliverIfReady();
        }

        @Override
        public void onAdDismissedFullScreenContent() {
            Log.d(TAG, "Rewarded ad dismissed; reward earned=" + (earned != null));
            showingFullScreen = false;
            dismissed = true;
            deliverIfReady();
        }

        @Override
        public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
            showingFullScreen = false;
            callback.onAdFailedToShow();
        }

        private void deliverIfReady() {
            if (dismissed && earned != null && !delivered) {
                delivered = true;
                Log.d(TAG, "Delivering earned reward to content unlock callback");
                RewardItem reward = earned;
                earned = null;
                callback.onRewardEarned(reward);
            }
        }
    }
}
