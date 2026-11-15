package com.drivingschoolrwandaapp.utils;

import android.os.Bundle;
import android.util.Log;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import com.drivingschoolrwandaapp.R;

/** Called only after the SDK has earned and dismissed a rewarded retake ad. */
public final class RetakeNavigation {
    private RetakeNavigation() { }

    public static void open(Fragment source, int testId, String title) {
        if (!source.isAdded() || source.getView() == null || testId <= 0) return;
        // AdActivity can dismiss before the underlying Fragment has resumed.
        ResumedAction.run(source.getViewLifecycleOwner(), () -> {
            if (!source.isAdded() || source.getView() == null) return;
            NavController nav = NavHostFragment.findNavController(source);
            if (nav.getCurrentDestination() == null) return;
            int origin = nav.getCurrentDestination().getId();
            if (origin != R.id.testResultFragment && origin != R.id.resultsFragment) return;
            Bundle args = new Bundle();
            args.putInt("testId", testId);
            args.putString("title", title);
            args.putBoolean("isFree", true);
            args.putBoolean("isRealTimeFeedback", true);
            args.putBoolean("isReviewMode", false);
            NavOptions.Builder options = new NavOptions.Builder();
            if (origin == R.id.testResultFragment) {
                options.setPopUpTo(R.id.testResultFragment, true);
            }
            nav.navigate(R.id.action_global_testQuestionsFragment, args, options.build());
            Log.d("RetakeNavigation", "Opened rewarded exam attempt: " + testId);
        });
    }
}
