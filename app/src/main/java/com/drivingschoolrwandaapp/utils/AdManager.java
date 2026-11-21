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
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
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
            ? "ca-app-pub-3940256099942544/1033173712"
            : "ca-app-pub-2864387622629553/2665325775";

    private static final String REWARDED_AD_UNIT_ID = BuildConfig.DEBUG
            ? "ca-app-pub-3940256099942544/5224354917"
            : "ca-app-pub-2864387622629553/5099917426";

    private static final String RETAKE_AD_UNIT_ID = BuildConfig.DEBUG
            ? "ca-app-pub-3940256099942544/5354046379"
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
    private static long lastInterstitialShownAt;
    private static long rewardedRetryAt;
    private static long retakeRetryAt;
    private static long interstitialRetryAt;
    private static final long LOAD_FAILURE_COOLDOWN_MS = 30_000;
    private static final java.util.Map<String, Long> bannerRetryAt = new java.util.HashMap<>();

    private static final long AD_MAX_AGE_MS = 55 * 60 * 1000L;
    private static final long TRANSITION_INTERVAL_MS = 120_000;
    /** Keep a short gap after any fullscreen ad so a rewarded exit never
     *  immediately opens an interstitial. Interstitials themselves still
     *  respect the full {@link #TRANSITION_INTERVAL_MS} cooldown. */
    private static final long MIN_FULLSCREEN_GAP_MS = 30_000;

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
            configureTestDevices(context);
            MobileAds.initialize(context.getApplicationContext(),
                    status -> Log.d(TAG, "Mobile Ads initialized"));
        }
        return true;
    }

    /**
     * Debug builds list their test device IDs in {@code admob_test_device_ids}
     * (comma separated), so Ad Inspector and test creatives work without
     * touching the AdMob console. Release builds always use an empty list.
     */
    private static void configureTestDevices(Context context) {
        RequestConfiguration.Builder builder = new RequestConfiguration.Builder();
        if (BuildConfig.DEBUG) {
            String ids = context.getString(R.string.admob_test_device_ids);
            if (ids != null && !ids.trim().isEmpty()) {
                java.util.List<String> deviceIds = new java.util.ArrayList<>();
                for (String id : ids.split(",")) {
                    if (!id.trim().isEmpty()) {
                        deviceIds.add(id.trim());
                    }
                }
                builder.setTestDeviceIds(deviceIds);
            }
        }
        MobileAds.setRequestConfiguration(builder.build());
    }

    /** Opens the AdMob Ad Inspector. Call from a debug-only entry point. */
    public static void openAdInspector(@NonNull Context context) {
        MobileAds.openAdInspector(context, error -> {
            if (error != null) {
                Log.w(TAG, "Ad Inspector failed: " + error.getMessage());
            }
        });
    }

    // ------------------------------------------------------------------
    // Diagnostics
    // ------------------------------------------------------------------
    private static OnPaidEventListener paidListener(@NonNull Context context,
                                                    @NonNull AdAnalytics.Format format) {
        // Ads live in static fields; never let a listener pin an Activity.
        Context appContext = context.getApplicationContext();
        return adValue -> {
            if (adValue == null) return;
            Log.d(TAG, format + " paid event: valueMicros=" + adValue.getValueMicros()
                    + ", currency=" + adValue.getCurrencyCode()
                    + ", precision=" + adValue.getPrecisionType());
            AdAnalytics.paid(appContext, format, adValue);
        };
    }

    private static void logResponseInfo(String format, @Nullable ResponseInfo info) {
        if (info == null) {
            Log.d(TAG, format + " filled without ResponseInfo");
            return;
        }
        StringBuilder message = new StringBuilder(format)
                .append(" filled: responseId=").append(info.getResponseId())
                .append(", adapter=").append(info.getMediationAdapterClassName());
        if (info.getLoadedAdapterResponseInfo() != null) {
            message.append(", source=")
                    .append(info.getLoadedAdapterResponseInfo().getAdSourceName());
        }
        Log.d(TAG, message.toString());
    }

    private static void logLoadFailure(String format, @NonNull LoadAdError error) {
        StringBuilder message = new StringBuilder(format)
                .append(" load failed: code=").append(error.getCode())
                .append(", domain=").append(error.getDomain())
                .append(", message=").append(error.getMessage());
        ResponseInfo info = error.getResponseInfo();
        if (info != null) {
            message.append(", responseId=").append(info.getResponseId());
            if (info.getLoadedAdapterResponseInfo() != null) {
                message.append(", source=")
                        .append(info.getLoadedAdapterResponseInfo().getAdSourceName());
            }
        }
        Log.w(TAG, message.toString());
    }

    private static void logShowFailure(String format, @NonNull AdError error) {
        Log.w(TAG, format + " show failed: code=" + error.getCode()
                + ", domain=" + error.getDomain()
                + ", message=" + error.getMessage());
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
        if (activity.isFinishing() || activity.isDestroyed()) return;
        // Keep the current banner/request rather than creating duplicates.
        if (container.getChildCount() > 0) return;
        if (!canRequestAds(activity)) {
            deferBannerUntilConsent(activity, container, adUnitId);
            return;
        }

        String unitId = adUnitId != null ? adUnitId : BANNER_AD_UNIT_ID;
        Long retryAt = bannerRetryAt.get(unitId);
        if (retryAt != null && SystemClock.elapsedRealtime() < retryAt) {
            container.setVisibility(View.GONE);
            return;
        }
        AdView adView = new AdView(activity);
        adView.setAdUnitId(unitId);
        adView.setOnPaidEventListener(paidListener(activity, AdAnalytics.Format.BANNER));

        hideBanner(container);
        container.addView(adView,
                new FrameLayout.LayoutParams(-2, -2, android.view.Gravity.CENTER));
        container.setVisibility(View.INVISIBLE);

        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                if (adView.getParent() != container) return;
                if (!ConsentHelper.canShowAds()) {
                    hideBanner(container);
                    return;
                }
                bannerRetryAt.remove(unitId);
                container.setVisibility(View.VISIBLE);
                logResponseInfo("Banner", adView.getResponseInfo());
                AdAnalytics.loaded(activity, AdAnalytics.Format.BANNER, adView.getResponseInfo());
            }

            @Override
            public void onAdImpression() {
                Log.d(TAG, "Banner impression");
                AdAnalytics.impression(activity, AdAnalytics.Format.BANNER);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                if (adView.getParent() != container) return;
                bannerRetryAt.put(unitId, SystemClock.elapsedRealtime() + LOAD_FAILURE_COOLDOWN_MS);
                // Collapse unused space and release the failed view. A later screen
                // visit may request again after cooldown; do not loop on no-fill.
                hideBanner(container);
                logLoadFailure("Banner", error);
                AdAnalytics.loadFailed(activity, AdAnalytics.Format.BANNER, error);
            }
        });

        attachBannerLifecycle(activity, container, adView);
        measureAndLoadAdaptiveBanner(container, adView);
    }

    private static void deferBannerUntilConsent(Activity activity,
                                                FrameLayout container,
                                                String adUnitId) {
        LifecycleOwner owner = ViewTreeLifecycleOwner.get(container);
        if (owner == null && activity instanceof LifecycleOwner) {
            owner = (LifecycleOwner) activity;
        }
        if (owner == null
                || owner.getLifecycle().getCurrentState()
                        == androidx.lifecycle.Lifecycle.State.DESTROYED) {
            return;
        }
        final androidx.lifecycle.Observer<Boolean>[] observerRef =
                new androidx.lifecycle.Observer[1];
        androidx.lifecycle.Observer<Boolean> observer = eligible -> {
            if (!Boolean.TRUE.equals(eligible) || !ConsentHelper.canShowAds()) {
                return;
            }
            ConsentHelper.eligibility.removeObserver(observerRef[0]);
            container.post(() -> {
                if (container.isAttachedToWindow()
                        && container.getChildCount() == 0) {
                    showBanner(activity, container, adUnitId);
                }
            });
        };
        observerRef[0] = observer;
        ConsentHelper.eligibility.observe(owner, observer);
        // If consent is denied (or never resolves), drop the observer with the view.
        owner.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onDestroy(@NonNull LifecycleOwner lifecycleOwner) {
                ConsentHelper.eligibility.removeObserver(observer);
                lifecycleOwner.getLifecycle().removeObserver(this);
            }
        });
    }

    private static void attachBannerLifecycle(Activity activity,
                                              FrameLayout container,
                                              AdView adView) {
        LifecycleOwner owner = ViewTreeLifecycleOwner.get(container);
        if (owner == null && activity instanceof LifecycleOwner) {
            owner = (LifecycleOwner) activity;
        }
        if (owner != null) {
            owner.getLifecycle().addObserver(new DefaultLifecycleObserver() {
                @Override
                public void onStart(@NonNull LifecycleOwner lifecycleOwner) {
                    adView.resume();
                }

                @Override
                public void onStop(@NonNull LifecycleOwner lifecycleOwner) {
                    adView.pause();
                }

                @Override
                public void onDestroy(@NonNull LifecycleOwner lifecycleOwner) {
                    lifecycleOwner.getLifecycle().removeObserver(this);
                    if (adView.getParent() == container) {
                        container.removeView(adView);
                    }
                    adView.destroy();
                    container.setVisibility(View.GONE);
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
                        if (adView.getParent() != container) {
                            container.getViewTreeObserver()
                                    .removeOnGlobalLayoutListener(this);
                            return;
                        }
                        int width = container.getWidth()
                                - container.getPaddingLeft()
                                - container.getPaddingRight();
                        if (width <= 0) {
                            return;
                        }
                        container.getViewTreeObserver()
                                .removeOnGlobalLayoutListener(this);
                        if (!ConsentHelper.canShowAds()) {
                            return;
                        }
                        int widthDp = (int) (width
                                / container.getResources().getDisplayMetrics().density);
                        adView.setAdSize(
                                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                                        container.getContext(), widthDp));
                        AdAnalytics.request(container.getContext(), AdAnalytics.Format.BANNER);
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
        if (SystemClock.elapsedRealtime() < rewardedRetryAt) return;
        if (SystemClock.elapsedRealtime() - rewardedLoadedAt >= AD_MAX_AGE_MS) {
            rewardedAd = null;
        }
        if (!canRequestAds(context) || loadingReward || rewardedAd != null) {
            return;
        }
        loadingReward = true;
        AdAnalytics.request(context, AdAnalytics.Format.REWARDED);
        RewardedAd.load(context.getApplicationContext(), REWARDED_AD_UNIT_ID,
                new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        loadingReward = false;
                        rewardedLoadedAt = SystemClock.elapsedRealtime();
                        logResponseInfo("Rewarded", ad.getResponseInfo());
                        AdAnalytics.loaded(context, AdAnalytics.Format.REWARDED, ad.getResponseInfo());
                        ad.setOnPaidEventListener(paidListener(context, AdAnalytics.Format.REWARDED));
                        rewardedAd = ConsentHelper.canShowAds() ? ad : null;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        loadingReward = false;
                        logLoadFailure("Rewarded", error);
                        AdAnalytics.loadFailed(context, AdAnalytics.Format.REWARDED, error);
                        rewardedRetryAt = SystemClock.elapsedRealtime() + LOAD_FAILURE_COOLDOWN_MS;
                    }
                });
    }

    public static void showRewardedAdIfReady(@NonNull Activity activity,
                                             @NonNull RewardedAdCallback callback) {
        if (showingFullScreen) {
            return;
        }
        JourneyAnalytics.reward(activity, JourneyAnalytics.RewardFormat.VIDEO, JourneyAnalytics.RewardStage.SELECTED, false);
        if (SystemClock.elapsedRealtime() - rewardedLoadedAt >= AD_MAX_AGE_MS) {
            rewardedAd = null;
        }
        if (!ConsentHelper.canShowAds()) {
            rewardedAd = null;
            JourneyAnalytics.reward(activity, JourneyAnalytics.RewardFormat.VIDEO, JourneyAnalytics.RewardStage.UNAVAILABLE, false);
            callback.onAdFailedToShow();
            return;
        }
        RewardedAd ad = rewardedAd;
        if (ad == null) {
            loadRewardedAd(activity);
            JourneyAnalytics.reward(activity, JourneyAnalytics.RewardFormat.VIDEO, JourneyAnalytics.RewardStage.UNAVAILABLE, false);
            callback.onAdFailedToShow();
            return;
        }
        rewardedAd = null;
        showingFullScreen = true;
        RewardEvents events = new RewardEvents(callback, activity.getApplicationContext(), JourneyAnalytics.RewardFormat.VIDEO);
        ad.setFullScreenContentCallback(events);
        ad.show(activity, events);
    }

    // ------------------------------------------------------------------
    // Retake rewarded interstitial
    // ------------------------------------------------------------------
    public static void loadRetakeAd(@NonNull Context context) {
        if (SystemClock.elapsedRealtime() < retakeRetryAt) return;
        if (SystemClock.elapsedRealtime() - retakeLoadedAt >= AD_MAX_AGE_MS) {
            retakeAd = null;
        }
        if (!canRequestAds(context) || loadingRetake || retakeAd != null) {
            return;
        }
        loadingRetake = true;
        AdAnalytics.request(context, AdAnalytics.Format.REWARDED_INTERSTITIAL);
        RewardedInterstitialAd.load(context.getApplicationContext(),
                RETAKE_AD_UNIT_ID,
                new AdRequest.Builder().build(),
                new RewardedInterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedInterstitialAd ad) {
                        loadingRetake = false;
                        retakeLoadedAt = SystemClock.elapsedRealtime();
                        logResponseInfo("Rewarded interstitial", ad.getResponseInfo());
                        AdAnalytics.loaded(context, AdAnalytics.Format.REWARDED_INTERSTITIAL,
                                ad.getResponseInfo());
                        ad.setOnPaidEventListener(paidListener(context,
                                AdAnalytics.Format.REWARDED_INTERSTITIAL));
                        if (ConsentHelper.canShowAds()) {
                            retakeAd = ad;
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        loadingRetake = false;
                        logLoadFailure("Rewarded interstitial", error);
                        AdAnalytics.loadFailed(context, AdAnalytics.Format.REWARDED_INTERSTITIAL, error);
                        retakeRetryAt = SystemClock.elapsedRealtime() + LOAD_FAILURE_COOLDOWN_MS;
                    }
                });
    }

    public static void showRetakeAd(Activity activity, RewardedAdCallback callback) {
        if (showingFullScreen) {
            return;
        }
        JourneyAnalytics.reward(activity, JourneyAnalytics.RewardFormat.INTERSTITIAL, JourneyAnalytics.RewardStage.SELECTED, false);
        if (SystemClock.elapsedRealtime() - retakeLoadedAt >= AD_MAX_AGE_MS) {
            retakeAd = null;
        }
        RewardedInterstitialAd ad = retakeAd;
        if (ad == null || !canRequestAds(activity)) {
            loadRetakeAd(activity);
            JourneyAnalytics.reward(activity, JourneyAnalytics.RewardFormat.INTERSTITIAL, JourneyAnalytics.RewardStage.UNAVAILABLE, false);
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
                RewardEvents events = new RewardEvents(callback, activity.getApplicationContext(), JourneyAnalytics.RewardFormat.INTERSTITIAL);
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
        if (SystemClock.elapsedRealtime() < interstitialRetryAt) return;
        if (SystemClock.elapsedRealtime() - interstitialLoadedAt >= AD_MAX_AGE_MS) {
            interstitialAd = null;
        }
        if (!canRequestAds(context) || loadingInterstitial || interstitialAd != null) {
            return;
        }
        loadingInterstitial = true;
        AdAnalytics.request(context, AdAnalytics.Format.INTERSTITIAL);
        InterstitialAd.load(context.getApplicationContext(),
                INTERSTITIAL_AD_UNIT_ID,
                new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        loadingInterstitial = false;
                        interstitialLoadedAt = SystemClock.elapsedRealtime();
                        logResponseInfo("Interstitial", ad.getResponseInfo());
                        AdAnalytics.loaded(context, AdAnalytics.Format.INTERSTITIAL,
                                ad.getResponseInfo());
                        ad.setOnPaidEventListener(paidListener(context,
                                AdAnalytics.Format.INTERSTITIAL));
                        interstitialAd = ConsentHelper.canShowAds() ? ad : null;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        loadingInterstitial = false;
                        logLoadFailure("Interstitial", error);
                        AdAnalytics.loadFailed(context, AdAnalytics.Format.INTERSTITIAL, error);
                        interstitialRetryAt = SystemClock.elapsedRealtime() + LOAD_FAILURE_COOLDOWN_MS;
                    }
                });
    }

    public static void showTransitionAd(Activity activity, Runnable continueAction) {
        if (showingFullScreen) {
            return;
        }
        // Do not request while the 2-minute cooldown is active: that request
        // could never be shown and only inflates the request count.
        if (!ConsentHelper.canShowAds() || !isTransitionAllowed()) {
            continueAction.run();
            return;
        }
        if (interstitialAd != null
                && SystemClock.elapsedRealtime() - interstitialLoadedAt >= AD_MAX_AGE_MS) {
            interstitialAd = null;
        }
        if (interstitialAd == null) {
            loadInterstitial(activity);
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
                long now = SystemClock.elapsedRealtime();
                lastFullScreenAt = now;
                lastInterstitialShownAt = now;
                Log.d(TAG, "Interstitial impression");
                AdAnalytics.impression(activity, AdAnalytics.Format.INTERSTITIAL);
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                finishTransition();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                logShowFailure("Interstitial", error);
                AdAnalytics.showFailed(activity, AdAnalytics.Format.INTERSTITIAL, error);
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
        long now = SystemClock.elapsedRealtime();
        // Interstitials keep their own 2-minute cadence; a rewarded show only
        // imposes the short cross-format gap instead of resetting that timer.
        if (lastInterstitialShownAt > 0
                && now - lastInterstitialShownAt < TRANSITION_INTERVAL_MS) {
            return false;
        }
        return lastFullScreenAt <= 0
                || now - lastFullScreenAt >= MIN_FULLSCREEN_GAP_MS;
    }

    // ------------------------------------------------------------------
    // Reward delivery helper
    // ------------------------------------------------------------------
    private static final class RewardEvents extends FullScreenContentCallback
            implements OnUserEarnedRewardListener {

        private final RewardedAdCallback callback;
        private final Context context;
        private final JourneyAnalytics.RewardFormat format;
        @SuppressWarnings("unused")
        private RewardItem earnedReward;
        private boolean dismissed;
        private boolean delivered;

        RewardEvents(RewardedAdCallback callback, Context context, JourneyAnalytics.RewardFormat format) {
            this.callback = callback;
            this.context = context;
            this.format = format;
        }

        @Override
        public void onAdShowedFullScreenContent() {
            lastFullScreenAt = SystemClock.elapsedRealtime();
            Log.d(TAG, format + " impression");
            // The retake unit is a rewarded interstitial, so both formats report
            // as rewarded_interstitial in AdMob; keep the load/paid/impression
            // events on the same ad_format so per-format funnels reconcile.
            AdAnalytics.Format analyticsFormat = format == JourneyAnalytics.RewardFormat.INTERSTITIAL
                    ? AdAnalytics.Format.REWARDED_INTERSTITIAL
                    : AdAnalytics.Format.REWARDED;
            AdAnalytics.impression(context, analyticsFormat);
            JourneyAnalytics.reward(context, format, JourneyAnalytics.RewardStage.SHOWN, false);
        }

        @Override
        public void onUserEarnedReward(@NonNull RewardItem reward) {
            if (earnedReward == null && !delivered) {
                JourneyAnalytics.reward(context, format, JourneyAnalytics.RewardStage.EARNED, true);
            }
            Log.d(TAG, "Reward earned by SDK callback");
            earnedReward = reward;
            deliverIfReady();
        }

        @Override
        public void onAdDismissedFullScreenContent() {
            if (!dismissed) JourneyAnalytics.reward(context, format, JourneyAnalytics.RewardStage.DISMISSED, earnedReward != null);
            Log.d(TAG, "Rewarded ad dismissed; reward earned=" + (earnedReward != null));
            showingFullScreen = false;
            dismissed = true;
            deliverIfReady();
        }

        @Override
        public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
            logShowFailure(format.name(), error);
            AdAnalytics.showFailed(context,
                    format == JourneyAnalytics.RewardFormat.INTERSTITIAL
                            ? AdAnalytics.Format.REWARDED_INTERSTITIAL
                            : AdAnalytics.Format.REWARDED,
                    error);
            JourneyAnalytics.reward(context, format, JourneyAnalytics.RewardStage.SHOW_FAILED, false);
            showingFullScreen = false;
            callback.onAdFailedToShow();
        }

        private void deliverIfReady() {
            if (dismissed && earnedReward != null && !delivered) {
                delivered = true;
                Log.d(TAG, "Delivering earned reward to content unlock callback");
                RewardItem reward = earnedReward;
                earnedReward = null;
                callback.onRewardEarned(reward);
            }
        }
    }
}
