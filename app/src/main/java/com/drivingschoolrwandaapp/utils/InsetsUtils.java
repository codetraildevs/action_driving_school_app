package com.drivingschoolrwandaapp.utils;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;

/**
 * Helpers for handling system bar insets in edge-to-edge mode.
 */
public final class InsetsUtils {

    private InsetsUtils() {
    }

    /**
     * Pads the given view so its content stays clear of the transparent status bar
     * (top) and gesture navigation bar (bottom) when edge-to-edge is enabled.
     *
     * <p>Uses the platform {@link View#setOnApplyWindowInsetsListener} instead of the
     * deprecated {@code ViewCompat.setOnApplyWindowInsetsListener} (deprecated in
     * androidx.core 1.16+; using it triggers Play Console's "deprecated APIs or
     * parameters for edge-to-edge" check). The platform insets are converted to
     * {@link WindowInsetsCompat} for {@code Type.systemBars()} handling.
     *
     * @param view          the view to pad
     * @param includeTop    apply the status-bar inset as top padding
     * @param includeBottom apply the navigation-bar inset as bottom padding
     */
    public static void applySystemBarsPadding(View view, boolean includeTop, boolean includeBottom) {
        view.setOnApplyWindowInsetsListener((v, insets) -> {
            WindowInsetsCompat compatInsets = WindowInsetsCompat.toWindowInsetsCompat(insets);
            Insets systemBars = compatInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                v.getPaddingLeft(),
                includeTop ? systemBars.top : v.getPaddingTop(),
                v.getPaddingRight(),
                includeBottom ? systemBars.bottom : v.getPaddingBottom()
            );
            return insets;
        });
    }
}
