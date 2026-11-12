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
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;

/** Consent-gated banners and opt-in rewarded ads. */
public final class AdManager {

    private static final String TAG = "AdManager";

    private static final String BANNER_AD_UNIT_ID = "ca-app-pub-2864387622629553/4416179690";
    private static final String REWARDED_AD_UNIT_ID = "ca-app-pub-2864387622629553/5099917426";
    private static boolean initialized;
    private static boolean loadingReward;
    private static RewardedAd rewardedAd;

    private AdManager() { /* static utility */ }

    private static boolean canRequestAds(Context context) {
        if (!ConsentHelper.canShowAds()) return false;
        if (!initialized) {
            initialized = true;
            MobileAds.initialize(context.getApplicationContext(), status -> Log.d(TAG, "Mobile Ads initialized"));
        }
        return true;
    }

    public static void showBanner(@NonNull Activity activity,
                                  @NonNull FrameLayout container,
                                  @Nullable String adUnitId) {
        // Don't show ads until consent is obtained (GDPR compliance)
        if (!canRequestAds(activity)) {
            Log.d(TAG, "Banner skipped — consent not yet obtained");
            return;
        }
        AdView adView = new AdView(activity);
        adView.setAdUnitId(adUnitId != null ? adUnitId : BANNER_AD_UNIT_ID);
        adView.setAdSize(AdSize.BANNER);

        hideBanner(container);
        container.addView(adView);

        adView.setAdListener(new com.google.android.gms.ads.AdListener() {
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
        adView.loadAd(new AdRequest.Builder().build());
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

    public interface RewardedAdCallback {
        void onRewardEarned(@NonNull RewardItem reward);
        void onAdFailedToShow();
    }

    public static void loadRewardedAd(@NonNull Context context) {
        if (!canRequestAds(context) || loadingReward || rewardedAd != null) return;
        loadingReward = true;
        RewardedAd.load(context.getApplicationContext(), REWARDED_AD_UNIT_ID,
                new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                loadingReward = false;
                rewardedAd = ConsentHelper.canShowAds() ? ad : null;
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                loadingReward = false;
                Log.w(TAG, "Rewarded ad load failed: " + error.getCode());
            }
        });
    }

    public static void showRewardedAdIfReady(@NonNull Activity activity, @NonNull RewardedAdCallback callback) {
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
        // A reward may arrive before the ad closes. Wait for both events before
        // opening the material or navigating to the retake.
        class RewardEvents extends FullScreenContentCallback implements com.google.android.gms.ads.OnUserEarnedRewardListener {
            private RewardItem earned;
            private boolean dismissed;

            @Override
            public void onUserEarnedReward(@NonNull RewardItem reward) {
                earned = reward;
                deliverIfReady();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                dismissed = true;
                deliverIfReady();
            }

            private void deliverIfReady() {
                if (dismissed && earned != null) {
                    RewardItem reward = earned;
                    earned = null;
                    callback.onRewardEarned(reward);
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError error) {
                callback.onAdFailedToShow();
            }
        }
        RewardEvents events = new RewardEvents();
        ad.setFullScreenContentCallback(events);
        ad.show(activity, events);
    }
}
