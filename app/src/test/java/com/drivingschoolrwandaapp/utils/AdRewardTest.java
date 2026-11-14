package com.drivingschoolrwandaapp.utils;

import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Constructor;

import static org.mockito.Mockito.*;

/** Rewards must wait for ad dismissal and may unlock access only once. */
public class AdRewardTest {
    private AdManager.RewardedAdCallback callback;
    private RewardItem reward;
    private FullScreenContentCallback events;
    private OnUserEarnedRewardListener rewards;

    @Before
    public void setUp() throws Exception {
        callback = mock(AdManager.RewardedAdCallback.class);
        reward = mock(RewardItem.class);
        Class<?> type = Class.forName(AdManager.class.getName() + "$RewardEvents");
        Constructor<?> constructor = type.getDeclaredConstructor(AdManager.RewardedAdCallback.class);
        constructor.setAccessible(true);
        Object handler = constructor.newInstance(callback);
        events = (FullScreenContentCallback) handler;
        rewards = (OnUserEarnedRewardListener) handler;
    }

    @Test
    public void rewardBeforeDismissalWaitsForDismissal() {
        rewards.onUserEarnedReward(reward);
        verifyNoInteractions(callback);
        events.onAdDismissedFullScreenContent();
        verify(callback).onRewardEarned(reward);
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void dismissalBeforeRewardWaitsForReward() {
        events.onAdDismissedFullScreenContent();
        verifyNoInteractions(callback);
        rewards.onUserEarnedReward(reward);
        verify(callback).onRewardEarned(reward);
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void skippingWithoutRewardDoesNotUnlockAccess() {
        events.onAdDismissedFullScreenContent();
        verifyNoInteractions(callback);
    }

    @Test
    public void repeatedCallbacksUnlockOnlyOnce() {
        rewards.onUserEarnedReward(reward);
        events.onAdDismissedFullScreenContent();
        rewards.onUserEarnedReward(reward);
        events.onAdDismissedFullScreenContent();
        verify(callback, times(1)).onRewardEarned(reward);
        verifyNoMoreInteractions(callback);
    }
}
